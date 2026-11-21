package ru.mirea.tour.tour_server.view.model;

/** Сколько секунд покупатель провёл в помещении и сколько раз в него заходил. */
public class RoomStats {

    private long seconds;
    private int visits;

    public void addSeconds(long value) {
        seconds += value;
    }

    public void addVisit() {
        visits++;
    }

    public long seconds() {
        return seconds;
    }

    public int visits() {
        return visits;
    }
}
