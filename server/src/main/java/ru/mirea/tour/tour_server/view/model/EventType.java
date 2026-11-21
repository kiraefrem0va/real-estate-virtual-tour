package ru.mirea.tour.tour_server.view.model;

/** Тип события просмотра, которое присылает виртуальный тур. */
public enum EventType {
    /** Покупатель перешёл в помещение. */
    ENTER_ROOM,
    /** Покупатель выбрал вариант отделки в помещении. */
    SELECT_FINISH,
    /** Покупатель завершил просмотр. */
    END;

    /** Нужно ли для события указывать помещение. */
    public boolean needsRoom() {
        return this != END;
    }

    /** Нужно ли для события указывать вариант отделки. */
    public boolean needsFinish() {
        return this == SELECT_FINISH;
    }
}
