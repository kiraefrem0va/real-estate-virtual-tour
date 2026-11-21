package ru.mirea.tour.tour_server.view.model;

/**
 * Обращение покупателя в отдел продаж.
 *
 * @param id          номер обращения
 * @param apartmentId квартира, по которой обратился покупатель
 * @param sessionId   сеанс тура, из которого создано обращение; пустая строка, если тура не было
 */
public record Appeal(String id, String apartmentId, String sessionId) {

    /** Связано ли обращение с сеансом виртуального тура. */
    public boolean hasSession() {
        return sessionId != null && !sessionId.isEmpty();
    }
}
