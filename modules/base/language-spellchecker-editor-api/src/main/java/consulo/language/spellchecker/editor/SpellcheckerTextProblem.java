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
package consulo.language.spellchecker.editor;

import consulo.document.util.TextRange;
import consulo.localize.LocalizeValue;

import java.util.List;

/**
 * A problem found by {@link SpellcheckerEngine#checkText} in a natural language text.
 *
 * @param kind        {@link SpellcheckerFeature#GRAMMAR} or {@link SpellcheckerFeature#STYLE}; spelling is reported per word
 * @param range       range in the checked text
 * @param message     short description shown in the editor
 * @param suggestions replacements of the range, best first
 * @param ruleId      engine specific rule id, used to tell problems apart
 * @author VISTALL
 * @since 2026-11-01
 */
public record SpellcheckerTextProblem(SpellcheckerFeature kind, TextRange range, LocalizeValue message, List<String> suggestions, String ruleId) {
}
