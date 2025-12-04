package TeamMate.Test;

import TeamMate.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TeamBuilderTest {

    private final TeamBuilder teamBuilder = new TeamBuilder();
    private static final int MIN_SIZE = 3;

    // Helper method to quickly create a test participant with a specific personality
    private Participant createParticipantWithPersonality(String id, PersonalityType type) {
        // Use default values for other fields
        return new Participant(id, "TestName", "test@a.com", "GameX", Role.ATTACKER, 50, type);
    }

    @Test
    void testBuildTeamsAndValidate_ThrowsOnInvalidMinSize() {
        // Test boundary condition: team size is 2 (below the minimum of 3)
        int invalidSize = MIN_SIZE - 1;

        // Assert that the specific TeamMateException is thrown
        TeamMateException thrown = assertThrows(
                TeamMateException.class,
                () -> teamBuilder.buildTeamsAndValidate(Collections.emptyList(), invalidSize),
                "Should throw TeamMateException for team size < 3"
        );

        // Verify the message the user sees is correct
        assertTrue(thrown.getMessage().contains("Team size must be at least " + MIN_SIZE));
    }

    @Test
    void testBuildTeamsAndValidate_AllowsValidMinSize() {
        // Test minimum valid size
        int validSize = MIN_SIZE; // size is 3

        // 🚨 FIX: Create a list of participants that satisfies the business rule for size 3 teams:
        // Must have at least 1 LEADER, 1 THINKER, and one other (e.g., BALANCED or another LEADER/THINKER)
        List<Participant> validParticipants = List.of(
                createParticipantWithPersonality("P1", PersonalityType.LEADER),
                createParticipantWithPersonality("P2", PersonalityType.THINKER),
                createParticipantWithPersonality("P3", PersonalityType.BALANCED)
        );

        // Assert that NO exception is thrown
        assertDoesNotThrow(
                () -> teamBuilder.buildTeamsAndValidate(validParticipants, validSize),
                "Should not throw exception for team size >= 3 when business rules are met"
        );
    }
}