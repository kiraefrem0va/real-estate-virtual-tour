package ru.mirea.tour.tour_server.view.model;

/**
 * Проверенное событие просмотра.
 *
 * @param sessionId сеанс просмотра
 * @param time      секунды от начала сеанса
 * @param type      тип события
 * @param roomId    помещение (пустая строка, если для типа не нужно)
 * @param finishId  вариант отделки (пустая строка, если для типа не нужен)
 */
public record ViewEvent(String sessionId, long time, EventType type, String roomId, String finishId) {
}
