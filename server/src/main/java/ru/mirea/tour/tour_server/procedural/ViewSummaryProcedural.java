package ru.mirea.tour.tour_server.procedural;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Процедурная реализация алгоритмов аналитического этапа.
 *
 * Событие просмотра хранится как массив строк из пяти полей:
 * [0] идентификатор сеанса, [1] время в секундах от начала сеанса,
 * [2] тип события, [3] идентификатор помещения, [4] идентификатор варианта отделки.
 *
 * Собственные классы данных намеренно не используются: данные передаются
 * между функциями в виде массивов и стандартных коллекций.
 */
public final class ViewSummaryProcedural {

    public static final String ENTER_ROOM = "ENTER_ROOM";
    public static final String SELECT_FINISH = "SELECT_FINISH";
    public static final String END = "END";

    public static final int F_SESSION = 0;
    public static final int F_TIME = 1;
    public static final int F_TYPE = 2;
    public static final int F_ROOM = 3;
    public static final int F_FINISH = 4;

    /** Индексы в массиве статистики помещения: суммарное время и число заходов. */
    public static final int S_SECONDS = 0;
    public static final int S_VISITS = 1;

    private ViewSummaryProcedural() {
    }

    // ------------------------------------------------------------------
    // Чтение входных данных
    // ------------------------------------------------------------------

