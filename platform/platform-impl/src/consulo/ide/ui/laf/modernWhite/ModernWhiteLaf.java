/*
 * Copyright 2013-2014 must-be.org
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
package consulo.ide.ui.laf.modernWhite;

import com.intellij.ide.ui.laf.ideaOld.IdeaBlueMetalTheme;
import consulo.ide.ui.laf.modernDark.ModernDarkLaf;

import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * @author VISTALL
 * @since 02.03.14
 */
public class ModernWhiteLaf extends ModernDarkLaf {
  @Override
  public String getName() {
    return "Modern White";
  }

  @Override
  protected String getPrefix() {
    return "modernWhite";
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
