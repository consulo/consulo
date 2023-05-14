/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.refactoring.internal;

import consulo.language.Language;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

public class DefaultNamesValidator implements NamesValidator {
  @Override
  public boolean isIdentifier(final String name, final Project project) {
    final int len = name.length();
    if (len == 0) return false;

    if (!Character.isJavaIdentifierStart(name.charAt(0))) return false;

    for (int i = 1; i < len; i++) {
      if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
    }

    return true;
  }

  @Override
  public boolean isKeyword(final String name, final Project project) {
    return false;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}