    /** Разбирает строки формата «сеанс;время;тип;помещение;отделка». Пустые строки и строки с # пропускаются. */
    public static String[][] parseEvents(List<String> lines) {
        List<String[]> result = new ArrayList<>();
        for (String line : lines) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) {
                continue;
            }
            String[] parts = s.split(";", -1);
            String[] event = new String[5];
            for (int i = 0; i < 5; i++) {
                event[i] = i < parts.length ? parts[i].trim() : "";
            }
            result.add(event);
        }
        return result.toArray(new String[0][]);
    }

    // ------------------------------------------------------------------
    // Алгоритм 1. Регистрация событий просмотра с проверкой корректности
    // ------------------------------------------------------------------

    /**
     * Проверяет каждое поступившее событие и добавляет корректные в журнал.
     *
     * @param incoming   поступившие события
     * @param roomIds    идентификаторы помещений квартиры
     * @param finishIds  идентификаторы вариантов отделки
     * @param log        журнал принятых событий (дополняется)
     * @param errors     сообщения об отклонённых событиях (дополняется)
     * @return количество принятых событий
     */
    public static int registerEvents(String[][] incoming, Set<String> roomIds, Set<String> finishIds,
                                     List<String[]> log, List<String> errors) {
        Map<String, Long> lastTime = new HashMap<>();
        Set<String> closed = new HashSet<>();
        // учитываем события, уже находящиеся в журнале
        for (String[] e : log) {
            lastTime.put(e[F_SESSION], Long.parseLong(e[F_TIME]));
            if (END.equals(e[F_TYPE])) {
                closed.add(e[F_SESSION]);
            }
        }

        int accepted = 0;
        for (int i = 0; i < incoming.length; i++) {
            String[] e = incoming[i];
            String error = checkEvent(e, roomIds, finishIds, lastTime, closed);
            if (error != null) {
                errors.add("Событие " + (i + 1) + " отклонено: " + error);
                continue;
            }
            log.add(e);
            lastTime.put(e[F_SESSION], Long.parseLong(e[F_TIME]));
            if (END.equals(e[F_TYPE])) {
                closed.add(e[F_SESSION]);
            }
            accepted++;
        }
        return accepted;
    }

    /** Возвращает описание ошибки или null, если событие корректно. */
    static String checkEvent(String[] e, Set<String> roomIds, Set<String> finishIds,
                             Map<String, Long> lastTime, Set<String> closed) {
        if (e[F_SESSION].isEmpty()) {
            return "не указан сеанс";
        }
        long time;
        try {
            time = Long.parseLong(e[F_TIME]);
        } catch (NumberFormatException ex) {
            return "некорректное время «" + e[F_TIME] + "»";
        }
        if (time < 0) {
            return "отрицательное время";
        }
        if (closed.contains(e[F_SESSION])) {
            return "сеанс " + e[F_SESSION] + " уже завершён";
        }
        Long prev = lastTime.get(e[F_SESSION]);
        if (prev != null && time < prev) {
            return "время меньше времени предыдущего события сеанса";
        }
        String type = e[F_TYPE];
        if (ENTER_ROOM.equals(type)) {
            if (!roomIds.contains(e[F_ROOM])) {
                return "неизвестное помещение «" + e[F_ROOM] + "»";
            }
        } else if (SELECT_FINISH.equals(type)) {
            if (!roomIds.contains(e[F_ROOM])) {
                return "неизвестное помещение «" + e[F_ROOM] + "»";
            }
            if (!finishIds.contains(e[F_FINISH])) {
                return "неизвестный вариант отделки «" + e[F_FINISH] + "»";
            }
        } else if (!END.equals(type)) {
            return "неизвестный тип события «" + type + "»";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Алгоритм 2. Расчёт времени пребывания в помещениях
    // ------------------------------------------------------------------

    /**
     * Для событий одного сеанса считает суммарное время и число заходов в каждое помещение.
     * Время пребывания в помещении длится от входа в него до следующего входа в другое
     * помещение или до завершения сеанса. Если события END нет, сеанс считается
     * завершённым в момент последнего события.
     *
     * @return помещение -> {секунды, число заходов} в порядке первого посещения
     */
    public static Map<String, long[]> calculateRoomStats(String[][] sessionEvents) {
        String[][] events = sessionEvents.clone();
        // сортировка по времени (устойчивая, события с равным временем сохраняют порядок)
        Arrays.sort(events, (a, b) -> Long.compare(Long.parseLong(a[F_TIME]), Long.parseLong(b[F_TIME])));

        Map<String, long[]> stats = new LinkedHashMap<>();
        String currentRoom = null;
        long enteredAt = 0;
        long lastTime = 0;

        for (String[] e : events) {
            long t = Long.parseLong(e[F_TIME]);
            lastTime = t;
            String type = e[F_TYPE];
            if (ENTER_ROOM.equals(type)) {
                if (currentRoom != null) {
                    stats.get(currentRoom)[S_SECONDS] += t - enteredAt;
                }
                if (!e[F_ROOM].equals(currentRoom)) {
                    long[] s = stats.computeIfAbsent(e[F_ROOM], k -> new long[2]);
                    s[S_VISITS]++;
                }
                currentRoom = e[F_ROOM];
                enteredAt = t;
            } else if (END.equals(type)) {
                if (currentRoom != null) {
                    stats.get(currentRoom)[S_SECONDS] += t - enteredAt;
                }
                currentRoom = null;
                break;
            }
            // SELECT_FINISH на время пребывания не влияет
        }
        if (currentRoom != null) {
            stats.get(currentRoom)[S_SECONDS] += lastTime - enteredAt;
        }
        return stats;
    }

    // ------------------------------------------------------------------
    // Алгоритм 3. Формирование сводки просмотра по обращению
    // ------------------------------------------------------------------

    /**
     * Формирует для менеджера текстовую сводку просмотра, связанного с обращением.
     *
     * @param appealId    номер обращения
     * @param sessionId   сеанс просмотра, из которого создано обращение
     * @param log         журнал всех событий
     * @param roomNames   идентификатор помещения -> название
     * @param finishNames идентификатор отделки -> название
     */
    public static List<String> buildSummary(String appealId, String sessionId, List<String[]> log,
                                            Map<String, String> roomNames, Map<String, String> finishNames) {
        // 1) выбор событий сеанса
        List<String[]> own = new ArrayList<>();
        for (String[] e : log) {
            if (e[F_SESSION].equals(sessionId)) {
                own.add(e);
            }
        }
        List<String> out = new ArrayList<>();
        out.add("Сводка просмотра по обращению " + appealId + " (сеанс " + sessionId + ")");
        if (own.isEmpty()) {
            out.add("Покупатель не проходил виртуальный тур перед обращением.");
            return out;
        }
        String[][] events = own.toArray(new String[0][]);

        // 2) время и заходы по помещениям
        Map<String, long[]> stats = calculateRoomStats(events);
        long total = 0;
        for (long[] s : stats.values()) {
            total += s[S_SECONDS];
        }

        // 3) последний выбранный вариант отделки в каждом помещении
        Map<String, String> chosen = new LinkedHashMap<>();
        for (String[] e : events) {
            if (SELECT_FINISH.equals(e[F_TYPE])) {
                chosen.put(e[F_ROOM], e[F_FINISH]);
            }
        }

        // 4) ранжирование помещений по времени
        String[] rooms = stats.keySet().toArray(new String[0]);
        Arrays.sort(rooms, (a, b) -> Long.compare(stats.get(b)[S_SECONDS], stats.get(a)[S_SECONDS]));

        out.add("Общая длительность просмотра: " + formatTime(total));
        out.add("Помещения по времени осмотра:");
        for (int i = 0; i < rooms.length; i++) {
            long[] s = stats.get(rooms[i]);
            long percent = total == 0 ? 0 : Math.round(100.0 * s[S_SECONDS] / total);
            String line = (i + 1) + ". " + name(roomNames, rooms[i]) + " — " + formatTime(s[S_SECONDS])
                    + " (" + percent + " %), заходов: " + s[S_VISITS];
            out.add(line);
        }

        List<String> returned = new ArrayList<>();
        for (String r : rooms) {
            if (stats.get(r)[S_VISITS] > 1) {
                returned.add(name(roomNames, r));
            }
        }
        out.add("Возвращался в помещения: " + (returned.isEmpty() ? "нет" : String.join(", ", returned)));

        if (chosen.isEmpty()) {
            out.add("Варианты отделки не выбирались.");
        } else {
            out.add("Выбранная отделка:");
            for (Map.Entry<String, String> c : chosen.entrySet()) {
                out.add("- " + name(roomNames, c.getKey()) + ": " + name(finishNames, c.getValue()));
            }
        }
        out.add("Наибольший интерес: " + name(roomNames, rooms[0]));
        return out;
    }

    static String name(Map<String, String> names, String id) {
        String n = names.get(id);
        return n == null ? id : n;
    }

    static String formatTime(long seconds) {
        return (seconds / 60) + " мин " + (seconds % 60) + " с";
    }

    // ------------------------------------------------------------------
    // Демонстрация на примере
    // ------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Map<String, String> roomNames = new LinkedHashMap<>();
        roomNames.put("hall", "Прихожая");
        roomNames.put("kitchen", "Кухня-гостиная");
        roomNames.put("bedroom", "Спальня");
        roomNames.put("bath", "Санузел");
        Map<String, String> finishNames = new LinkedHashMap<>();
        finishNames.put("light", "Светлая (white box)");
        finishNames.put("wood", "Скандинавская с деревом");
        finishNames.put("dark", "Тёмная графитовая");

        Path input = args.length > 0 ? Path.of(args[0])
                : Path.of("src/main/resources/sample/events.csv");
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        String[][] incoming = parseEvents(lines);

        List<String[]> log = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int accepted = registerEvents(incoming, roomNames.keySet(), finishNames.keySet(), log, errors);

        System.out.println("Получено событий: " + incoming.length + ", принято: " + accepted);
        for (String err : errors) {
            System.out.println(err);
        }
        System.out.println();
        for (String line : buildSummary("A-001", "S1", log, roomNames, finishNames)) {
            System.out.println(line);
        }
    }
}
