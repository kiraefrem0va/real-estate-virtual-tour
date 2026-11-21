package ru.mirea.tour.tour_server.view.summary;

import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;

import java.util.List;

/**
 * Отметка о том, что сведений об осмотре нет.
 *
 * @param appealId  номер обращения
 * @param sessionId сеанс из обращения (пустая строка, если тура не было)
 * @param reason    почему сведений нет
 */
public record NoInspectionData(String appealId, String sessionId, Reason reason) implements InspectionInfo {

    public enum Reason {
        NO_TOUR("покупатель не проходил тур."),
        OTHER_APARTMENT("сеанс относится к другой квартире, сводка не сформирована."),
        NO_ROOMS_VISITED("в сеансе не посещено ни одно помещение.");

        private final String text;

        Reason(String text) {
            this.text = text;
        }
    }

    @Override
    public List<String> toLines(ApartmentCatalog catalog) {
        String header = reason == Reason.NO_TOUR
                ? "Обращение " + appealId + " поступило без виртуального тура"
                : "Сводка просмотра по обращению " + appealId + " (сеанс " + sessionId + ")";
        return List.of(header, "Сведений об осмотре нет: " + reason.text);
    }
}
