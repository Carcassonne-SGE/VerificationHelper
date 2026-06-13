/*
 * Purpose: Baseline Random Agent that selects legal Carcassonne actions uniformly at random.
 */
package arena;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import model.collections.ActionSet;
import sge.CarcassonneAction;
import sge.CarcassonneGame;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RandomAgent implements GameAgent<CarcassonneGame, CarcassonneAction> {
    private final Random random = new Random();

    @Override
    public CarcassonneAction computeNextAction(CarcassonneGame game, long computationTime, TimeUnit timeUnit) {
        ActionSet actions = game.getBoard().calculatePossibleActionsUnique();
        if (actions.isEmpty()) {
            return null;
        }
        int index = random.nextInt(actions.size());
        return actions.getActionObject(index);
    }
}
