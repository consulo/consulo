/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.ui.laf.darcula;

import com.intellij.ide.IdeBundle;
import consulo.ide.ui.laf.LafWithColorScheme;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaLookAndFeelInfo extends UIManager.LookAndFeelInfo implements LafWithColorScheme {
  public DarculaLookAndFeelInfo() {
    super(IdeBundle.message("idea.dark.look.and.feel"), DarculaLaf.class.getName());
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof DarculaLookAndFeelInfo);
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @NotNull
  @Override
  public String getColorSchemeName() {
    return getName();
  }
}
