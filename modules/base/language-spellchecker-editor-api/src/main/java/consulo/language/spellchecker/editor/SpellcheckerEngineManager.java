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
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 26/03/2023
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface SpellcheckerEngineManager {
  @Nullable
  SpellcheckerEngine getActiveEngine();

  @Nonnull
  default List<String> getSuggestions(@Nonnull Project project, @Nonnull String text) {
    SpellcheckerEngine engine = getActiveEngine();
    if (engine == null) {
      return List.of();
    }
    return engine.getSuggestions(project, text);
  }

  default boolean hasProblem(@Nonnull Project project, @Nonnull String word) {
    SpellcheckerEngine engine = getActiveEngine();
    return engine != null && engine.hasProblem(project, word);
  }

  default boolean canSaveUserWords(@Nonnull Project project) {
    SpellcheckerEngine engine = getActiveEngine();
    return engine != null && engine.canSaveUserWords(project);
  }

  default void acceptWordAsCorrect(@Nonnull Project project, @Nonnull String word) {
    if (!canSaveUserWords(project)) {
      throw new IllegalArgumentException("#canSaveUserWords(Project) return false");
    }

    SpellcheckerEngine engine = getActiveEngine();
    if (engine != null) {
      engine.acceptWordAsCorrect(project, word);
    }
  }
}
