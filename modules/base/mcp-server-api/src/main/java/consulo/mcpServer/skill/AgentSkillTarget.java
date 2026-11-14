// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.mcpServer.skill;

import java.util.Set;

/**
 * An agent that can receive a bundled skill.
 */
public enum AgentSkillTarget {
    CLAUDE,
    CODEX,
    JUNIE,
    OTHER;

    /**
     * All targets.
     */
    public static final Set<AgentSkillTarget> ALL = Set.of(values());
}
