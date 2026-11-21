package ru.mirea.tour.tour_server.view.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Каталог объектов: какой квартире принадлежат помещения, варианты отделки и сеансы просмотра,
 * а также названия помещений и отделки для сводки.
 */
public class ApartmentCatalog {

    private final Map<String, String> roomApartment = new HashMap<>();
    private final Map<String, String> roomNames = new HashMap<>();
    private final Map<String, String> finishApartment = new HashMap<>();
    private final Map<String, String> finishNames = new HashMap<>();
    private final Map<String, String> sessionApartment = new HashMap<>();

    public ApartmentCatalog addRoom(String roomId, String apartmentId, String name) {
        roomApartment.put(roomId, apartmentId);
        roomNames.put(roomId, name);
        return this;
    }

    public ApartmentCatalog addFinish(String finishId, String apartmentId, String name) {
        finishApartment.put(finishId, apartmentId);
        finishNames.put(finishId, name);
        return this;
    }

    public ApartmentCatalog addSession(String sessionId, String apartmentId) {
        sessionApartment.put(sessionId, apartmentId);
        return this;
    }

    public Optional<String> apartmentOfSession(String sessionId) {
        return Optional.ofNullable(sessionApartment.get(sessionId));
    }

    public Optional<String> apartmentOfRoom(String roomId) {
        return Optional.ofNullable(roomApartment.get(roomId));
    }

    public Optional<String> apartmentOfFinish(String finishId) {
        return Optional.ofNullable(finishApartment.get(finishId));
    }

    /** Название помещения; если его нет в каталоге, возвращается идентификатор. */
    public String roomName(String roomId) {
        return roomNames.getOrDefault(roomId, roomId);
    }

    /** Название варианта отделки; если его нет в каталоге, возвращается идентификатор. */
    public String finishName(String finishId) {
        return finishNames.getOrDefault(finishId, finishId);
    }
}
