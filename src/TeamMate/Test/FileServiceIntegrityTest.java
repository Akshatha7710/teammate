package TeamMate.Test;

import TeamMate.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

public class FileServiceIntegrityTest {

    private final FileService fileService = new FileService();
    private static final String TEST_CSV = "test_integrity_check.csv";

    // Create a participant whose name requires CSV escaping (contains a comma)
    private Participant createEscapedParticipant() {
        return new Participant("P101", "Test,Comma User", "comma@test.org",
                "DOTA 2", Role.STRATEGIST, 75, PersonalityType.BALANCED);
    }

    @Test
    void testParticipantSaveAndLoadIntegrity() throws IOException {
        Path tempPath = Path.of(TEST_CSV);
        Files.deleteIfExists(tempPath); // Clean up any previous test file

        Participant original = createEscapedParticipant();
        List<Participant> originalList = List.of(original);

        // 1. Save the participant list
        fileService.saveParticipants(originalList, TEST_CSV);

        // 2. Check if the file was created
        assertTrue("Test CSV file must exist after saving.", Files.exists(tempPath));

        // 3. Load the participant list back
        List<Participant> loadedList = fileService.loadParticipants(TEST_CSV);

        // 4. Assert: Check that the loaded data matches the original data
        assertEquals(1, loadedList.size(), "Should load exactly one participant.");
        Participant loaded = loadedList.getFirst();

        // The most critical check: ensure the name with the comma was saved and loaded correctly
        assertEquals(original.getName(), loaded.getName(),
                "Saved and loaded name must be identical (CSV escaping check).");
        assertEquals(original.getId(), loaded.getId());
        assertEquals(original.getSkillLevel(), loaded.getSkillLevel());
        assertEquals(original.getPreferredRole(), loaded.getPreferredRole());

        // Cleanup
        Files.deleteIfExists(tempPath);
    }
}