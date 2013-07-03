/*
 * Copyright 2013 Consulo.org
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
package com.intellij.spellchecker.vcs;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.vcs.ui.SpellCheckerCustomization;
import com.intellij.spellchecker.ui.SpellCheckingEditorCustomization;
import com.intellij.ui.EditorCustomization;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12:12/03.07.13
 */
public class SpellCheckerCustomizationImpl extends SpellCheckerCustomization implements ApplicationComponent{
  @NotNull
  @Override
  public EditorCustomization getCustomization(boolean enabled) {
    return SpellCheckingEditorCustomization.getInstance(enabled);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void initComponent() {
    setInstance(new SpellCheckerCustomizationImpl());
  }

  @Override
  public void disposeComponent() {
  }

  @NotNull
  @Override
  public String getComponentName() {
    return getClass().getSimpleName();
  }
}
