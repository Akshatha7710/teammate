package TeamMate;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileService {

    /** Load participants from CSV, validates data, defaults unknown personality types to UNCLASSIFIED */
    public List<Participant> loadFromCsv(String path) throws IOException {
        List<Participant> participants = new ArrayList<>();
        Path p = Path.of(path);
        if (!Files.exists(p)) throw new FileNotFoundException("CSV not found: " + path);

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String header = br.readLine(); // skip header
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
                    System.err.printf("Skipping line %d: %s%n", lineNo, ex.getMessage());
                }
            }
        }
        return participants;
    }

    /** Parse a single participant from CSV columns */
    private Participant parseParticipant(String[] parts, int lineNo) {
        String id = parts[0].trim();
        String name = parts[1].trim();
        String email = parts[2].trim();
        String preferredGame = parts[3].trim();

        int skillLevel = parseIntInRange(parts[4], 0, 100, "skillLevel", lineNo);
        if (skillLevel == -1) return null;

        Role role = parseRole(parts[5], lineNo);
        if (role == null) return null;

        int personalityScore = parseIntInRange(parts[6], 0, 100, "personalityScore", lineNo);
        if (personalityScore == -1) return null;

        PersonalityType personalityType = PersonalityType.UNCLASSIFIED; // default
        if (parts.length >= 8 && !parts[7].trim().isEmpty()) {
            try {
                personalityType = PersonalityType.valueOf(parts[7].trim().toUpperCase());
            } catch (IllegalArgumentException iae) {
                System.err.printf("Warning line %d: unknown personality type '%s'. Defaulting to UNCLASSIFIED.%n",
                        lineNo, parts[7]);
            }
        }

        return new Participant(id, name, email, preferredGame, role, skillLevel, personalityScore, personalityType);
    }

    /** Parse integer with range check */
    private int parseIntInRange(String value, int min, int max, String field, int lineNo) {
        try {
            int num = Integer.parseInt(value.trim());
            if (num < min || num > max) {
                System.err.printf("Line %d: %s out of range (%d-%d): %d%n", lineNo, field, min, max, num);
                return -1;
            }
            return num;
        } catch (NumberFormatException nfe) {
            System.err.printf("Line %d: invalid %s '%s'%n", lineNo, field, value);
            return -1;
        }
    }

    /** Parse role from string */
    private Role parseRole(String value, int lineNo) {
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException iae) {
            System.err.printf("Line %d: unknown role '%s'%n", lineNo, value);
            return null;
        }
    }

    /** Save list of teams to CSV */
    public void saveTeams(List<Team> teams, String path) throws IOException {
        Path p = Path.of(path);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            bw.write("teamId,memberId,memberName,memberInterest,memberRole,skillLevel,personalityScore,personalityType");
            bw.newLine();
            for (Team team : teams) {
                for (Participant m : team.getMembers()) {
                    String[] cols = new String[]{
                            team.getId(),
                            m.getId(),
                            m.getName(),
                            m.getInterest(),
                            m.getPreferredRole().name(),
                            String.valueOf(m.getSkillLevel()),
                            String.valueOf(m.getPersonalityScore()),
                            m.getPersonalityType() != null ? m.getPersonalityType().name() : "UNCLASSIFIED"
                    };
                    bw.write(joinEscaped(cols));
                    bw.newLine();
                }
            }
        }
    }

    /** Split CSV line handling quotes */
    private static String[] splitCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
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
        return out;
    }
}
