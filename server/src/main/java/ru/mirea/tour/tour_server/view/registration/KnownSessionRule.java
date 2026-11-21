package ru.mirea.tour.tour_server.view.registration;

import ru.mirea.tour.tour_server.view.data.ApartmentCatalog;
import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.Optional;

/** Сеанс события должен быть известен каталогу. */
public class KnownSessionRule implements EventRule {

    private final ApartmentCatalog catalog;

    public KnownSessionRule(ApartmentCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Optional<String> check(ViewEvent event) {
        if (catalog.apartmentOfSession(event.sessionId()).isEmpty()) {
            return Optional.of("неизвестный сеанс «" + event.sessionId() + "»");
        }
        return Optional.empty();
    }
}
