package TeamMate;

import java.util.List;
import java.util.Scanner;

public class ParticipantEditor {

    public static void editParticipant(List<Participant> participants, Scanner scanner) {
        System.out.print("Enter Participant ID to edit: ");
        String pid = scanner.nextLine().trim();

        Participant p = participants.stream().filter(x -> x.getId().equalsIgnoreCase(pid)).findFirst().orElse(null);
        if (p == null) {
            System.out.println("Participant ID not found.");
            return;
        }

        System.out.println("Editing participant: " + p.getName());

        System.out.print("New Name (current: " + p.getName() + "): ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) p.setName(name);

        System.out.print("New Email (current: " + p.getEmail() + "): ");
        String email = scanner.nextLine().trim();
        if (!email.isEmpty()) p.setEmail(email);

        System.out.print("New Interest (current: " + p.getInterest() + "): ");
        String interest = scanner.nextLine().trim();
        if (!interest.isEmpty()) p.setInterest(interest);

        // *** SKILL LEVEL 1-10 UPDATE ***
        System.out.print("New Skill (1-10) (current: " + p.getSkillLevel() + "): ");
        String skillStr = scanner.nextLine().trim();
        if (!skillStr.isEmpty()) {
            try {
                int skill = Integer.parseInt(skillStr);
                if (skill >= 1 && skill <= 10) {
                    p.setSkillLevel(skill);
                } else {
                    System.out.println("Skill must be between 1 and 10. Keeping previous value.");
                }
            } catch (NumberFormatException ignored) {
                System.out.println("Invalid skill input. Keeping previous value.");
            }
        }

        // Prompt to match the Role.java enum
        System.out.print("New Role (ATTACKER, DEFENDER, STRATEGIST, SUPPORTER, COORDINATOR) (current: " + p.getPreferredRole() + "): ");
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
        if (!scoreStr.isEmpty()) {
            try {
                p.setPersonalityScore(Integer.parseInt(scoreStr));
            } catch (NumberFormatException ignored) {
                System.out.println("Invalid personality score. Keeping previous value.");
            }
        }

        System.out.println("Participant updated: " + p);
    }

    public static void removeParticipant(List<Participant> participants, Scanner scanner) {
        System.out.print("Enter Participant ID to remove: ");
        String pid = scanner.nextLine().trim();

        boolean removed = participants.removeIf(p -> p.getId().equalsIgnoreCase(pid));
        if (removed) System.out.println("Participant removed successfully.");
        else System.out.println("Participant ID not found.");
    }
}