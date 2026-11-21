package ru.mirea.tour.tour_server.view.registration;

/** Событие нельзя разобрать: не указан сеанс, неверное время или неизвестный тип. */
public class InvalidEventException extends Exception {

    public InvalidEventException(String message) {
        super(message);
    }
}
