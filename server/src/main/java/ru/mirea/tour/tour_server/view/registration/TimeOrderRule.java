package ru.mirea.tour.tour_server.view.registration;

import ru.mirea.tour.tour_server.view.data.EventLog;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.Optional;
import java.util.OptionalLong;

/** Время события не может быть меньше времени предыдущего события того же сеанса. */
public class TimeOrderRule implements EventRule {

    private final EventLog log;

    public TimeOrderRule(EventLog log) {
        this.log = log;
    }

    @Override
    public Optional<String> check(ViewEvent event) {
        OptionalLong prev = log.lastTime(event.sessionId());
        if (prev.isPresent() && event.time() < prev.getAsLong()) {
            return Optional.of("время меньше времени предыдущего события сеанса");
        }
        return Optional.empty();
    }
}
