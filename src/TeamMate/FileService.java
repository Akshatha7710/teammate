package TeamMate;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileService {

    public static final String INPUT_FILE = "participants_sample.csv";
    private static final String PARTICIPANT_HEADER = "ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType";
    // NEW HEADER: Added PreferredGame (Game) column
    private static final String TEAM_HEADER = "TeamID,TeamSize,MemberID,Name,PreferredGame,Role,Skill,PersonalityType";


    /** * Load participants from CSV, validates data, defaults unknown personality types to UNCLASSIFIED.
     * Renamed from loadFromCsv to resolve naming issues.
     */
    public List<Participant> loadParticipants(String path) throws IOException {
        List<Participant> participants = new ArrayList<>();
        Path p = Path.of(path);
        if (!Files.exists(p)) throw new FileNotFoundException("CSV not found: " + path);

        try (BufferedReader br = Files.newBufferedReader(p)) {
            br.readLine(); // skip header
            int lineNo = 1;
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] parts = splitCsvLine(line);

                if (parts.length < 7) {
                    System.err.printf("Skipping line %d: expected 7 columns but found %d%n", lineNo, parts.length);
                    continue;
                }

                try {
                    Participant participant = parseParticipant(parts, lineNo);
                    if (participant != null) participants.add(participant);
                } catch (Exception ex) {
                    System.err.printf("Error parsing participant data on line %d: %s%n", lineNo, ex.getMessage());
                }
            }
        }
        return participants;
    }

    /** Saves a list of participants to the specified CSV file. */
    public void saveParticipants(List<Participant> participants, String path) throws IOException {
        Path p = Path.of(path);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            bw.write(PARTICIPANT_HEADER);
            bw.newLine();
            for (Participant participant : participants) {
                String[] cols = new String[]{
                        participant.getId(),
                        participant.getName(),
                        participant.getEmail(),
                        participant.getInterest(),
                        String.valueOf(participant.getSkillLevel()),
                        participant.getPreferredRole().name(),
                        String.valueOf(participant.getPersonalityScore()),
                        participant.getPersonalityType().name()
                };
                bw.write(joinEscaped(cols));
                bw.newLine();
            }
        }
    }

    /** Saves a list of teams to the specified CSV file.
     * UPDATED to include PreferredGame/Interest.
     */
    public void saveTeams(List<Team> teams, String path) throws IOException {
        Path p = Path.of(path);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            // Use the corrected header
            bw.write(TEAM_HEADER);
            bw.newLine();
            for (Team team : teams) {
                for (Participant member : team.getMembers()) {
                    String[] cols = new String[]{
                            team.getId(),
                            String.valueOf(team.size()),
                            member.getId(),
                            member.getName(),
                            member.getInterest(), // <-- ADDED THE MISSING GAME/INTEREST
                            member.getPreferredRole().name(),
                            String.valueOf(member.getSkillLevel()),
                            member.getPersonalityType().name()
                    };
                    bw.write(joinEscaped(cols));
                    bw.newLine();
                }
            }
        }
    }

    // --- Private Helper Methods ---

    /** Parses a single participant line from CSV. */
    private Participant parseParticipant(String[] parts, int lineNo) {
        String id = parts[0].trim();
        String name = parts[1].trim();
        String email = parts[2].trim();
        String preferredGame = parts[3].trim();

        // CSV Order: SkillLevel (index 4), PreferredRole (index 5)
        int skillLevel = parseIntInRange(parts[4], 0, 100, "skillLevel", lineNo);
        if (skillLevel == -1) return null;

        Role role = parseRole(parts[5], lineNo);
        if (role == null) return null;

        int personalityScore = parseIntInRange(parts[6], 0, 100, "personalityScore", lineNo);
        if (personalityScore == -1) return null;

        // PersonalityType is optional for input, defaults to UNCLASSIFIED if not provided or invalid
        PersonalityType personalityType = PersonalityType.UNCLASSIFIED;
        if (parts.length > 7 && !parts[7].trim().isEmpty()) {
            try {
                personalityType = PersonalityType.valueOf(parts[7].trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                System.err.printf("Warning on line %d: Invalid PersonalityType '%s'. Using UNCLASSIFIED.%n", lineNo, parts[7].trim());
            }
        }

        // Constructor order: (id, name, email, interest, role, skill, score, type)
        return new Participant(id, name, email, preferredGame, role, skillLevel, personalityScore, personalityType);
    }

    /** Parses a string to a Role enum safely. */
    private Role parseRole(String value, int lineNo) {
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.printf("Error on line %d: Invalid Role value '%s'.%n", lineNo, value.trim());
            return null;
        }
    }

    /** Parses a string to an integer, checking range validity. */
    private int parseIntInRange(String value, int min, int max, String field, int lineNo) {
        try {
            int num = Integer.parseInt(value.trim());
            if (num < min || num > max) {
                System.err.printf("Error on line %d: %s value %d is outside the valid range [%d-%d].%n", lineNo, field, num, min, max);
                return -1;
            }
            return num;
        } catch (NumberFormatException e) {
            System.err.printf("Error on line %d: %s value '%s' is not a valid integer.%n", lineNo, field, value.trim());
            return -1;
        }
    }

    /** Splits a CSV line, handling quotes */
    private static String[] splitCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    cur.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }

    /** Join array into CSV line with escaping */
    private static String joinEscaped(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escapeCsv(cols[i]));
        }
        return sb.toString();
    }

    /** Escape CSV special characters */
    private static String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\"") || out.contains("\n") || out.contains("\r")) {
            return "\"" + out + "\"";
        }
        return s;
    }
}