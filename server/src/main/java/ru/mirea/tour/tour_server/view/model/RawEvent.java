package ru.mirea.tour.tour_server.view.model;

/**
 * Событие в том виде, в каком оно пришло из тура: пять текстовых полей без проверки.
 * Строка файла «сеанс;время;тип;помещение;отделка» превращается в такой объект.
 */
public record RawEvent(String session, String time, String type, String room, String finish) {

    /** Разбирает строку формата «сеанс;время;тип;помещение;отделка». Недостающие поля считаются пустыми. */
    public static RawEvent fromLine(String line) {
        String[] parts = line.trim().split(";", -1);
        String[] f = new String[5];
        for (int i = 0; i < 5; i++) {
            f[i] = i < parts.length ? parts[i].trim() : "";
        }
        return new RawEvent(f[0], f[1], f[2], f[3], f[4]);
    }
}
