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

import java.util.List;

/**
 * A directory of rule files an agent reads.
 *
 * @param path       relative to the root of a project
 * @param extensions the extensions of rule files, with the dot - a new rule gets the first
 * @param template   the text of a new rule, {@code {name}} replaced by its name
 * @author VISTALL
 * @since 2026-11-14
 */
public record AIAgentRuleDirectory(String path, List<String> extensions, String template) {
    public String newFileExtension() {
        return extensions.isEmpty() ? ".md" : extensions.getFirst();
    }
}
