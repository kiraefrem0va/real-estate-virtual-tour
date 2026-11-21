package ru.mirea.tour.tour_server.view.summary;

import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;

import java.util.List;

/**
 * Сведения об осмотре, которые менеджер получает вместе с обращением.
 * Бывают двух видов: сводка просмотра или отметка о том, что сведений нет.
 * Каждый вид сам знает, как превратиться в текст для менеджера.
 */
public sealed interface InspectionInfo permits ViewSummary, NoInspectionData {

    String appealId();

    /** Текст для менеджера; названия помещений и отделки берутся из каталога. */
    List<String> toLines(ApartmentCatalog catalog);

    /** Время в формате «5 мин 10 с». */
    static String formatTime(long seconds) {
        return (seconds / 60) + " мин " + (seconds % 60) + " с";
    }
}
