package TeamMate;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private static int counter = 1;
    private String id;
    private List<Participant> members = new ArrayList<>();

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

    @Override
    public String toString() {
        return id + " -> " + members;
    }
}