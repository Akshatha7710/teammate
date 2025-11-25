package TeamMate;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Handles file I/O operations (reading participants, saving teams).
 */
public class FileService {

    public List<Participant> loadFromCsv(String path) throws IOException {
        List<Participant> participants = new ArrayList<>();
        Path p = Path.of(path);
        if (!Files.exists(p)) throw new FileNotFoundException("CSV not found: " + path);

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String header = br.readLine(); // skip header if present
            int lineNo = 1;
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] parts = splitCsvLine(line);
                if (parts.length < 7) {
                    System.err.printf("Skipping line %d: expected at least 7 columns but found %d.%n", lineNo, parts.length);
                    continue;
                }

                try {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String email = parts[2].trim();
                    String preferredGame = parts[3].trim();

                    int skillLevel;
                    try {
                        skillLevel = Integer.parseInt(parts[4].trim());
                    } catch (NumberFormatException nfe) {
                        System.err.printf("Skipping line %d: invalid skill '%s'%n", lineNo, parts[4]);
                        continue;
                    }
                    if (skillLevel < 0 || skillLevel > 100) {
                        System.err.printf("Skipping line %d: skill out of range (0-100): %d%n", lineNo, skillLevel);
                        continue;
                    }

                    Role role;
                    try {
                        role = Role.valueOf(parts[5].trim().toUpperCase());
                    } catch (IllegalArgumentException iae) {
                        System.err.printf("Skipping line %d: unknown role '%s'%n", lineNo, parts[5]);
                        continue;
                    }

                    int personalityScore;
                    try {
                        personalityScore = Integer.parseInt(parts[6].trim());
                    } catch (NumberFormatException nfe) {
                        System.err.printf("Skipping line %d: invalid personality score '%s'%n", lineNo, parts[6]);
                        continue;
                    }
                    if (personalityScore < 0 || personalityScore > 100) {
                        System.err.printf("Skipping line %d: personality score out of range (0-100): %d%n", lineNo, personalityScore);
                        continue;
                    }

                    PersonalityType personalityType = null;
                    if (parts.length >= 8 && !parts[7].trim().isEmpty()) {
                        try {
                            personalityType = PersonalityType.valueOf(parts[7].trim().toUpperCase());
                        } catch (IllegalArgumentException iae) {
                            System.err.printf("Warning line %d: unknown personality type '%s' - will classify from score.%n", lineNo, parts[7]);
                        }
                    }

                    Participant participant = new Participant(
                            id, name, email, preferredGame,
                            role, skillLevel, personalityScore, personalityType
                    );
                    participants.add(participant);

                } catch (Exception ex) {
                    System.err.printf("Skipping line %d due to unexpected error: %s%n", lineNo, ex.getMessage());
                }
            }
        }
        return participants;
    }

    public void saveTeams(List<Team> teams, String path) throws IOException {
        Path p = Path.of(path);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            bw.write("teamId,memberId,memberName,memberInterest,memberRole,skillLevel,personalityScore,personalityType");
            bw.newLine();
            for (Team team : teams) {
                for (Participant m : team.getMembers()) {
                    String[] cols = new String[] {
                            team.getId(),
                            m.getId(),
                            m.getName(),
                            m.getInterest(),
                            m.getPreferredRole().name(),
                            Integer.toString(m.getSkillLevel()),
                            Integer.toString(m.getPersonalityScore()),
                            m.getPersonalityType().name()
                    };
                    bw.write(joinEscaped(cols));
                    bw.newLine();
                }
            }
        }
    }

    // --- CSV utilities ---
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

    private static String joinEscaped(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escapeCsv(cols[i]));
        }
        return sb.toString();
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\"") || out.contains("\n") || out.contains("\r")) {
            return "\"" + out + "\"";
        }
        return out;
    }
}