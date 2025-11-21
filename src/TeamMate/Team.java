package TeamMate;

import java.util.ArrayList;
import java.util.List;

public class Team {
    // The counter must be static to ensure unique IDs across all team instances
    private static int counter = 1;
    private final String id;
    private final List<Participant> members = new ArrayList<>();

    public Team() {
        this.id = "T" + counter++;
    }

    public void addMember(Participant p) {
        members.add(p);
    }

    public List<Participant> getMembers() {
        return members;
    }

    public int size() {
        return members.size();
    }

    public String getId() {
        return id;
    }

    /**
     * Resets the static team counter, necessary for clean re-runs of the main application.
     */
    public static void resetCounter() {
        counter = 1;
    }

    @Override
    public String toString() {
        // Enhanced output to show size
        return id + " [" + size() + " members] -> " + members;
    }
}