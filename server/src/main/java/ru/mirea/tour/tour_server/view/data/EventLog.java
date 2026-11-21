package ru.mirea.tour.tour_server.view.data;

import ru.mirea.tour.tour_server.view.model.EventType;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Журнал принятых событий просмотра. Помимо самих событий хранит время последнего
 * события каждого сеанса и список завершённых сеансов, чтобы проверки выполнялись
 * без перебора всего журнала.
 */
public class EventLog {

    private final List<ViewEvent> events = new ArrayList<>();
    private final Map<String, Long> lastTime = new HashMap<>();
    private final Set<String> closed = new HashSet<>();

    public void add(ViewEvent event) {
        events.add(event);
        lastTime.put(event.sessionId(), event.time());
        if (event.type() == EventType.END) {
            closed.add(event.sessionId());
        }
    }

    public OptionalLong lastTime(String sessionId) {
        Long t = lastTime.get(sessionId);
        return t == null ? OptionalLong.empty() : OptionalLong.of(t);
    }

    public boolean isClosed(String sessionId) {
        return closed.contains(sessionId);
    }

    /** События одного сеанса в порядке поступления. */
    public List<ViewEvent> eventsOf(String sessionId) {
        List<ViewEvent> result = new ArrayList<>();
        for (ViewEvent e : events) {
            if (e.sessionId().equals(sessionId)) {
                result.add(e);
            }
        }
        return result;
    }

    public int size() {
        return events.size();
    }
}
