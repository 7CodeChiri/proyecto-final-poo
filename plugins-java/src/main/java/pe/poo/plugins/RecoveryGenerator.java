package pe.poo.plugins;

import pe.poo.api.Strategy;
import pe.poo.game.Arena;
import pe.poo.game.LevelLayout;
import pe.poo.game.LevelRequest;

public final class RecoveryGenerator implements Strategy<LevelRequest, LevelLayout> {
    public String id() { return "recuperación"; }
    public boolean supports(LevelRequest request) { return request.number() > 0; }

    public LevelLayout execute(LevelRequest request) {
        return new LevelLayout(new Arena(request.seed(), .22),
            Math.min(8, 2 + (request.number() - 1) / 4));
    }
}
