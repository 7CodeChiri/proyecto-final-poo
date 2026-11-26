package pe.poo.adaptive;

import pe.poo.api.Strategy;
import pe.poo.game.LevelLayout;
import pe.poo.game.LevelRequest;

/** Selects and validates a generator from the measured results of the last level. */
public final class LevelSelector {
    private final DifficultyConfig config;

    public LevelSelector() { this(new DifficultyConfig(2, 45)); }

    public LevelSelector(DifficultyConfig config) {
        this.config = java.util.Objects.requireNonNull(config);
    }
    public record Selection(String id, String reason, LevelLayout layout) {}

    public Selection generate(LevelRequest request) {
        String className = config.balancedClass();
        String reason = "Dificultad base";
        if (request.damage() >= config.recoveryDamage()) {
            className = config.recoveryClass();
            reason = "Perdiste al menos " + config.recoveryDamage() + " puntos de vida";
        } else if (request.number() > 1 && request.seconds() < config.challengeSeconds()) {
            className = config.challengeClass();
            reason = "Completaste rápido el nivel anterior";
        }

        try {
            Class<?> candidate = Class.forName(className, false, LevelSelector.class.getClassLoader());
            Strategy<?, ?> strategy = (Strategy<?, ?>) candidate
                .asSubclass(Strategy.class).getConstructor().newInstance();
            return execute(strategy, request, reason);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            throw new IllegalStateException("No se pudo cargar " + className, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Selection execute(Strategy<?, ?> candidate, LevelRequest request, String reason) {
        Strategy<LevelRequest, ?> strategy = (Strategy<LevelRequest, ?>) candidate;
        if (!strategy.supports(request)) {
            throw new IllegalArgumentException("El generador no admite este nivel");
        }
        return new Selection(strategy.id(), reason, LevelLayout.class.cast(strategy.execute(request)));
    }
}
