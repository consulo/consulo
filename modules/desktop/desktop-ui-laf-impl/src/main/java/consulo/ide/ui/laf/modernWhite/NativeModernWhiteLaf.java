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
package consulo.ide.ui.laf.modernWhite;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * @author VISTALL
 * @since 02.03.14
 */
public class NativeModernWhiteLaf extends ModernWhiteLaf {
  @Nonnull
  @Override
  public UIDefaults getDefaultsImpl(UIDefaults superDefaults) {
    UIDefaults defaults = super.getDefaultsImpl(superDefaults);
    if (DwmApi.Wrapper.DwmIsCompositionEnabled()) {
      int[] colorWithAlpha = DwmApi.Wrapper.DwmGetColorizationColor();

      Color value = new Color(colorWithAlpha[0], colorWithAlpha[1] == 1);
      for (Map.Entry<Object, Object> entry : defaults.entrySet()) {
        String key = entry.getKey().toString();
        if(key.endsWith(".selectionBackground"))  {
          defaults.put(key, value);
        }
      }
      defaults.put("Color.SelectionBackground", value);
      defaults.put("Hyperlink.linkColor", new Color(value.getRed(), value.getGreen(), value.getBlue(), value.getAlpha() + 10));
      defaults.put("ProgressBar.foreground", value);
      defaults.put("ProgressBar.stepColor2", new Color(value.getRed(), value.getGreen(), value.getBlue(), value.getAlpha() + 10));
    }
    return defaults;
  }

  @Override
  public String getName() {
    return "Modern Light (native color)";
  }

  @Override
  public String getID() {
    return "modern-light-native";
  }
}
