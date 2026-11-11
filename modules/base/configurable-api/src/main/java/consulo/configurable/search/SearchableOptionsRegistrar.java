/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.configurable.search;

import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * @author anna
 * @since 2006-02-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class SearchableOptionsRegistrar {
    public static SearchableOptionsRegistrar getInstance() {
        return Application.get().getInstance(SearchableOptionsRegistrar.class);
    }

    public abstract ConfigurableHit getConfigurables(
        Configurable[] allConfigurables,
        boolean changed,
        @Nullable Set<Configurable> configurables,
        String option,
        Project project
    );

    public abstract @Nullable String getInnerPath(SearchableConfigurable configurable, String option);




    public abstract Map<String, Set<String>> findPossibleExtension(String prefix, Project project);

    public abstract Set<String> getProcessedWordsWithoutStemming(String text);

    /**
     * The stem of a lower cased word, by the {@link SearchStemmer} of its language - an English word by the English
     * one, any other by the one of the current locale, kept as it is when no plugin brings one. Null for no word.
     */
    public abstract @Nullable String stem(String word);

    /**
     * The options of the words starting with the prefix, or its stem - null when there is none.
     */
    public abstract @Nullable Set<OptionDescription> getAcceptableDescriptions(String prefix);

    public abstract Set<String> getProcessedWords(String text);
}
