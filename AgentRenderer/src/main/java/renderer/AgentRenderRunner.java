package renderer;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import mcts.AgentAdaptiveV5;
import mcts.AgentNaiveHeuristicEnsemble;
import model.area.AreaRegistry;
import model.bits.PositionTileMapLayoutBit;
import model.bits.TileLayoutBit;
import model.collections.PositionTileMap;
import model.points.MeepleRegistry;
import model.state.State;
import model.tile.TileSpec;
import sge.CarcassonneAction;
import sge.CarcassonneGame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class AgentRenderRunner {

    private final GameAgent<CarcassonneGame, CarcassonneAction> agent1;
    private final GameAgent<CarcassonneGame, CarcassonneAction> agent2;
    private final AgentRenderer renderer;

    private CarcassonneGame game;
    private volatile boolean running;

    public AgentRenderRunner(GameAgent<CarcassonneGame, CarcassonneAction> agent1,
            GameAgent<CarcassonneGame, CarcassonneAction> agent2,
            AgentRenderer renderer) {
        this.agent1 = agent1;
        this.agent2 = agent2;
        this.renderer = renderer;
    }

    public void start() {
        game = new CarcassonneGame();
        renderer.initRender();
        renderer.setBoard(game.getBoard());
        running = true;
        Thread thread = new Thread(this::runLoop, "renderer");
        thread.start();
    }

    private void runLoop() {
        try {
            while (running && !game.isGameOver()) {
                int player = game.getCurrentPlayer();
                if (player < 0) {
                    game = (CarcassonneGame) game.doAction(game.determineNextAction());
                } else {
                    var agent = player == 0 ? agent1 : agent2;
                    CarcassonneAction action = agent.computeNextAction(game, 2, TimeUnit.SECONDS);
                    game = (CarcassonneGame) game.doAction(action);
                }
                renderer.setBoard(game.getBoard());
            }
        } catch (Exception exception) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        new AgentRenderRunner(new AgentAdaptiveV5(), new AgentNaiveHeuristicEnsemble(), new AgentRenderer()).start();
    }
}
