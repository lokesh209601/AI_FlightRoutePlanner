import java.util.ArrayList;
import java.util.List;


public class CsvUtils {

   
    public static List<String> parseCsvLine(String line) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);

            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    currentField.append('"');
                    index++;
                }
                else {
                    inQuotes = !inQuotes;
                }
            }
            else if (currentChar == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            }
            else {
                currentField.append(currentChar);
            }
        }

        fields.add(currentField.toString());
        return fields;
    }

}
