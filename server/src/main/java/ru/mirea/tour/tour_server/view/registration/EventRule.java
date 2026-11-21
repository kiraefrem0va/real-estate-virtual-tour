package ru.mirea.tour.tour_server.view.registration;

import ru.mirea.tour.tour_server.view.model.ViewEvent;

import java.util.Optional;

/**
 * Одно правило проверки события перед записью в журнал.
 * Чтобы добавить новую проверку, достаточно написать ещё один класс с этим интерфейсом
 * и передать его объекту EventRegistrar.
 */
public interface EventRule {

    /** Возвращает описание ошибки или пустой Optional, если событие правилу соответствует. */
    Optional<String> check(ViewEvent event);
}
