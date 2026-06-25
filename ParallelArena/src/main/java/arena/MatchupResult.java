package arena;

/// MatchupResult
///
/// Stores the aggregated collaborative result of one agent pairing over one or multiple games.
public record MatchupResult(
        String firstAgent,
        String secondAgent,
        int gamesPlayed,
        double averageCollaborativeScore
) {}
