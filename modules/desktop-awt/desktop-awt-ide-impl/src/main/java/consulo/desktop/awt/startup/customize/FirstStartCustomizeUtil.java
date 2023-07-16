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
package consulo.desktop.awt.startup.customize;

import consulo.desktop.awt.startup.customizeNew.CustomizeIDEWizardDialog;
import consulo.desktop.awt.ui.plaf.darcula.DarculaLaf;
import consulo.desktop.awt.ui.plaf.intellij.IntelliJLaf;
import consulo.externalService.update.UpdateChannel;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 27.11.14
 */
public class FirstStartCustomizeUtil {
  @RequiredUIAccess
  public static void showDialog(boolean initLaf, boolean isDark, @Nullable UpdateChannel updateChannel) {
    if (initLaf) {
      initLaf(isDark);
    }

    new CustomizeIDEWizardDialog(isDark, updateChannel).showAsync();
  }

  private static void initLaf(boolean isDark) {
    try {
      UIManager.setLookAndFeel(isDark ? new DarculaLaf() : new IntelliJLaf());
    }
    catch (Exception ignored) {
    }
  }
}
