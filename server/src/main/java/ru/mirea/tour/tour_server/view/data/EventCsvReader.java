package ru.mirea.tour.tour_server.view.data;

import ru.mirea.tour.tour_server.view.model.RawEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Читает события из текстового файла. Пустые строки и строки с # пропускаются. */
public class EventCsvReader {

    public List<RawEvent> read(Path file) throws IOException {
        return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    public List<RawEvent> parse(List<String> lines) {
        List<RawEvent> result = new ArrayList<>();
        for (String line : lines) {
            String s = line.trim();
            if (!s.isEmpty() && !s.startsWith("#")) {
                result.add(RawEvent.fromLine(s));
            }
        }
        return result;
    }
}
