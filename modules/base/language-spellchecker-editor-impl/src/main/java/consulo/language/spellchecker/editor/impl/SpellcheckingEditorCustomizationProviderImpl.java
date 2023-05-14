/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.spellchecker.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.EditorEx;
import consulo.language.spellchecker.editor.SpellcheckingEditorCustomizationProvider;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * TODO impl handling different engines
 *
 * @author nik
 */
@ServiceImpl
@Singleton
public class SpellcheckingEditorCustomizationProviderImpl implements SpellcheckingEditorCustomizationProvider {

  private final SpellcheckerEngineManager mySpellcheckerEngineManager;

  @Inject
  public SpellcheckingEditorCustomizationProviderImpl(SpellcheckerEngineManager spellcheckerEngineManager) {
    mySpellcheckerEngineManager = spellcheckerEngineManager;
  }

  @Nonnull
  @Override
  public Optional<Consumer<EditorEx>> getCustomizationOpt(boolean enabled) {
    SpellcheckerEngine activeEngine = mySpellcheckerEngineManager.getActiveEngine();
    if (activeEngine == null) {
      return Optional.empty();
    }

    SpellCheckingEditorCustomization customization = new SpellCheckingEditorCustomization(enabled, activeEngine.getId());
    return Optional.of(customization);
  }
}
