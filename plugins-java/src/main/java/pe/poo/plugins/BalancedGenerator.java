package pe.poo.plugins;

import pe.poo.api.Strategy;
import pe.poo.game.Arena;
import pe.poo.game.LevelLayout;
import pe.poo.game.LevelRequest;

public final class BalancedGenerator implements Strategy<LevelRequest, LevelLayout> {
    public String id() { return "equilibrado"; }
    public boolean supports(LevelRequest request) { return request.number() > 0; }

    public LevelLayout execute(LevelRequest request) {
        return new LevelLayout(new Arena(request.seed(), .33),
            Math.min(8, 3 + (request.number() - 1) / 4));
    }
}
