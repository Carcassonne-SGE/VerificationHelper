

package heuristicOptimization;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.game.Match;
import model.heuristic.HeuristicConfiguration;
import mcts.AgentGreedyHeuristic;
import sge.CarcassonneAction;
import sge.CarcassonneGame;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParallelEvaluator {

    private final ExecutorService chunkExecutor;
    private final ExecutorService matchExecutor;
    private final Logger quietLogger;

    public ParallelEvaluator() {
        int threads = Runtime.getRuntime().availableProcessors();

        this.chunkExecutor = Executors.newFixedThreadPool(threads);
        this.matchExecutor = Executors.newCachedThreadPool();

        PrintStream nullStream = new PrintStream(java.io.OutputStream.nullOutputStream());
        this.quietLogger = new Logger(
                -1, "[match ", "",
                "trace]: ", System.err, "",
                "debug]: ", System.err, "",
                "info]: ",nullStream, "",
                "warn]: ", System.err, "",
                "error]: ", System.err, ""
        );
    }

    public List<PerformanceResult> evaluatePerformance(List<HeuristicConfiguration> population, int chunkSize, int gamesPerConfig) {
        List<Future<List<PerformanceResult>>> futures = new ArrayList<>();

        for (int i = 0; i < population.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, population.size());
            List<HeuristicConfiguration> chunk = population.subList(start, end);

            futures.add(chunkExecutor.submit(() -> {
                List<PerformanceResult> chunkResults = new ArrayList<>();
                for (var config : chunk) {
                    float scoreSum = 0.0f;
                    float mini = Float.MAX_VALUE;
                    float maxi = -Float.MAX_VALUE;

                    for (int g = 0; g < gamesPerConfig; g++) {
                        float v = playEvaluationGame(new HeuristicConfiguration[]{config},g);
                        scoreSum += v;
                        mini = Math.min(mini,v);
                        maxi = Math.max(maxi,v);
                    }

                    float avgScore = scoreSum / gamesPerConfig;
                    chunkResults.add(new PerformanceResult(config, avgScore, avgScore));
                }
                return chunkResults;
            }));
        }

        List<PerformanceResult> results = new ArrayList<>(population.size());
        for (var future : futures) {
            try {
                results.addAll(future.get());
            } catch (Exception e) {
                throw new RuntimeException("Kritischer Fehler bei der parallelen Evaluierung", e);
            }
        }
        return results;
    }

    public float avg(HeuristicConfiguration[] config, int games) throws Exception {
        float sum = 0.0f;
        for(int i = 0; i < games; i++){
            float result = playEvaluationGame(config, i);
            sum += result;
        }
        return sum/games;
    }

    public float playEvaluationGame(HeuristicConfiguration[] config,int seed) throws Exception {
        var agent1 = new AgentGreedyHeuristic(config);
        var agent2 = new AgentGreedyHeuristic(config);

        var match = new Match<CarcassonneGame, GameAgent<CarcassonneGame, CarcassonneAction>, CarcassonneAction>(
                new CarcassonneGame(),
                List.of(agent2, agent1),
                3000,
                TimeUnit.MILLISECONDS,
                false,
                quietLogger,
                matchExecutor,
                10000
        );

        var result = match.call();
        if(result.getResult()[0] < 0 || result.getResult()[1] < 0){
            System.out.println("sus");
        }
        return (float) result.getResult()[0];
    }

    public void shutdown() {
        chunkExecutor.shutdown();
        matchExecutor.shutdown();
        try {
            if (!chunkExecutor.awaitTermination(3, TimeUnit.SECONDS)) chunkExecutor.shutdownNow();
            if (!matchExecutor.awaitTermination(3, TimeUnit.SECONDS)) matchExecutor.shutdownNow();
        } catch (InterruptedException e) {
            chunkExecutor.shutdownNow();
            matchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
