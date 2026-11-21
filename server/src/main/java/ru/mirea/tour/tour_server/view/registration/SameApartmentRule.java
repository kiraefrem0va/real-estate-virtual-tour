package ru.mirea.tour.tour_server.view.registration;

import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.Optional;

/**
 * Помещение и вариант отделки из события должны быть в каталоге и относиться
 * к той же квартире, что и сеанс события (ограничение целостности из логической модели данных).
 */
public class SameApartmentRule implements EventRule {

    private final ApartmentCatalog catalog;

    public SameApartmentRule(ApartmentCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Optional<String> check(ViewEvent event) {
        String apartment = catalog.apartmentOfSession(event.sessionId()).orElse("");
        if (event.type().needsRoom()) {
            Optional<String> owner = catalog.apartmentOfRoom(event.roomId());
            if (owner.isEmpty()) {
                return Optional.of("неизвестное помещение «" + event.roomId() + "»");
            }
            if (!owner.get().equals(apartment)) {
                return Optional.of("помещение «" + event.roomId() + "» относится к другой квартире");
            }
        }
        if (event.type().needsFinish()) {
            Optional<String> owner = catalog.apartmentOfFinish(event.finishId());
            if (owner.isEmpty()) {
                return Optional.of("неизвестный вариант отделки «" + event.finishId() + "»");
            }
            if (!owner.get().equals(apartment)) {
                return Optional.of("вариант отделки «" + event.finishId() + "» относится к другой квартире");
            }
        }
        return Optional.empty();
    }
}
