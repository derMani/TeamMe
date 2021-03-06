package de.pasligh.android.teamme.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Game implements Serializable {

    private static final long serialVersionUID = -3420071497019663100L;

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    private Date startedAt;
    private List<PlayerAssignment> assignments;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id;

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    private String sport;

    public Game() {
        super();
    }

    public Game(Set<PlayerAssignment> p_assignments) {
        super();
        assignments = new ArrayList<PlayerAssignment>();
        startedAt = new Date();
        sport = "unknown";
        Iterator<PlayerAssignment> iterator = p_assignments.iterator();
        while (iterator.hasNext()) {
            assignments.add(iterator.next());
        }
    }

    /**
     * @return the startedAt
     */
    public Date getStartedAt() {
        return startedAt;
    }

    /**
     * @return the assignments
     */
    public List<PlayerAssignment> getAssignments() {
        return assignments;
    }

}
