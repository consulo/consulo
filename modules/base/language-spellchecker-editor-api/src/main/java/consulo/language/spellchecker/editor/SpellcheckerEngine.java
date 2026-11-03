/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.configurable.UnnamedConfigurable;
import consulo.language.spellcheker.text.TextContent;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates words for the platform spellchecking.
 * <p>
 * The platform owns everything around validation: tokenizing, the inspection, quick fixes,
 * user/project dictionaries and the bundled word lists of {@link consulo.language.spellchecker.editor.dictionary.BundledDictionaryProvider}.
 * Words accepted by those are never passed to the engine.
 * <p>
 * Only the engine selected in <em>Settings | Editor | Spellchecking</em> is called. With automatic selection the engine with
 * the most {@link #getFeatures() features}, then the most {@link #getLocales() locales} is used.
 * An engine must not load its dictionaries or models until it is called for the first time,
 * so an installed but unused engine costs nothing.
 *
 * @author VISTALL
 * @since 2023-03-26
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SpellcheckerEngine {
    String getId();

    LocalizeValue getDisplayName();

    /**
     * @return kinds of problems the engine reports
     */
    default Set<SpellcheckerFeature> getFeatures() {
        return EnumSet.of(SpellcheckerFeature.SPELLING);
    }

    /**
     * @return languages the engine can check. Must be cheap: it is called for automatic engine selection, also while the engine is
     * not active, so dictionaries must not be loaded here
     */
    Set<Locale> getLocales();

    boolean hasProblem(Project project, String word);

    /**
     * @return suggestions for the word, best first. The platform keeps this order
     */
    List<String> getSuggestions(Project project, String word);

    /**
     * Checks grammar and style of a natural language text (a comment, a doc comment, a string literal, plain text).
     * Called only when {@link #getFeatures()} contains {@link SpellcheckerFeature#GRAMMAR} or {@link SpellcheckerFeature#STYLE}.
     *
     * @return problems with ranges in {@code text}
     */
    default List<SpellcheckerTextProblem> checkText(Project project, TextContent text) {
        return List.of();
    }

    /**
     * @param ruleId {@link SpellcheckerTextProblem#ruleId()} of a problem reported by {@link #checkText}
     * @return the name of the rule, shown by the quick fix which disables it; empty when the engine cannot disable the rule
     */
    default LocalizeValue getRuleDisplayName(String ruleId) {
        return LocalizeValue.empty();
    }

    /**
     * Turns off the rule which reported a text problem. Called only for rules with a {@link #getRuleDisplayName display name}.
     */
    default void disableRule(String ruleId) {
    }

    /**
     * @return a counter which changes whenever the engine options change the results of {@link #checkText}, e.g. a rule is disabled
     */
    default long getModificationCount() {
        return 0;
    }

    /**
     * @return engine options, shown in <em>Settings | Editor | Spellchecking</em> below the engine selector while this engine is selected
     */
    default @Nullable UnnamedConfigurable createConfigurable() {
        return null;
    }
}
