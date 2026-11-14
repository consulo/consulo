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
package consulo.mcpServer.impl.internal.ai;

import consulo.annotation.component.ExtensionImpl;
import consulo.mcpServer.skill.BundledSkill;
import consulo.mcpServer.skill.BundledSkillsProvider;

import java.util.List;
import java.util.Set;

/**
 * The skill which teaches an agent the tools of the MCP server of the IDE.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class McpIdeSkillsProvider implements BundledSkillsProvider {
    @Override
    public List<BundledSkill> registeredSkills() {
        return List.of(new BundledSkill(
            "consulo-ide",
            Set.of(
                "search_symbol", "get_symbol_info", "analyze_calls", "search_text", "search_regex", "search_file",
                "rename_refactoring", "reformat_file", "get_file_problems", "lint_files", "build_project",
                "get_run_configurations", "execute_run_configuration"
            ),
            Set.of("search_symbol", "get_file_problems")
        ));
    }
}
