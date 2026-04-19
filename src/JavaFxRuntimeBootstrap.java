import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaFxRuntimeBootstrap {

    private static final String BOOTSTRAP_FLAG = "flightplanner.javafx.bootstrapped";
    private static final String[] REQUIRED_JAVAFX_CLASSES = {
            "javafx.embed.swing.JFXPanel",
            "javafx.scene.web.WebView",
            "javafx.application.Platform"
    };
    private static final String REQUIRED_MODULES = "javafx.controls,javafx.swing,javafx.web,jdk.httpserver";
    private static final String NATIVE_ACCESS_MODULES = "javafx.graphics,javafx.web";

    public static boolean relaunchWithJavaFxIfNeeded(Class<?> mainClass, String[] args) {
        if (isJavaFxAvailable()) {
            return false;
        }

        if (Boolean.getBoolean(BOOTSTRAP_FLAG)) {
            System.err.println("JavaFX runtime bootstrap was attempted, but JavaFX is still unavailable.");
            System.err.println("Set JAVAFX_HOME to your JavaFX SDK folder if you want embedded map support.");
            return false;
        }

        Path javaFxLibPath = findJavaFxLibPath();
        if (javaFxLibPath == null) {
            System.err.println("JavaFX SDK was not found automatically. The GUI will use the browser fallback for maps.");
            System.err.println("Set JAVAFX_HOME to your JavaFX SDK folder to enable embedded WebView.");
            return false;
        }

        try {
            relaunchProcess(mainClass, args, javaFxLibPath);
            return true;
        }
        catch (IOException e) {
            System.err.println("Failed to relaunch with JavaFX runtime: " + e.getMessage());
            return false;
        }
    }

    public static boolean isJavaFxAvailable() {
        for (String className: REQUIRED_JAVAFX_CLASSES) {
            try {
                Class.forName(className);
            }
            catch (ClassNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    public static Path findJavaFxLibPath() {
        List<Path> candidates = new ArrayList<>();

        addEnvironmentCandidate(candidates, "JAVAFX_HOME");
        addEnvironmentCandidate(candidates, "PATH_TO_FX");
        addEnvironmentCandidate(candidates, "JAVAFX_SDK");

        candidates.add(Paths.get("javafx-sdk", "lib"));
        candidates.add(Paths.get("lib", "javafx-sdk", "lib"));

        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null && !programFiles.isBlank()) {
            Path programFilesPath = Paths.get(programFiles);
            candidates.add(programFilesPath.resolve("javafx-sdk-24.0.1").resolve("lib"));
            candidates.addAll(findVersionedJavaFxLibDirectories(programFilesPath));
        }

        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        if (programFilesX86 != null && !programFilesX86.isBlank()) {
            candidates.addAll(findVersionedJavaFxLibDirectories(Paths.get(programFilesX86)));
        }

        for (Path candidate : candidates) {
            if (candidate != null) {
                Path normalizedCandidate = candidate.toAbsolutePath().normalize();
                if (Files.isDirectory(normalizedCandidate) && Files.exists(normalizedCandidate.resolve("javafx.web.jar"))) {
                    return normalizedCandidate;
                }
            }
        }

        return null;
    }

    private static void addEnvironmentCandidate(List<Path> candidates, String environmentVariable) {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) {
            return;
        }

        Path homePath = Paths.get(value);
        candidates.add(homePath);
        candidates.add(homePath.resolve("lib"));
    }

    private static List<Path> findVersionedJavaFxLibDirectories(Path parentDirectory) {
        ArrayList<Path> candidates = new ArrayList<>();
        if (!Files.isDirectory(parentDirectory)) {
            return candidates;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDirectory, "javafx-sdk-*")) {
            for (Path candidate : stream) {
                candidates.add(candidate.resolve("lib"));
            }
        }
        catch (IOException ignored) {
        }

        return candidates;
    }

    private static void relaunchProcess(Class<?> mainClass, String[] args, Path javaFxLibPath) throws IOException {
        ArrayList<String> command = new ArrayList<>();

        String javaExecutable = Paths.get(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
        command.add(javaExecutable);
        command.add("--module-path");
        command.add(javaFxLibPath.toString());
        command.add("--add-modules");
        command.add(REQUIRED_MODULES);
        command.add("--enable-native-access=" + NATIVE_ACCESS_MODULES);
        command.add("--sun-misc-unsafe-memory-access=allow");
        command.add("-D" + BOOTSTRAP_FLAG + "=true");

        String classPath = System.getProperty("java.class.path");
        if (classPath != null && !classPath.isBlank()) {
            command.add("-cp");
            command.add(classPath);
        }

        command.add(mainClass.getName());
        command.addAll(Arrays.asList(args));

        new ProcessBuilder(command)
                .directory(new File(System.getProperty("user.dir")))
                .inheritIO()
                .start();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

}
