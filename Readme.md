### Verification Helper

This repository implements modules which were used to validate that the implementation works as expected and to evaluate and improve agents.

#### ParallelArena

Allows for easy benchmarking of the agents in parallel manner. Performs all possible matches and gathers the results over multiple independent trials. Then produces tables with averages to interpret the results.

#### HeuristicOptimization

Implements a Genetic Algorithm aimed at optimizing the parameters of the heuristic. The Default Heuristic was a result from that optimizer


#### Agent Renderer

Very simple renderer for the game. Takes two agents and renders into java swing window. Was used to validate as an end to end test that the points and placement logic works