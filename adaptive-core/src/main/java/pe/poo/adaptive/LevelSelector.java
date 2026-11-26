package pe.poo.adaptive;

import pe.poo.api.Strategy;
import pe.poo.game.LevelLayout;
import pe.poo.game.LevelRequest;

public final class LevelSelector {
    public record Selection(String id, String reason, LevelLayout layout) {}

    public Selection generate(LevelRequest request) {
        String className = "BalancedGenerator";
        String reason = "Dificultad base";
        if (request.damage() >= 2) {
            className = "RecoveryGenerator";
            reason = "Perdiste al menos 2 puntos de vida";
        } else if (request.number() > 1 && request.seconds() < 45) {
            className = "ChallengeGenerator";
            reason = "Completaste rápido el nivel anterior";
        }

        try {
            Class<?> candidate = Class.forName("pe.poo.plugins." + className);
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
