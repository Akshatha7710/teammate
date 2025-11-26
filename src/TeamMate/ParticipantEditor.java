package TeamMate;

import java.util.List;
import java.util.Scanner;

/** Utility class to edit or remove participants from a list */
public class ParticipantEditor {

    /** Edits participant details via console prompts */
    public static void editParticipant(List<Participant> participants, Scanner scanner) {
        System.out.print("Enter Participant ID to edit: ");
        String pid = scanner.nextLine().trim();

        // Find participant by ID
        Participant p = participants.stream().filter(x -> x.getId().equalsIgnoreCase(pid)).findFirst().orElse(null);
        if (p == null) {
            System.out.println("Participant ID not found.");
            return;
        }

        System.out.println("Editing participant: " + p.getName());

        // Update fields if user provides input
        System.out.print("New Name (current: " + p.getName() + "): ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) p.setName(name);

        System.out.print("New Email (current: " + p.getEmail() + "): ");
        String email = scanner.nextLine().trim();
        if (!email.isEmpty()) p.setEmail(email);

        System.out.print("New Interest (current: " + p.getInterest() + "): ");
        String interest = scanner.nextLine().trim();
        if (!interest.isEmpty()) p.setInterest(interest);

        System.out.print("New Skill (0-100) (current: " + p.getSkillLevel() + "): ");
        String skillStr = scanner.nextLine().trim();
        if (!skillStr.isEmpty()) p.setSkillLevel(Integer.parseInt(skillStr));

        System.out.print("New Role (ATTACKER, DEFENDER, STRATEGIST, SUPPORT, LEADER) (current: " + p.getPreferredRole() + "): ");
        String roleStr = scanner.nextLine().trim();
        if (!roleStr.isEmpty()) {
            try {
                p.setPreferredRole(Role.valueOf(roleStr.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                System.out.println("Invalid role. Keeping previous value.");
            }
        }

        System.out.print("New Personality Score (0-100) (current: " + p.getPersonalityScore() + "): ");
        String scoreStr = scanner.nextLine().trim();
        if (!scoreStr.isEmpty()) p.setPersonalityScore(Integer.parseInt(scoreStr));

        System.out.println("Participant updated: " + p);
    }

    /** Removes participant from list by ID */
    public static void removeParticipant(List<Participant> participants, Scanner scanner) {
        System.out.print("Enter Participant ID to remove: ");
        String pid = scanner.nextLine().trim();

        boolean removed = participants.removeIf(p -> p.getId().equalsIgnoreCase(pid));
        if (removed) System.out.println("Participant removed successfully.");
        else System.out.println("Participant ID not found.");
    }
}
