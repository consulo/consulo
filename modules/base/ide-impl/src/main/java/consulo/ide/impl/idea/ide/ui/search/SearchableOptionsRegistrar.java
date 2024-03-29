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

package consulo.ide.impl.idea.ide.ui.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.DocumentEvent;
import java.util.Map;
import java.util.Set;

/**
 * User: anna
 * Date: 13-Feb-2006
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class SearchableOptionsRegistrar {
  public static SearchableOptionsRegistrar getInstance(){
    return ServiceManager.getService(SearchableOptionsRegistrar.class);
  }

  @Nonnull
  public abstract ConfigurableHit getConfigurables(final Configurable[] allConfigurables,
                                                     final DocumentEvent.EventType type,
                                                     final Set<Configurable> configurables,
                                                     final String option,
                                                     final Project project);

  @Nullable
  public abstract String getInnerPath(SearchableConfigurable configurable, String option);

  public abstract void addOption(String option, String path, String hit, final String configurableId, final String configurableDisplayName);

  public abstract boolean isStopWord(String word);

  public abstract Set<String> getSynonym(final String option, @Nonnull final SearchableConfigurable configurable);

  public abstract Set<String> replaceSynonyms(Set<String> options, SearchableConfigurable configurable);

  public abstract Map<String, Set<String>> findPossibleExtension(@Nonnull String prefix, final Project project);


  public abstract Set<String> getProcessedWordsWithoutStemming(@Nonnull String text);

  public abstract Set<String> getProcessedWords(@Nonnull String text);

}
