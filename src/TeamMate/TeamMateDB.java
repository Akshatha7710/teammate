package TeamMate;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A light-weight in-memory database stub, now persistent via Java Serialization.
 * Data is saved to 'teammate_data.ser' in the project root.
 */
public class TeamMateDB {

    // File name where the database object will be saved
    private static final String DB_FILE_NAME = "teammate_data.ser";

    private final Map<String, Participant> participants = new ConcurrentHashMap<>();
    private final Map<String, Team> teams = new ConcurrentHashMap<>();

    // Constructor: loads data from disk on creation
    public TeamMateDB() {
        loadFromDisk();
    }

    // --- PERSISTENCE METHODS ---

    /** Loads the entire database state from the serialization file. */
    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        File file = new File(DB_FILE_NAME);
        if (!file.exists()) {
            AppLogger.info("DB: No existing data file found (" + DB_FILE_NAME + "). Starting fresh.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            // Read the data in the exact order it was saved
            Object readParticipants = ois.readObject();
            Object readTeams = ois.readObject();

            if (readParticipants instanceof ConcurrentHashMap) {
                participants.putAll((ConcurrentHashMap<String, Participant>) readParticipants);
            }
            if (readTeams instanceof ConcurrentHashMap) {
                teams.putAll((ConcurrentHashMap<String, Team>) readTeams);
            }

            AppLogger.info("DB: Loaded " + participants.size() + " participants and " + teams.size() + " teams from disk.");

            // Re-initialize Team counter to prevent ID conflicts on new teams
            Team.initializeCounter(findAllTeams());

        } catch (IOException | ClassNotFoundException e) {
            AppLogger.error("DB: Failed to load data from " + DB_FILE_NAME + ". Starting fresh.", e);
        }
    }

    /** Saves the entire database state to the serialization file. Called by the Shutdown Hook. */
    public void saveToDisk() throws TeamMateDBException {
        try (FileOutputStream fos = new FileOutputStream(DB_FILE_NAME);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            // Write the data
            oos.writeObject(participants);
            oos.writeObject(teams);

            AppLogger.info("DB: Database successfully serialized to " + DB_FILE_NAME);

        } catch (IOException e) {
            throw new TeamMateDBException("Failed to save database to disk.", e);
        }
    }

    // ==================== PARTICIPANT OPERATIONS ====================

    public void saveParticipant(Participant p) throws TeamMateDBException {
        if (p == null)
            throw new TeamMateDBException("Cannot save NULL participant");
        if (p.getId() == null || p.getId().isBlank())
            throw new TeamMateDBException("Participant ID cannot be empty");

        participants.put(p.getId(), p);
        AppLogger.info("DB: Saved participant " + p.getId());
    }

    public Participant findParticipant(String id) throws TeamMateDBException {
        if (id == null || id.isBlank())
            throw new TeamMateDBException("Participant ID cannot be empty");

        Participant p = participants.get(id);
        if (p == null)
            throw new TeamMateDBException("Participant " + id + " not found");

        return p;
    }

    public List<Participant> findAllParticipants() {
        return new ArrayList<>(participants.values());
    }

    public void deleteParticipant(String id) throws TeamMateDBException {
        if (!participants.containsKey(id))
            throw new TeamMateDBException("Participant does not exist: " + id);

        participants.remove(id);
        AppLogger.warning("DB: Deleted participant " + id);
    }

    // ==================== TEAM OPERATIONS ====================

    public void saveTeam(Team team) throws TeamMateDBException {
        if (team == null)
            throw new TeamMateDBException("Cannot save NULL team");

        if (team.getId() == null || team.getId().isBlank()) // CORRECTED: team.getId()
            throw new TeamMateDBException("Team ID cannot be empty");

        teams.put(team.getId(), team); // CORRECTED: team.getId()
        AppLogger.info("DB: Saved team " + team.getId()); // CORRECTED: team.getId()
    }

    public Team findTeam(String id) throws TeamMateDBException {
        if (id == null || id.isBlank())
            throw new TeamMateDBException("Team ID cannot be empty");

        Team t = teams.get(id);
        if (t == null)
            throw new TeamMateDBException("Team not found: " + id);

        return t;
    }

    public List<Team> findAllTeams() {
        return new ArrayList<>(teams.values());
    }

    public void deleteTeam(String id) throws TeamMateDBException {
        if (!teams.containsKey(id))
            throw new TeamMateDBException("Team does not exist: " + id);

        teams.remove(id);
        AppLogger.warning("DB: Deleted team " + id);
    }
}