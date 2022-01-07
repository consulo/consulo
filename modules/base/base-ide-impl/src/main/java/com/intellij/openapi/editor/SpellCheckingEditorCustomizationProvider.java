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
package com.intellij.openapi.editor;

import com.intellij.openapi.vcs.ui.SpellCheckerCustomization;
import com.intellij.ui.EditorCustomization;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public class SpellCheckingEditorCustomizationProvider {
  private static final SpellCheckingEditorCustomizationProvider ourInstance = new SpellCheckingEditorCustomizationProvider();

  @Nonnull
  public static SpellCheckingEditorCustomizationProvider getInstance() {
    return ourInstance;
  }

  @Nullable
  public final EditorCustomization getCustomization(boolean enabled) {
    return enabled ? getEnabledCustomization() : getDisabledCustomization();
  }

  @Nullable
  public EditorCustomization getEnabledCustomization() {
    SpellCheckerCustomization spellCheckerCustomization = SpellCheckerCustomization.getInstance();
    return spellCheckerCustomization.isEnabled() ? spellCheckerCustomization.getCustomization(true) : null;
  }

  @Nullable
  public EditorCustomization getDisabledCustomization() {
    SpellCheckerCustomization spellCheckerCustomization = SpellCheckerCustomization.getInstance();
    return spellCheckerCustomization.isEnabled() ? spellCheckerCustomization.getCustomization(false) : null;
  }
}
