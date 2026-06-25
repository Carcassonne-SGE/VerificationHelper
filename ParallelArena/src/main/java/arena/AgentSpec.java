package arena;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import sge.CarcassonneAction;
import sge.CarcassonneGame;

/// AgentSpec
///
/// stores the visible name and java class of one agent in the arena
public record AgentSpec(
        String name,
        Class<? extends GameAgent<CarcassonneGame, CarcassonneAction>> agentClass
) {}
