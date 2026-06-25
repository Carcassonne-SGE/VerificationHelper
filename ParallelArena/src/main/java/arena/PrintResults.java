package arena;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.vandermeer.asciitable.AsciiTable;

public final class PrintResults {
    private PrintResults() {
    }

    public static void printAll(List<AgentSpec> agents, List<MatchupResult> results) {
        printMatchupTable(agents, results);
        printSelfPlayRanking(results);
        printOverallRanking(agents, results);
    }

    private static void printMatchupTable(List<AgentSpec> agents, List<MatchupResult> results) {
        List<AgentSpec> sortedAgents = agents.stream().sorted(Comparator.comparing(AgentSpec::name)).toList();
        Map<String, Map<String, MatchupResult>> byFirstThenSecond = new LinkedHashMap<>();
        for (MatchupResult result : results) {
            byFirstThenSecond.computeIfAbsent(result.firstAgent(), ignored -> new LinkedHashMap<>())
                    .put(result.secondAgent(), result);
        }

        System.out.println("Matchup matrix:");
        AsciiTable table = new AsciiTable();
        table.addRule();
        
        List<String> header = new ArrayList<>();
        header.add("");
        for (AgentSpec agent : sortedAgents) {
            header.add(agent.name());
        }
        header.add("row avg");
        table.addRow(header);
        table.addRule();

        for (AgentSpec rowAgent : sortedAgents) {
            List<String> rowCells = new ArrayList<>();
            rowCells.add(rowAgent.name());
            double rowSum = 0.0;
            int rowCount = 0;
            Map<String, MatchupResult> row = byFirstThenSecond.getOrDefault(rowAgent.name(), Map.of());
            for (AgentSpec columnAgent : sortedAgents) {
                MatchupResult result = row.get(columnAgent.name());
                if (result == null) {
                    rowCells.add("-");
                } else {
                    double value = result.averageCollaborativeScore();
                    rowCells.add(format(value));
                    rowSum += value;
                    rowCount++;
                }
            }
            rowCells.add(rowCount == 0 ? "-" : format(rowSum / rowCount));
            table.addRow(rowCells);
        }

        table.addRule();
        List<String> colAvgRow = new ArrayList<>();
        colAvgRow.add("col avg");
        for (AgentSpec columnAgent : sortedAgents) {
            double columnSum = 0.0;
            int columnCount = 0;
            for (AgentSpec rowAgent : sortedAgents) {
                MatchupResult result = byFirstThenSecond.getOrDefault(rowAgent.name(), Map.of()).get(columnAgent.name());
                if (result != null) {
                    columnSum += result.averageCollaborativeScore();
                    columnCount++;
                }
            }
            colAvgRow.add(columnCount == 0 ? "-" : format(columnSum / columnCount));
        }
        colAvgRow.add("");
        table.addRow(colAvgRow);
        table.addRule();
        System.out.println(table.render());
    }

    private static void printSelfPlayRanking(List<MatchupResult> results) {
        List<Map.Entry<String, Double>> rows = results.stream()
                .filter(result -> result.firstAgent().equals(result.secondAgent()))
                .sorted(Comparator.comparingDouble(MatchupResult::averageCollaborativeScore).reversed())
                .map(result -> Map.entry(result.firstAgent(), result.averageCollaborativeScore()))
                .toList();

        System.out.println("Self play ranking:");
        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("rank", "agent", "self avg");
        table.addRule();
        for (int i = 0; i < rows.size(); i++) {
            Map.Entry<String, Double> row = rows.get(i);
            table.addRow(String.valueOf(i + 1), row.getKey(), format(row.getValue()));
        }
        table.addRule();
        System.out.println(table.render());
    }

    private static void printOverallRanking(List<AgentSpec> agents, List<MatchupResult> results) {
        Map<String, double[]> totals = new LinkedHashMap<>();
        for (AgentSpec agent : agents) {
            totals.put(agent.name(), new double[2]);
        }

        for (MatchupResult result : results) {
            double[] first = totals.get(result.firstAgent());
            if (first != null) {
                first[0] += result.averageCollaborativeScore();
                first[1] += 1;
            }

            double[] second = totals.get(result.secondAgent());
            if (second != null) {
                second[0] += result.averageCollaborativeScore();
                second[1] += 1;
            }
        }

        List<Map.Entry<String, Double>> rows = totals.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()[1] == 0 ? 0.0 : entry.getValue()[0] / entry.getValue()[1]))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();

        System.out.println("Overall ranking:");
        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("rank", "agent", "overall avg");
        table.addRule();
        for (int i = 0; i < rows.size(); i++) {
            Map.Entry<String, Double> row = rows.get(i);
            table.addRow(String.valueOf(i + 1), row.getKey(), format(row.getValue()));
        }
        table.addRule();
        System.out.println(table.render());
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }
}
