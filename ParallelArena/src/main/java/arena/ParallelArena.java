package arena;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.game.Match;
import at.ac.tuwien.ifs.sge.engine.game.MatchResult;
import mcts.AgentAdaptiveV5;
import mcts.AgentGreedyHeuristic;
import mcts.AgentNaive;
import mcts.AgentNaiveEnsemble;
import mcts.AgentNaiveEnsembleNormalized;
import mcts.AgentNaiveHeuristic;
import mcts.AgentNaiveHeuristicEnsemble;
import sge.CarcassonneAction;
import sge.CarcassonneGame;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/// ParallelArena
///
/// runs all configured agent matchups in parallel and prints aggregated results
public class ParallelArena {

    private final List<AgentSpec> agents;
    private final long computationTime;
    private final TimeUnit timeUnit;
    private final int gamesPerMatchup;

    public static void main(String[] args) throws Exception {
        var agent = List.of(
                new AgentSpec("AgentGreedyHeuristic", AgentGreedyHeuristic.class),
                new AgentSpec("RandomAgent", RandomAgent.class));
        var arena = new ParallelArena(agent, 10, TimeUnit.SECONDS, 3);
        arena.runGames(2);
    }

    public ParallelArena(List<AgentSpec> agents, long computationTime, TimeUnit timeUnit, int gamesPerMatchup) {
        this.agents = agents;
        this.computationTime = computationTime;
        this.timeUnit = timeUnit;
        this.gamesPerMatchup = gamesPerMatchup;
    }

    public void runGame(AgentSpec a, AgentSpec b) throws Exception {
        new Arena(a, b).playSingleGame();
    }

    public List<MatchupResult> runGames(int threads) {
        ArrayList<Future<MatchupResult>> futures = new ArrayList<>();
        try (var executorService = Executors.newFixedThreadPool(threads)) {
            for (var firstAgent : agents) {
                for (var secondAgent : agents) {
                    futures.add(executorService.submit(new Arena(firstAgent, secondAgent)));
                }
            }

            ArrayList<MatchupResult> results = new ArrayList<>(futures.size());
            for (Future<MatchupResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception exception) {
                    throw new RuntimeException("failed to execute arena matchup", exception);
                }
            }

            PrintResults.printAll(agents, results);
            return results;
        }
    }

    private class Arena implements Callable<MatchupResult> {
        private final AgentSpec firstAgent;
        private final AgentSpec secondAgent;

        private Arena(AgentSpec firstAgent, AgentSpec secondAgent) {
            this.firstAgent = firstAgent;
            this.secondAgent = secondAgent;
        }

        @Override
        public MatchupResult call() throws Exception {
            double collaborativeScoreSum = java.util.stream.IntStream.range(0, gamesPerMatchup)
                    .mapToDouble(i -> {
                        try {
                            MatchResult<CarcassonneGame, GameAgent<CarcassonneGame, CarcassonneAction>> result = playSingleGame();
                            return result.getResult()[0];
                        } catch (Exception exception) {
                            throw new RuntimeException("failed to execute single game in parallel", exception);
                        }
                    })
                    .sum();

            var avgScore = collaborativeScoreSum / gamesPerMatchup;
            return new MatchupResult(firstAgent.name(), secondAgent.name(), gamesPerMatchup, avgScore);
        }

        private MatchResult<CarcassonneGame, GameAgent<CarcassonneGame, CarcassonneAction>> playSingleGame()
                throws Exception {
            try (ExecutorService pool = Executors.newCachedThreadPool()) {

                PrintStream nullStream = new PrintStream(java.io.OutputStream.nullOutputStream());
                var logger = new Logger(
                        -1, "[match ", "",
                        "trace]: ", System.err, "",
                        "debug]: ", System.err, "",
                        "info]: ", System.out, "",
                        "warn]: ", System.err, "",
                        "error]: ", System.err, "");

                GameAgent<CarcassonneGame, CarcassonneAction> first = firstAgent.agentClass().getDeclaredConstructor()
                        .newInstance();
                GameAgent<CarcassonneGame, CarcassonneAction> second = secondAgent.agentClass().getDeclaredConstructor()
                        .newInstance();
                var match = new Match<CarcassonneGame, GameAgent<CarcassonneGame, CarcassonneAction>, CarcassonneAction>(
                        new CarcassonneGame(),
                        List.of(first, second),
                        computationTime,
                        timeUnit,
                        true,
                        logger,
                        pool,
                        10000);
                return match.call();
            }
        }
    }
}
