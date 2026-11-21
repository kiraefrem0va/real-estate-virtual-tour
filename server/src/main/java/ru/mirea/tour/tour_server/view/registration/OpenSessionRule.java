package ru.mirea.tour.tour_server.view.registration;

import ru.mirea.tour.tour_server.view.data.EventLog;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.Optional;

/** После события END новые события сеанса не принимаются. */
public class OpenSessionRule implements EventRule {

    private final EventLog log;

    public OpenSessionRule(EventLog log) {
        this.log = log;
    }

    @Override
    public Optional<String> check(ViewEvent event) {
        if (log.isClosed(event.sessionId())) {
            return Optional.of("сеанс " + event.sessionId() + " уже завершён");
        }
        return Optional.empty();
    }
}
