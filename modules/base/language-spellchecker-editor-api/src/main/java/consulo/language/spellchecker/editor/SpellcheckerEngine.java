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
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 26/03/2023
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SpellcheckerEngine {
  @Nonnull
  String getId();

  @Nonnull
  LocalizeValue getDisplayName();

  @Nonnull
  List<String> getSuggestions(@Nonnull Project project, @Nonnull String text);

  boolean hasProblem(@Nonnull Project project, @Nonnull String word);

  default boolean canSaveUserWords(@Nonnull Project project) {
    return false;
  }

  default void acceptWordAsCorrect(@Nonnull Project project, @Nonnull String word) {
    throw new UnsupportedOperationException();
  }
}
