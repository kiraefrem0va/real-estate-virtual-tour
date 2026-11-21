package ru.mirea.tour.tour_server.view.registration;

import ru.mirea.tour.tour_server.view.model.EventType;
import ru.mirea.tour.tour_server.view.model.RawEvent;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

/** Превращает текстовое событие в проверенный объект ViewEvent или сообщает, что разобрать его нельзя. */
public class EventParser {

    public ViewEvent parse(RawEvent raw) throws InvalidEventException {
        if (raw.session().isEmpty()) {
            throw new InvalidEventException("не указан сеанс");
        }
        long time;
        try {
            time = Long.parseLong(raw.time());
        } catch (NumberFormatException ex) {
            throw new InvalidEventException("некорректное время «" + raw.time() + "»");
        }
        if (time < 0) {
            throw new InvalidEventException("отрицательное время");
        }
        EventType type;
        try {
            type = EventType.valueOf(raw.type());
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventException("неизвестный тип события «" + raw.type() + "»");
        }
        return new ViewEvent(raw.session(), time, type, raw.room(), raw.finish());
    }
}
