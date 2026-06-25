package heuristicOptimization;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;

import model.heuristic.AreaPointChangeConfig;
import model.heuristic.HeuristicConfiguration;
import model.heuristic.HeuristicMixConfig;
import model.heuristic.MeeplePlacementHeuristic;
import model.heuristic.PositionHeuristik;

/// HeuristicOptimizer
///
/// Optimizes all numeric parameters of HeuristicConfiguration using a parallelized Memetic Algorithm.
/// Combines a Genetic Algorithm
public class HeuristicOptimizer {
    private float keep = 0.25f;
    private ParallelEvaluator evaluator = new ParallelEvaluator();
    private Random rand = new Random(0);
    private Mutator<HeuristicConfiguration> mut = new Mutator<HeuristicConfiguration>(HeuristicConfiguration.class);


    public List<PerformanceResult> optimize(int iter, int populationSize){
        var population = mut.randomPopulation(populationSize,rand);
        List<PerformanceResult> eliteResult = null;
        for(int i = 0; i < iter; i++){
            eliteResult = evaluator.evaluatePerformance(population,1,10).stream()
                    .sorted(Comparator.comparing(PerformanceResult::performance).reversed())
                    .limit( Math.round(populationSize * keep))
                    .toList();

            DoubleSummaryStatistics stats = eliteResult.stream()
                    .mapToDouble(PerformanceResult::raw)
                    .summaryStatistics();

            System.out.printf("%d iter \t min: %.3f \t mean %.3f \t  max %.3f%n",i, stats.getMin(), stats.getAverage(),stats.getMax());
            var elite = eliteResult.stream().map(PerformanceResult::config).toList();
            
            int eliteCount = elite.size();
            int childrenCount = populationSize - eliteCount;
            List<HeuristicConfiguration> parentsForCrossover = new ArrayList<>(childrenCount);
            for (int j = 0; j < childrenCount; j++) {
                parentsForCrossover.add(elite.get(rand.nextInt(eliteCount)));
            }

            var children = mut.crossOver(HeuristicConfiguration.class,parentsForCrossover,rand,100f,0.2f);
            
            List<HeuristicConfiguration> newPopulation = new ArrayList<>(populationSize);
            newPopulation.addAll(elite);
            newPopulation.addAll(children);
            population = newPopulation;


        }
        return eliteResult.stream().sorted(Comparator.comparing(PerformanceResult::performance).reversed()).toList();
    }




    public static void main(String[] args) throws Exception {
        int iterations =100;
        int populationSize = 1000;
        int heuristicsCount = 1;
        new HeuristicOptimizer().optimize(1,10);
    }
}
