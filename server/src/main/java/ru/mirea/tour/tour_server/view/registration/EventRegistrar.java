package ru.mirea.tour.tour_server.view.registration;

import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;
import ru.mirea.tour.tour_server.view.data.EventLog;
import ru.mirea.tour.tour_server.view.model.RawEvent;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Алгоритм 1: принимает события из тура, проверяет их набором правил и записывает корректные в журнал.
 */
public class EventRegistrar {

    private final EventParser parser;
    private final List<EventRule> rules;
    private final EventLog log;

    public EventRegistrar(EventParser parser, List<EventRule> rules, EventLog log) {
        this.parser = parser;
        this.rules = List.copyOf(rules);
        this.log = log;
    }

    /** Регистратор со стандартным набором правил. */
    public static EventRegistrar withDefaultRules(ApartmentCatalog catalog, EventLog log) {
        return new EventRegistrar(new EventParser(), List.of(
                new KnownSessionRule(catalog),
                new OpenSessionRule(log),
                new TimeOrderRule(log),
                new SameApartmentRule(catalog)), log);
    }

    public RegistrationResult register(List<RawEvent> incoming) {
        List<String> errors = new ArrayList<>();
        int accepted = 0;
        for (int i = 0; i < incoming.size(); i++) {
            Optional<String> error = tryAccept(incoming.get(i));
            if (error.isPresent()) {
                errors.add("Событие " + (i + 1) + " отклонено: " + error.get());
            } else {
                accepted++;
            }
        }
        return new RegistrationResult(incoming.size(), accepted, errors);
    }

    private Optional<String> tryAccept(RawEvent raw) {
        ViewEvent event;
        try {
            event = parser.parse(raw);
        } catch (InvalidEventException ex) {
            return Optional.of(ex.getMessage());
        }
        for (EventRule rule : rules) {
            Optional<String> error = rule.check(event);
            if (error.isPresent()) {
                return error;
            }
        }
        log.add(event);
        return Optional.empty();
    }
}
