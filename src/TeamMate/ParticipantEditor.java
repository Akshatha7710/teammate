package TeamMate;

import java.util.List;
import java.util.Scanner;

public class ParticipantEditor {

    private static final List<String> GAME_OPTIONS = List.of("CS:GO", "FIFA", "DOTA 2", "Basketball", "Valorant","Chess");
    private static final List<Role> ROLE_OPTIONS = List.of(Role.ATTACKER, Role.DEFENDER, Role.STRATEGIST, Role.SUPPORTER, Role.COORDINATOR);

    // Edit participant
    public static void editParticipant(List<Participant> participants, Scanner scanner) {
        System.out.print("Enter participant ID to edit: ");
        String id = scanner.nextLine().trim();

        if (!id.matches("P\\d+")) {
            System.out.println("Invalid Participant ID");
            return;
        }

        Participant p = participants.stream().filter(x -> x.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (p == null) {
            System.out.println("ID not available in list.");
            return;
        }

        System.out.println("Editing: " + p);

        // Edit Name
        System.out.print("New name (enter to skip): ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) p.setName(name);

        // Edit Preferred Game
        System.out.println("Choose new preferred game (input number between 1-6 or press enter to skip):");
        for (int i = 0; i < GAME_OPTIONS.size(); i++) {
            System.out.println((i + 1) + ") " + GAME_OPTIONS.get(i));
        }
        System.out.print("Choice: ");
        String gameChoice = scanner.nextLine().trim();
        if (!gameChoice.isEmpty() && !gameChoice.equals("0")) {
            try {
                int idx = Integer.parseInt(gameChoice) - 1;
                if (idx >= 0 && idx < GAME_OPTIONS.size()) {
                    p.setInterest(GAME_OPTIONS.get(idx));
                } else {
                    System.out.println("Enter a number between 1 and " + GAME_OPTIONS.size());
                }
            } catch (Exception e) {
                System.out.println("Enter a number between 1 and " + GAME_OPTIONS.size());
            }
        }

        // Edit Role
        System.out.println("Choose new role (input a number between 1-5 or press enter to skip):");
        for (int i = 0; i < ROLE_OPTIONS.size(); i++) {
            System.out.println((i + 1) + ") " + ROLE_OPTIONS.get(i));
        }
        System.out.print("Choice: ");
        String roleChoice = scanner.nextLine().trim();
        if (!roleChoice.isEmpty() && !roleChoice.equals("0")) {
            try {
                int idx = Integer.parseInt(roleChoice) - 1;
                if (idx >= 0 && idx < ROLE_OPTIONS.size()) {
                    p.setPreferredRole(ROLE_OPTIONS.get(idx));
                } else {
                    System.out.println("Enter a number between 1 and " + ROLE_OPTIONS.size());
                }
            } catch (Exception e) {
                System.out.println("Enter a number between 1 and " + ROLE_OPTIONS.size());
            }
        }

        // **********************************************
        // NEW CODE ADDED: Edit Skill Level
        // **********************************************
        System.out.print("New Skill Level (1-10, enter to skip): ");
        String skillInput = scanner.nextLine().trim();
        if (!skillInput.isEmpty()) {
            try {
                int newSkill = Integer.parseInt(skillInput);
                if (newSkill >= 1 && newSkill <= 10) {
                    p.setSkillLevel(newSkill);
                    System.out.println("Skill level updated to " + newSkill + ".");
                } else {
                    System.out.println("Skill level must be between 1 and 10.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format for skill level.");
            }
        }
        // **********************************************

        System.out.println("Personality cannot be edited.");
        System.out.println("Participant " + p.getId() + " has been edited.");
        AppLogger.info("Participant edited: " + p.getId());
    }

    // Remove participant
    public static void removeParticipant(List<Participant> participants, List<Team> teams, List<Participant> unformed, Scanner scanner) {
        System.out.print("Enter participant ID to remove: ");
        String id = scanner.nextLine().trim();

        if (!id.matches("P\\d+")) {
            System.out.println("Invalid participant ID.");
            return;
        }

        Participant p = participants.stream().filter(x -> x.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (p == null) {
            System.out.println("ID not available in list.");
            return;
        }

        // Remove from any team
        for (Team t : teams) {
            if (t.getMembers().stream().anyMatch(m -> m.getId().equalsIgnoreCase(id))) {
                t.removeMember(p);
                AppLogger.info("Removed participant " + id + " from team " + t.getId());
            }
        }

        participants.remove(p);
        unformed.removeIf(x -> x.getId().equalsIgnoreCase(id));
        AppLogger.info("Participant removed: " + id);
        System.out.println("Participant " + id + " has been removed successfully.");

    }
}
