package ru.mirea.tour.tour_server.view.summary;

import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;
import ru.mirea.tour.tour_server.view.data.EventLog;
import ru.mirea.tour.tour_server.view.model.Appeal;
import ru.mirea.tour.tour_server.view.model.EventType;
import ru.mirea.tour.tour_server.view.model.RoomStats;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Алгоритм 3: готовит сведения об осмотре по обращению — сводку просмотра
 * или отметку о том, что сведений нет.
 */
public class SummaryBuilder {

    private final ApartmentCatalog catalog;
    private final EventLog log;
    private final RoomStatsCalculator calculator;

    public SummaryBuilder(ApartmentCatalog catalog, EventLog log, RoomStatsCalculator calculator) {
        this.catalog = catalog;
        this.log = log;
        this.calculator = calculator;
    }

    public InspectionInfo build(Appeal appeal) {
        if (!appeal.hasSession()) {
            return new NoInspectionData(appeal.id(), "", NoInspectionData.Reason.NO_TOUR);
        }
        String sessionApartment = catalog.apartmentOfSession(appeal.sessionId()).orElse(null);
        if (!appeal.apartmentId().equals(sessionApartment)) {
            return new NoInspectionData(appeal.id(), appeal.sessionId(), NoInspectionData.Reason.OTHER_APARTMENT);
        }

        List<ViewEvent> events = log.eventsOf(appeal.sessionId());
        Map<String, RoomStats> stats = calculator.calculate(events);
        if (stats.isEmpty()) {
            return new NoInspectionData(appeal.id(), appeal.sessionId(), NoInspectionData.Reason.NO_ROOMS_VISITED);
        }

        long total = 0;
        List<ViewSummary.RoomLine> rooms = new ArrayList<>();
        for (Map.Entry<String, RoomStats> s : stats.entrySet()) {
            total += s.getValue().seconds();
            rooms.add(new ViewSummary.RoomLine(s.getKey(), s.getValue().seconds(), s.getValue().visits()));
        }
        rooms.sort(Comparator.comparingLong(ViewSummary.RoomLine::seconds).reversed());

        Map<String, String> finishes = new LinkedHashMap<>();
        for (ViewEvent e : events) {
            if (e.type() == EventType.SELECT_FINISH) {
                finishes.put(e.roomId(), e.finishId()); // более поздний выбор заменяет ранний
            }
        }
        return new ViewSummary(appeal.id(), appeal.sessionId(), total, rooms, finishes);
    }
}
