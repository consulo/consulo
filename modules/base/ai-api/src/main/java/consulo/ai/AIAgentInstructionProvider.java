/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;

import java.util.List;

/**
 * Tells where an agent reads the instructions of a project - its instruction files, rule directories and skills - so
 * the IDE can list them and add, edit or remove them.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AIAgentInstructionProvider {
    /**
     * The agent, or the convention, the locations belong to.
     */
    LocalizeValue getName();

    /**
     * Files the agent reads at the root of a project, like {@code AGENTS.md}.
     */
    default List<String> getInstructionFiles() {
        return List.of();
    }

    /**
     * Directories, relative to the root of a project, the agent reads rule files from.
     */
    default List<AIAgentRuleDirectory> getRuleDirectories() {
        return List.of();
    }

    /**
     * Directories of skills - each skill a directory with a {@code SKILL.md} - relative to the root of a project and to
     * the home directory of the user, like {@code .agents/skills}.
     */
    default List<String> getSkillDirectories() {
        return List.of();
    }
}
