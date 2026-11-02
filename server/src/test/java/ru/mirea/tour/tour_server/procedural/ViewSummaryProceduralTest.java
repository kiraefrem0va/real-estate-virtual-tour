package ru.mirea.tour.tour_server.procedural;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewSummaryProceduralTest {

    private static final Set<String> ROOMS = Set.of("hall", "kitchen", "bedroom", "bath");
    private static final Set<String> FINISHES = Set.of("light", "wood", "dark");

    private static String[][] events(String... lines) {
        return ViewSummaryProcedural.parseEvents(List.of(lines));
    }

    // ---------- Алгоритм 1 ----------

    @Test
    void acceptsCorrectEvents() {
        List<String[]> log = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int accepted = ViewSummaryProcedural.registerEvents(events(
                "S1;0;ENTER_ROOM;hall;",
                "S1;10;SELECT_FINISH;hall;light",
                "S1;20;END;;"), ROOMS, FINISHES, log, errors);
        assertEquals(3, accepted);
        assertEquals(3, log.size());
        assertTrue(errors.isEmpty());
    }

    @Test
    void rejectsEventsAfterEndAndOutOfOrder() {
        List<String[]> log = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int accepted = ViewSummaryProcedural.registerEvents(events(
                "S1;0;ENTER_ROOM;hall;",
                "S1;30;ENTER_ROOM;kitchen;",
                "S1;20;ENTER_ROOM;bath;",
                "S1;40;END;;",
                "S1;50;ENTER_ROOM;hall;"), ROOMS, FINISHES, log, errors);
        assertEquals(3, accepted);
        assertEquals(2, errors.size());
    }

    @Test
    void rejectsUnknownRoomFinishAndType() {
        List<String[]> log = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int accepted = ViewSummaryProcedural.registerEvents(events(
                "S1;0;ENTER_ROOM;balcony;",
                "S1;5;SELECT_FINISH;hall;marble",
                "S1;6;OPEN_DOOR;hall;",
                "S1;abc;ENTER_ROOM;hall;"), ROOMS, FINISHES, log, errors);
        assertEquals(0, accepted);
        assertEquals(4, errors.size());
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
    void buildsSummaryWithTopRoomAndChosenFinish() {
        List<String[]> log = new ArrayList<>(List.of(events(
                "S1;0;ENTER_ROOM;hall;",
                "S1;20;ENTER_ROOM;kitchen;",
                "S1;95;SELECT_FINISH;kitchen;light",
                "S1;150;SELECT_FINISH;kitchen;wood",
                "S1;210;ENTER_ROOM;bedroom;",
                "S1;270;END;;",
                "S2;0;ENTER_ROOM;bath;",
                "S2;100;END;;")));
        List<String> summary = ViewSummaryProcedural.buildSummary("A-1", "S1", log,
                Map.of("kitchen", "Кухня"), Map.of("wood", "Дерево"));
        String text = String.join("\n", summary);
        assertTrue(text.contains("1. Кухня — 3 мин 10 с"));
        assertTrue(text.contains("- Кухня: Дерево"));
        assertTrue(text.contains("Наибольший интерес: Кухня"));
        assertTrue(!text.contains("bath"));
    }

    @Test
    void reportsWhenBuyerDidNotUseTour() {
        List<String> summary = ViewSummaryProcedural.buildSummary("A-2", "S9", new ArrayList<>(),
                Map.of(), Map.of());
        assertTrue(summary.get(1).contains("не проходил"));
    }
}
