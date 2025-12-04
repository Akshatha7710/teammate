package TeamMate;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileService {

    public static final String INPUT_FILE = "participants_sample.csv";
    public static final String OUTPUT_FILE = "formed_teams.csv";
    private static final String PARTICIPANT_HEADER = "ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType";
    private static final String TEAM_HEADER = "TeamID,TeamSize,MemberID,Name,PreferredGame,Role,Skill,PersonalityType";

    // Loads participant records from CSV
    public List<Participant> loadParticipants(String path) throws IOException {
        List<Participant> participants = new ArrayList<>();
        Path p = Path.of(path);
        if (!Files.exists(p)) return participants;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            br.readLine(); // skip header
            String line;
            int ln = 1;

            while ((line = br.readLine()) != null) {
                ln++;
                if (line.trim().isEmpty()) continue;
                String[] cols = splitCsvLine(line);
                if (cols.length < 7) continue;

                try {
                    String id = cols[0].trim();
                    String name = cols[1].trim();
                    String email = cols.length > 2 ? cols[2].trim() : "";
                    String game = cols.length > 3 ? cols[3].trim() : "Unknown";

                    int skill = 5; try { skill = Integer.parseInt(cols[4].trim()); } catch (Exception ignored) {}
                    Role role = Role.ATTACKER; try { role = Role.valueOf(cols[5].trim().toUpperCase()); } catch (Exception ignored) {}
                    int pScore = 0; try { pScore = Integer.parseInt(cols[6].trim()); } catch (Exception ignored) {}

                    PersonalityType pType = PersonalityType.UNCLASSIFIED;
                    if (cols.length > 7 && !cols[7].trim().isEmpty()) {
                        try { pType = PersonalityType.valueOf(cols[7].trim().toUpperCase()); } catch (Exception ignored) {}
                    }

                    participants.add(new Participant(id, name, email, game, role, skill, pScore, pType));

                } catch (Exception ex) {
                    AppLogger.warning("Skipping bad participant line " + ln);
                }
            }
        }
        return participants;
    }

    // Loads teams from CSV and maps members using participant list
    public List<Team> loadTeams(String path, List<Participant> participants) throws IOException {
        Path p = Path.of(path);
        if (!Files.exists(p)) return new ArrayList<>();

        List<Team> teams = new ArrayList<>();
        Map<String, Team> map = new HashMap<>();
        Map<String, Participant> byId = new HashMap<>();
        participants.forEach(pt -> byId.put(pt.getId(), pt));

        try (BufferedReader br = Files.newBufferedReader(p)) {
            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = splitCsvLine(line);
                if (cols.length < 8) continue;

                String teamId = cols[0].trim();
                String memberId = cols[2].trim();

                Team t = map.get(teamId);
                if (t == null) {
                    t = new Team();
                    map.put(teamId, t);
                    teams.add(t);
                }

                Participant member = byId.get(memberId);
                if (member != null) t.addMember(member);
            }
        }
        return teams;
    }

    // Saves participant list to CSV
    public void saveParticipants(List<Participant> participants, String path) throws IOException {
        Path p = Path.of(path);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            bw.write(PARTICIPANT_HEADER);
            bw.newLine();

            for (Participant pt : participants) {
                String[] cols = new String[]{
                        pt.getId(), pt.getName(), pt.getEmail(), pt.getInterest(),
                        String.valueOf(pt.getSkillLevel()), pt.getPreferredRole().name(),
                        String.valueOf(pt.getPersonalityScore()), pt.getPersonalityType().name()
                };
                bw.write(joinEscaped(cols));
                bw.newLine();
            }
        }
    }

    // Saves team data to CSV
    public void saveTeams(List<Team> teams, String path) throws IOException {
        Path p = Path.of(path);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            bw.write(TEAM_HEADER);
            bw.newLine();

            for (Team t : teams) {
                for (Participant m : t.getMembers()) {
                    String[] cols = new String[]{
                            t.getId(), String.valueOf(t.size()), m.getId(), m.getName(),
                            m.getInterest(), m.getPreferredRole().name(), String.valueOf(m.getSkillLevel()),
                            m.getPersonalityType().name()
                    };
                    bw.write(joinEscaped(cols));
                    bw.newLine();
                }
            }
        }
    }

    // CSV parsing (handles quoted fields)
    private static String[] splitCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            }
            else if (c == ',' && !inQuotes) {
                cols.add(cur.toString());
                cur.setLength(0);
            }
            else {
                cur.append(c);
            }
        }

        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }

    // Escapes a CSV field if needed
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
        if (out.contains(",") || out.contains("\"") || out.contains("\n") || out.contains("\r"))
            return "\"" + out + "\"";
        return s;
    }
}
