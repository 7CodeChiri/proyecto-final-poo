package pe.poo.plugins

import pe.poo.api.Strategy
import pe.poo.game.{Arena, LevelLayout, LevelRequest}

final class ChallengeGenerator extends Strategy[LevelRequest, LevelLayout]:
  override def id(): String = "desafío"
  override def supports(request: LevelRequest): Boolean = request.number() > 0

  override def execute(request: LevelRequest): LevelLayout =
    new LevelLayout(new Arena(request.seed(), 0.43),
      math.min(10, 4 + (request.number() - 1) / 3))
