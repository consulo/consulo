/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui;

import com.intellij.openapi.util.ClearableLazyValue;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.synth.ColorType;
import javax.swing.plaf.synth.SynthContext;
import java.awt.*;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 6/7/17
 */
public class GTKPlusUIUtil {
  private static final ClearableLazyValue<Boolean> ourValue = new ClearableLazyValue<Boolean>() {
    @NotNull
    @Override
    protected Boolean compute() {
      if (!UIUtil.isUnderGTKLookAndFeel()) {
        return false;
      }

      JTextArea dummyArea = new JTextArea();
      dummyArea.updateUI();

      SynthContext synthContext = getSynthContext(dummyArea.getUI(), dummyArea);

      Color colorBack = synthContext.getStyle().getColor(synthContext, ColorType.TEXT_BACKGROUND);
      Color colorFore = synthContext.getStyle().getColor(synthContext, ColorType.TEXT_FOREGROUND);

      double textAvg = colorFore.getRed() / 256. + colorFore.getGreen() / 256. + colorFore.getBlue() / 256.;
      double bgAvg = colorBack.getRed() / 256. + colorBack.getGreen() / 256. + colorBack.getBlue() / 256.;
      return textAvg > bgAvg;
    }
  };

  public static void updateUI() {
    ourValue.drop();
  }

  public static boolean isDarkTheme() {
    return ourValue.getValue();
  }

  public static SynthContext getSynthContext(final ComponentUI ui, final JComponent item) {
    try {
      final Method getContext = ui.getClass().getMethod("getContext", JComponent.class);
      getContext.setAccessible(true);
      return (SynthContext)getContext.invoke(ui, item);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
