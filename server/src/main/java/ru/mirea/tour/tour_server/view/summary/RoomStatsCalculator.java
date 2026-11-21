package ru.mirea.tour.tour_server.view.summary;

import ru.mirea.tour.tour_server.view.model.EventType;
import ru.mirea.tour.tour_server.view.model.RoomStats;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Алгоритм 2: время и число заходов по помещениям для событий одного сеанса.
 * Пребывание в помещении длится от входа в него до входа в другое помещение или до конца сеанса.
 * Если события END нет, сеанс считается завершённым в момент последнего события.
 */
public class RoomStatsCalculator {

    /** @return помещение -> статистика, в порядке первого посещения */
    public Map<String, RoomStats> calculate(List<ViewEvent> sessionEvents) {
        List<ViewEvent> events = new ArrayList<>(sessionEvents);
        events.sort(Comparator.comparingLong(ViewEvent::time)); // устойчивая сортировка

        Map<String, RoomStats> stats = new LinkedHashMap<>();
        String currentRoom = null;
        long enteredAt = 0;
        long lastTime = 0;

        for (ViewEvent e : events) {
            lastTime = e.time();
            if (e.type() == EventType.ENTER_ROOM) {
                if (currentRoom != null) {
                    stats.get(currentRoom).addSeconds(e.time() - enteredAt);
                }
                if (!e.roomId().equals(currentRoom)) {
                    stats.computeIfAbsent(e.roomId(), k -> new RoomStats()).addVisit();
                }
                currentRoom = e.roomId();
                enteredAt = e.time();
            } else if (e.type() == EventType.END) {
                if (currentRoom != null) {
                    stats.get(currentRoom).addSeconds(e.time() - enteredAt);
                }
                currentRoom = null;
                break;
            }
            // SELECT_FINISH на время пребывания не влияет
        }
        if (currentRoom != null) {
            stats.get(currentRoom).addSeconds(lastTime - enteredAt);
        }
        return stats;
    }
}
