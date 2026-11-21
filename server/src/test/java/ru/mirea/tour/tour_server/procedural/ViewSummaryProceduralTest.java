package ru.mirea.tour.tour_server.procedural;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewSummaryProceduralTest {

    // каталог: квартира K-12 (четыре помещения, три отделки) и квартира K-27 (студия, одна отделка)
    private static final Map<String, String> ROOMS = Map.of(
            "hall", "K-12", "kitchen", "K-12", "bedroom", "K-12", "bath", "K-12", "studio", "K-27");
    private static final Map<String, String> FINISHES = Map.of(
            "light", "K-12", "wood", "K-12", "dark", "K-12", "basic", "K-27");
    private static final Map<String, String> SESSIONS = Map.of("S1", "K-12", "S2", "K-12", "S3", "K-27");

    private static String[][] events(String... lines) {
        return ViewSummaryProcedural.parseEvents(List.of(lines));
    }

    private static int register(List<String[]> log, List<String> errors, String... lines) {
        return ViewSummaryProcedural.registerEvents(events(lines), SESSIONS, ROOMS, FINISHES, log, errors);
    }

    // ---------- Алгоритм 1 ----------

    @Test
    void acceptsCorrectEvents() {
        List<String[]> log = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int accepted = register(log, errors,
                "S1;0;ENTER_ROOM;hall;",
                "S1;10;SELECT_FINISH;hall;light",
                "S1;20;END;;");
        assertEquals(3, accepted);
        assertEquals(3, log.size());
        assertTrue(errors.isEmpty());
    }

    @Test
    void rejectsEventsAfterEndAndOutOfOrder() {
        List<String[]> log = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int accepted = register(log, errors,
                "S1;0;ENTER_ROOM;hall;",
                "S1;30;ENTER_ROOM;kitchen;",
                "S1;20;ENTER_ROOM;bath;",
                "S1;40;END;;",
                "S1;50;ENTER_ROOM;hall;");
        assertEquals(3, accepted);
        assertEquals(2, errors.size());
    }

    @Test
    void rejectsUnknownRoomFinishTypeAndSession() {
        List<String[]> log = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int accepted = register(log, errors,
                "S1;0;ENTER_ROOM;balcony;",
                "S1;5;SELECT_FINISH;hall;marble",
                "S1;6;OPEN_DOOR;hall;",
                "S1;abc;ENTER_ROOM;hall;",
                "S9;0;ENTER_ROOM;hall;");
        assertEquals(0, accepted);
        assertEquals(5, errors.size());
    }

    @Test
    void rejectsRoomAndFinishOfAnotherApartment() {
        List<String[]> log = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int accepted = register(log, errors,
                "S3;0;ENTER_ROOM;kitchen;",          // кухня квартиры K-12, а сеанс по K-27
                "S3;5;SELECT_FINISH;studio;wood",    // отделка квартиры K-12
                "S3;8;ENTER_ROOM;studio;");          // корректно
        assertEquals(1, accepted);
        assertTrue(errors.get(0).contains("относится к другой квартире"));
        assertTrue(errors.get(1).contains("вариант отделки «wood» относится к другой квартире"));
    }

    // ---------- Алгоритм 2 ----------

    @Test
    void countsTimeAndVisitsPerRoom() {
        Map<String, long[]> stats = ViewSummaryProcedural.calculateRoomStats(events(
                "S1;0;ENTER_ROOM;hall;",
                "S1;20;ENTER_ROOM;kitchen;",
                "S1;210;ENTER_ROOM;bedroom;",
                "S1;270;ENTER_ROOM;bath;",
                "S1;300;ENTER_ROOM;kitchen;",
                "S1;420;END;;"));
        assertEquals(20, stats.get("hall")[ViewSummaryProcedural.S_SECONDS]);
        assertEquals(310, stats.get("kitchen")[ViewSummaryProcedural.S_SECONDS]);
        assertEquals(2, stats.get("kitchen")[ViewSummaryProcedural.S_VISITS]);
        assertEquals(60, stats.get("bedroom")[ViewSummaryProcedural.S_SECONDS]);
        assertEquals(30, stats.get("bath")[ViewSummaryProcedural.S_SECONDS]);
    }

    @Test
    void sortsUnorderedEventsAndClosesSessionWithoutEnd() {
        Map<String, long[]> stats = ViewSummaryProcedural.calculateRoomStats(events(
                "S1;50;ENTER_ROOM;bath;",
                "S1;0;ENTER_ROOM;hall;",
                "S1;80;SELECT_FINISH;bath;dark"));
        assertEquals(50, stats.get("hall")[ViewSummaryProcedural.S_SECONDS]);
        assertEquals(30, stats.get("bath")[ViewSummaryProcedural.S_SECONDS]);
    }

    // ---------- Алгоритм 3 ----------

    @Test
    void buildsSummaryWithLongestRoomAndChosenFinish() {
        List<String[]> log = new ArrayList<>(List.of(events(
                "S1;0;ENTER_ROOM;hall;",
                "S1;20;ENTER_ROOM;kitchen;",
                "S1;95;SELECT_FINISH;kitchen;light",
                "S1;150;SELECT_FINISH;kitchen;wood",
                "S1;210;ENTER_ROOM;bedroom;",
                "S1;270;END;;",
                "S2;0;ENTER_ROOM;bath;",
                "S2;100;END;;")));
        List<String> summary = ViewSummaryProcedural.buildSummary("A-1", "K-12", "S1", SESSIONS, log,
                Map.of("kitchen", "Кухня"), Map.of("wood", "Дерево"));
        String text = String.join("\n", summary);
        assertTrue(text.contains("1. Кухня — 3 мин 10 с"));
        assertTrue(text.contains("- Кухня: Дерево"));
        assertTrue(text.contains("Дольше всего находился в помещении: Кухня"));
        assertFalse(text.contains("bath"));
    }

    @Test
    void reportsWhenAppealCameWithoutTour() {
        List<String> summary = ViewSummaryProcedural.buildSummary("A-2", "K-12", "", SESSIONS,
                new ArrayList<>(), Map.of(), Map.of());
        assertTrue(summary.get(1).contains("не проходил тур"));
    }

    @Test
    void reportsNoDataWhenOnlyEndInSession() {
        List<String[]> log = new ArrayList<>(List.of(events("S3;12;END;;")));
        List<String> summary = ViewSummaryProcedural.buildSummary("A-3", "K-27", "S3", SESSIONS, log,
                Map.of(), Map.of());
        assertEquals(2, summary.size());
        assertTrue(summary.get(1).contains("не посещено ни одно помещение"));
    }

    @Test
    void refusesSessionOfAnotherApartment() {
        List<String[]> log = new ArrayList<>(List.of(events("S3;0;ENTER_ROOM;studio;", "S3;30;END;;")));
        List<String> summary = ViewSummaryProcedural.buildSummary("A-4", "K-12", "S3", SESSIONS, log,
                Map.of(), Map.of());
        assertTrue(summary.get(1).contains("сеанс относится к другой квартире"));
    }
}
