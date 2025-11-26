package TeamMate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Represents a team of participants */
public class Team {
    private static int counter = 1; // used to assign unique team IDs
    private final String id;         // team ID
    private final List<Participant> members = new ArrayList<>();

    /** Creates a new team with unique ID */
    public Team() {
        this.id = "T" + counter++;
    }

    /** Adds a participant to the team */
    public void addMember(Participant p) {
        members.add(p);
    }

    /** Returns an unmodifiable list of team members */
    public List<Participant> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /** Returns number of members */
    public int size() { return members.size(); }

    /** Returns team ID */
    public String getId() { return id; }

    /** Resets team counter (useful for testing) */
    public static void resetCounter() { counter = 1; }

    /** Short string representation of team */
    @Override
    public String toString() {
        return id + " [" + size() + " members]";
    }
}
