package TeamMate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Team {
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
        return Collections.unmodifiableList(members);
    }

    public int size() { return members.size(); }

    public String getId() { return id; }

    public static void resetCounter() { counter = 1; }

    @Override
    public String toString() {
        return id + " [" + size() + " members]";
    }
}