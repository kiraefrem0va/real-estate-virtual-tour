package ru.mirea.tour.tour_server.view;

import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;
import ru.mirea.tour.tour_server.view.data.EventCsvReader;
import ru.mirea.tour.tour_server.view.data.EventLog;
import ru.mirea.tour.tour_server.view.model.Appeal;
import ru.mirea.tour.tour_server.view.registration.EventRegistrar;
import ru.mirea.tour.tour_server.view.registration.RegistrationResult;
import ru.mirea.tour.tour_server.view.summary.RoomStatsCalculator;
import ru.mirea.tour.tour_server.view.summary.SummaryBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Объектно-ориентированная версия демонстрации: тот же пример, что и в процедурной реализации. */
public final class ViewSummaryApp {

    private ViewSummaryApp() {
    }

    /** Каталог примера: две квартиры, их помещения, варианты отделки и сеансы. */
    public static ApartmentCatalog sampleCatalog() {
        return new ApartmentCatalog()
                .addRoom("hall", "K-12", "Прихожая")
                .addRoom("kitchen", "K-12", "Кухня-гостиная")
                .addRoom("bedroom", "K-12", "Спальня")
                .addRoom("bath", "K-12", "Санузел")
                .addRoom("studio", "K-27", "Комната-студия")
                .addFinish("light", "K-12", "Светлая (white box)")
                .addFinish("wood", "K-12", "Скандинавская с деревом")
                .addFinish("dark", "K-12", "Тёмная графитовая")
                .addFinish("basic", "K-27", "Базовая")
                .addSession("S1", "K-12")
                .addSession("S2", "K-12")
                .addSession("S3", "K-27");
    }

    /** Выполняет весь сценарий и возвращает строки отчёта. */
    public static List<String> run(Path input) throws IOException {
        ApartmentCatalog catalog = sampleCatalog();
        EventLog log = new EventLog();
        EventRegistrar registrar = EventRegistrar.withDefaultRules(catalog, log);
        SummaryBuilder builder = new SummaryBuilder(catalog, log, new RoomStatsCalculator());

        RegistrationResult result = registrar.register(new EventCsvReader().read(input));
        List<String> out = new ArrayList<>();
        out.add("Получено событий: " + result.received() + ", принято: " + result.accepted());
        out.addAll(result.errors());

        List<Appeal> appeals = List.of(
                new Appeal("A-001", "K-12", "S1"),
                new Appeal("A-002", "K-27", "S3"),
                new Appeal("A-003", "K-12", ""));
        for (Appeal appeal : appeals) {
            out.add("");
            out.addAll(builder.build(appeal).toLines(catalog));
        }
        return out;
    }

    public static void main(String[] args) throws IOException {
        Path input = args.length > 0 ? Path.of(args[0]) : Path.of("src/main/resources/sample/events.csv");
        for (String line : run(input)) {
            System.out.println(line);
        }
    }
}
