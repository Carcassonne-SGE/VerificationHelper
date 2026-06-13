package heuristicOptimization;

import model.heuristic.HeuristicConfiguration;

public record PerformanceResult (HeuristicConfiguration config, float performance, float raw) {
}
