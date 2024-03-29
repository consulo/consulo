/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ui.ex.awt;

import consulo.ui.ex.awt.TitledSeparator;

import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public class SeparatorFactory {
  private SeparatorFactory() {
  }

  @Deprecated
  public static TitledSeparator createSeparator(String text, @Nullable JComponent labelFor, boolean boldFont, boolean smallFont) {
    return new TitledSeparator(text, labelFor);
  }

  public static TitledSeparator createSeparator(String text, @Nullable JComponent labelFor) {
    return new TitledSeparator(text, labelFor);
  }

  @Deprecated
  public static JComponent createSeparatorWithBoldTitle(String text, @Nullable JComponent labelFor) {
    TitledSeparator separator = new TitledSeparator(text, labelFor);
    separator.setTitleFont(separator.getTitleFont().deriveFont(Font.BOLD));
    return separator;
  }
}