package ru.mirea.tour.tour_server.view;

import org.junit.jupiter.api.Test;
import ru.mirea.tour.tour_server.procedural.ViewSummaryProcedural;
import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;
import ru.mirea.tour.tour_server.view.data.EventCsvReader;
import ru.mirea.tour.tour_server.view.data.EventLog;
import ru.mirea.tour.tour_server.view.model.Appeal;
import ru.mirea.tour.tour_server.view.model.EventType;
import ru.mirea.tour.tour_server.view.model.RoomStats;
import ru.mirea.tour.tour_server.view.model.ViewEvent;
import ru.mirea.tour.tour_server.view.registration.EventParser;
import ru.mirea.tour.tour_server.view.registration.EventRegistrar;
import ru.mirea.tour.tour_server.view.registration.EventRule;
import ru.mirea.tour.tour_server.view.registration.RegistrationResult;
import ru.mirea.tour.tour_server.view.summary.InspectionInfo;
import ru.mirea.tour.tour_server.view.summary.NoInspectionData;
import ru.mirea.tour.tour_server.view.summary.RoomStatsCalculator;
import ru.mirea.tour.tour_server.view.summary.SummaryBuilder;
import ru.mirea.tour.tour_server.view.summary.ViewSummary;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewSummaryOopTest {

    private final ApartmentCatalog catalog = ViewSummaryApp.sampleCatalog();
    private final EventLog log = new EventLog();
    private final EventRegistrar registrar = EventRegistrar.withDefaultRules(catalog, log);
    private final SummaryBuilder builder = new SummaryBuilder(catalog, log, new RoomStatsCalculator());

    private RegistrationResult register(String... lines) {
        return registrar.register(new EventCsvReader().parse(List.of(lines)));
    }

    // ---------- Регистрация событий (алгоритм 1) ----------

    @Test
    void acceptsCorrectEvents() {
        RegistrationResult r = register("S1;0;ENTER_ROOM;hall;", "S1;10;SELECT_FINISH;hall;light", "S1;20;END;;");
        assertEquals(3, r.accepted());
        assertEquals(3, log.size());
        assertTrue(r.errors().isEmpty());
    }

    @Test
    void rejectsEventsAfterEndAndOutOfOrder() {
        RegistrationResult r = register("S1;0;ENTER_ROOM;hall;", "S1;30;ENTER_ROOM;kitchen;",
                "S1;20;ENTER_ROOM;bath;", "S1;40;END;;", "S1;50;ENTER_ROOM;hall;");
        assertEquals(3, r.accepted());
        assertEquals(2, r.errors().size());
    }

    @Test
    void rejectsUnknownRoomFinishTypeAndSession() {
        RegistrationResult r = register("S1;0;ENTER_ROOM;balcony;", "S1;5;SELECT_FINISH;hall;marble",
                "S1;6;OPEN_DOOR;hall;", "S1;abc;ENTER_ROOM;hall;", "S9;0;ENTER_ROOM;hall;");
        assertEquals(0, r.accepted());
        assertEquals(5, r.errors().size());
    }

    @Test
    void rejectsRoomAndFinishOfAnotherApartment() {
        RegistrationResult r = register("S3;0;ENTER_ROOM;kitchen;", "S3;5;SELECT_FINISH;studio;wood",
                "S3;8;ENTER_ROOM;studio;");
        assertEquals(1, r.accepted());
        assertTrue(r.errors().get(0).contains("относится к другой квартире"));
        assertTrue(r.errors().get(1).contains("вариант отделки «wood» относится к другой квартире"));
    }

    @Test
    void newRuleIsAddedWithoutChangingRegistrar() {
        // дополнительное правило: просмотр не может длиться дольше часа
        EventRule maxDuration = e -> e.time() > 3600
                ? Optional.of("слишком долгий просмотр") : Optional.empty();
        EventRegistrar strict = new EventRegistrar(new EventParser(), List.of(maxDuration), new EventLog());
        RegistrationResult r = strict.register(new EventCsvReader().parse(List.of(
                "S1;10;ENTER_ROOM;hall;", "S1;4000;END;;")));
        assertEquals(1, r.accepted());
        assertTrue(r.errors().get(0).contains("слишком долгий просмотр"));
    }

    // ---------- Время по помещениям (алгоритм 2) ----------

    @Test
    void countsTimeAndVisitsPerRoom() {
        register("S1;0;ENTER_ROOM;hall;", "S1;20;ENTER_ROOM;kitchen;", "S1;210;ENTER_ROOM;bedroom;",
                "S1;270;ENTER_ROOM;bath;", "S1;300;ENTER_ROOM;kitchen;", "S1;420;END;;");
        Map<String, RoomStats> s = new RoomStatsCalculator().calculate(log.eventsOf("S1"));
        assertEquals(20, s.get("hall").seconds());
        assertEquals(310, s.get("kitchen").seconds());
        assertEquals(2, s.get("kitchen").visits());
        assertEquals(60, s.get("bedroom").seconds());
        assertEquals(30, s.get("bath").seconds());
    }

    @Test
    void sortsUnorderedEventsAndClosesSessionWithoutEnd() {
        List<ViewEvent> events = new ArrayList<>();
        events.add(new ViewEvent("S1", 50, EventType.ENTER_ROOM, "bath", ""));
        events.add(new ViewEvent("S1", 0, EventType.ENTER_ROOM, "hall", ""));
        events.add(new ViewEvent("S1", 80, EventType.SELECT_FINISH, "bath", "dark"));
        Map<String, RoomStats> s = new RoomStatsCalculator().calculate(events);
        assertEquals(50, s.get("hall").seconds());
        assertEquals(30, s.get("bath").seconds());
    }

    // ---------- Сведения об осмотре (алгоритм 3) ----------

    @Test
    void buildsSummaryWithLongestRoomAndChosenFinish() {
        register("S1;0;ENTER_ROOM;hall;", "S1;20;ENTER_ROOM;kitchen;", "S1;95;SELECT_FINISH;kitchen;light",
                "S1;150;SELECT_FINISH;kitchen;wood", "S1;210;ENTER_ROOM;bedroom;", "S1;270;END;;",
                "S2;0;ENTER_ROOM;bath;", "S2;100;END;;");
        InspectionInfo info = builder.build(new Appeal("A-1", "K-12", "S1"));
        assertTrue(info instanceof ViewSummary);
        ViewSummary summary = (ViewSummary) info;
        assertEquals("kitchen", summary.longestRoom());
        assertEquals("wood", summary.finishes().get("kitchen"));
        assertEquals(270, summary.totalSeconds());
    }

    @Test
    void reportsWhenAppealCameWithoutTour() {
        InspectionInfo info = builder.build(new Appeal("A-2", "K-12", ""));
        assertEquals(NoInspectionData.Reason.NO_TOUR, ((NoInspectionData) info).reason());
    }

    @Test
    void reportsNoDataWhenOnlyEndInSession() {
        register("S3;12;END;;");
        InspectionInfo info = builder.build(new Appeal("A-3", "K-27", "S3"));
        assertEquals(NoInspectionData.Reason.NO_ROOMS_VISITED, ((NoInspectionData) info).reason());
    }

    @Test
    void refusesSessionOfAnotherApartment() {
        register("S3;0;ENTER_ROOM;studio;", "S3;30;END;;");
        InspectionInfo info = builder.build(new Appeal("A-4", "K-12", "S3"));
        assertEquals(NoInspectionData.Reason.OTHER_APARTMENT, ((NoInspectionData) info).reason());
    }

    // ---------- Сравнение с процедурной версией ----------

    @Test
    void givesSameOutputAsProceduralVersion() throws Exception {
        Path sample = Path.of("src/main/resources/sample/events.csv");
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            ViewSummaryProcedural.main(new String[]{sample.toString()});
        } finally {
            System.setOut(original);
        }
        List<String> procedural = buffer.toString(StandardCharsets.UTF_8).lines().toList();
        assertEquals(procedural, ViewSummaryApp.run(sample));
    }
}
