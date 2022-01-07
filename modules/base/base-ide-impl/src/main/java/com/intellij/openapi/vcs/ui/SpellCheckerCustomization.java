/*
 * Copyright 2013-2016 consulo.io
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
package com.intellij.openapi.vcs.ui;

import com.intellij.ui.EditorCustomization;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12:09/03.07.13
 *
 * Some hack - due spellchecker moved to plugins
 */
@Deprecated
public class SpellCheckerCustomization {
  private static SpellCheckerCustomization ourInstance = new SpellCheckerCustomization();

  public static void setInstance(SpellCheckerCustomization instance) {
    ourInstance = instance;
  }

  public static SpellCheckerCustomization getInstance() {
    return ourInstance;
  }

  @Nonnull
  public EditorCustomization getCustomization(boolean enabled) {
    throw new UnsupportedOperationException();
  }

  public boolean isEnabled() {
    return false;
  }
}
