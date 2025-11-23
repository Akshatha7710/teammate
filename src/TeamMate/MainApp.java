package TeamMate;

import java.io.IOException;
import java.util.List;

/**
 * Console entrypoint for TeamMate
 */
public class MainApp {

    private static final int TEAM_SIZE = 4;
    private static final String INPUT_FILE = "participants_sample.csv";
    private static final String OUTPUT_FILE = "formed_teams.csv";

    public static void main(String[] args) {
        System.out.println("--- Team Formation Application Initiated ---");

        FileService fileService = new FileService();
        List<Participant> participants;
        try {
            participants = fileService.loadFromCsv(INPUT_FILE);
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return;
        }

        if (participants.isEmpty()) {
            System.err.println("No valid participants loaded. Exiting.");
            return;
        }
        if (participants.size() < TEAM_SIZE) {
            System.err.printf("Not enough participants (%d) to form a team of size %d.%n", participants.size(), TEAM_SIZE);
            return;
        }

        System.out.printf("Loaded %d participants.%n", participants.size());

        TeamBuilder builder = new TeamBuilder();
        List<Team> formedTeams;
        try {
            formedTeams = builder.buildTeams(participants, TEAM_SIZE);
            // Save
            fileService.saveTeams(formedTeams, OUTPUT_FILE);

            System.out.printf("Successfully formed %d teams and saved to %s%n", formedTeams.size(), OUTPUT_FILE);
            System.out.println("Teams:");
            for (Team t : formedTeams) {
                System.out.println(t);
                t.getMembers().forEach(m -> System.out.printf("  - %s | %s | %s | skill=%d | type=%s%n",
                        m.getName(), m.getInterest(), m.getPreferredRole(), m.getSkillLevel(), m.getPersonalityType()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Team building interrupted.");
        } catch (IOException e) {
            System.err.println("Error saving teams: " + e.getMessage());
        } finally {
            builder.shutdown();
        }

        System.out.println("--- Team Formation Application Complete ---");
    }
}