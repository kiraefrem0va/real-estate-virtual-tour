package ru.mirea.tour.tour_server.view.summary;

import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сводка просмотра по обращению.
 *
 * @param appealId     номер обращения
 * @param sessionId    сеанс тура
 * @param totalSeconds общая длительность просмотра
 * @param rooms        помещения по убыванию времени
 * @param finishes     помещение -> последний выбранный вариант отделки
 */
public record ViewSummary(String appealId, String sessionId, long totalSeconds,
                          List<RoomLine> rooms, Map<String, String> finishes) implements InspectionInfo {

    /** Строка сводки по одному помещению. */
    public record RoomLine(String roomId, long seconds, int visits) {
    }

    /** Помещение, в котором покупатель находился дольше всего. */
    public String longestRoom() {
        return rooms.get(0).roomId();
    }

    /** Помещения, в которые покупатель возвращался. */
    public List<String> returnedRooms() {
        List<String> result = new ArrayList<>();
        for (RoomLine r : rooms) {
            if (r.visits() > 1) {
                result.add(r.roomId());
            }
        }
        return result;
    }

    @Override
    public List<String> toLines(ApartmentCatalog catalog) {
        List<String> out = new ArrayList<>();
        out.add("Сводка просмотра по обращению " + appealId + " (сеанс " + sessionId + ")");
        out.add("Общая длительность просмотра: " + InspectionInfo.formatTime(totalSeconds));
        out.add("Помещения по времени осмотра:");
        for (int i = 0; i < rooms.size(); i++) {
            RoomLine r = rooms.get(i);
            long percent = totalSeconds == 0 ? 0 : Math.round(100.0 * r.seconds() / totalSeconds);
            out.add((i + 1) + ". " + catalog.roomName(r.roomId()) + " — " + InspectionInfo.formatTime(r.seconds())
                    + " (" + percent + " %), заходов: " + r.visits());
        }
        List<String> returned = new ArrayList<>();
        for (String id : returnedRooms()) {
            returned.add(catalog.roomName(id));
        }
        out.add("Возвращался в помещения: " + (returned.isEmpty() ? "нет" : String.join(", ", returned)));
        if (finishes.isEmpty()) {
            out.add("Варианты отделки не выбирались.");
        } else {
            out.add("Выбранная отделка:");
            for (Map.Entry<String, String> f : finishes.entrySet()) {
                out.add("- " + catalog.roomName(f.getKey()) + ": " + catalog.finishName(f.getValue()));
            }
        }
        out.add("Дольше всего находился в помещении: " + catalog.roomName(longestRoom())
                + " (время — наблюдение, интерес уточняется в разговоре)");
        return out;
    }
}
