package TeamMate.Test;

import TeamMate.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TeamTest {

    // Helper method to quickly create a test participant
    private Participant createParticipant(String id, Role role, int skill) {
        return new Participant(id, "TestName", "test@a.com", "GameX", role, skill, PersonalityType.BALANCED);
    }

    @Test
    void testAverageSkill_WithMembers() {
        Team team = new Team();
        // Skills: 60, 80, 70
        team.addMember(createParticipant("P1", Role.ATTACKER, 60));
        team.addMember(createParticipant("P2", Role.DEFENDER, 80));
        team.addMember(createParticipant("P3", Role.STRATEGIST, 70));

        // Expected average: (60 + 80 + 70) / 3 = 70.0
        // The delta (0.001) is used for comparing floating-point numbers
        assertEquals(70.0, team.averageSkill(), 0.001, "Average skill should be calculated correctly.");
    }

    @Test
    void testAverageSkill_NoMembers() {
        Team team = new Team();
        // An empty team must return 0.0
        assertEquals(0.0, team.averageSkill(), "Average skill for an empty team must be 0.0.");
    }

    @Test
    void testRolesPresent_CountsUniqueRoles() {
        Team team = new Team();
        team.addMember(createParticipant("P1", Role.ATTACKER, 50));
        team.addMember(createParticipant("P2", Role.DEFENDER, 50));
        team.addMember(createParticipant("P3", Role.ATTACKER, 50)); // Duplicate role
        team.addMember(createParticipant("P4", Role.SUPPORTER, 50));

        // Should count 3 unique roles: ATTACKER, DEFENDER, SUPPORTER
        Set<Role> roles = team.rolesPresent();
        assertEquals(3, roles.size(), "Should count only the number of unique roles.");
        assertTrue(roles.contains(Role.ATTACKER));
        assertFalse(roles.contains(Role.COORDINATOR));
    }

    @Test
    void testTeamIDIncrement() {
        // Use the static reset method to ensure test independence
        Team.resetCounter();

        Team t1 = new Team();
        Team t2 = new Team();

        assertEquals("T1", t1.getId());
        assertEquals("T2", t2.getId());
    }
}