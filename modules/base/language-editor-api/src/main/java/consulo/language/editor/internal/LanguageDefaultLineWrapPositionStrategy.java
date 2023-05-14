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
package consulo.language.editor.internal;

import consulo.codeEditor.DefaultLineWrapPositionStrategy;
import consulo.language.Language;
import consulo.language.editor.LanguageLineWrapPositionStrategy;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 01-Aug-22
 */
public class LanguageDefaultLineWrapPositionStrategy extends DefaultLineWrapPositionStrategy implements LanguageLineWrapPositionStrategy {
  public static final LanguageDefaultLineWrapPositionStrategy INSTANCE = new LanguageDefaultLineWrapPositionStrategy();

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}
