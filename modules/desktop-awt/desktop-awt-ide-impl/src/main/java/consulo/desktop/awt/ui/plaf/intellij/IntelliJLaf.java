/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.plaf.intellij;

import consulo.desktop.awt.ui.plaf.darcula.DarculaLaf;
import consulo.desktop.awt.ui.plaf.ideaOld.IdeaBlueMetalTheme;

import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * @author Konstantin Bulenkov
 */
public class IntelliJLaf extends DarculaLaf {
  @Override
  public String getName() {
    return "IntelliJ";
  }

  @Override
  public String getID() {
    return "intellij";
  }

  @Override
  protected String getPrefix() {
    return "intellijlaf";
  }

  @Override
  public boolean isDark() {
    return false;
  }

  @Override
  protected DefaultMetalTheme createMetalTheme() {
    return new IdeaBlueMetalTheme();
  }
}
