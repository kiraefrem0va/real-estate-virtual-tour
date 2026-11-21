package ru.mirea.tour.tour_server.view.registration;

import java.util.List;

/**
 * Итог регистрации пачки событий.
 *
 * @param received сколько событий поступило
 * @param accepted сколько принято в журнал
 * @param errors   сообщения об отклонённых событиях
 */
public record RegistrationResult(int received, int accepted, List<String> errors) {
}
