package TeamMate;
import java.io.*;
import java.util.*;

public class CSVReader {

    public static List<Participant> readParticipants(String filePath) {
        List<Participant> participants = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                // Skip header
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] data = line.split(",");

                if (data.length < 8) continue;

                Participant p = new Participant(
                        data[0], // ID
                        data[1], // Name
                        data[2], // Email
                        data[3], // PreferredGame
                        Integer.parseInt(data[4]), // SkillLevel
                        data[5], // PreferredRole
                        Integer.parseInt(data[6]), // PersonalityScore
                        data[7]  // PersonalityType
                );
                participants.add(p);
            }
        } catch (IOException e) {
            System.out.println("Error reading CSV: " + e.getMessage());
        }

        return participants;
    }
}
