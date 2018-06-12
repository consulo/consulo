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
package consulo.ui.internal;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.impl.style.StyleImpl;
import consulo.ui.impl.style.StyleManagerImpl;
import consulo.ui.shared.ColorValue;
import consulo.ui.style.ColorKey;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.ui.style.Style;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 06-Nov-17
 */
public class DesktopStyleManagerImpl extends StyleManagerImpl {
  public DesktopStyleManagerImpl() {
    setCurrentStyle(new StyleImpl() {
      @Nonnull
      @Override
      public ColorValue getColor(@Nonnull ColorKey colorKey) {
        if (colorKey == ComponentColors.TEXT) {
          return TargetAWT.from(UIUtil.getLabelForeground());
        }
        else if (colorKey == ComponentColors.DISABLED_TEXT) {
          return TargetAWT.from(UIUtil.getInactiveTextColor());
        }
        else if (colorKey == ComponentColors.BORDER) {
          return TargetAWT.from(UIUtil.getBorderColor());
        }
        else if (colorKey == StandardColors.BLUE) {
          return TargetAWT.from(JBColor.BLUE);
        }
        else if (colorKey == StandardColors.RED) {
          return TargetAWT.from(JBColor.RED);
        }
        else if (colorKey == StandardColors.GREEN) {
          return TargetAWT.from(JBColor.GREEN);
        }
        else if (colorKey == StandardColors.GRAY) {
          return TargetAWT.from(JBColor.GRAY);
        }
        else if (colorKey == StandardColors.LIGHT_GRAY) {
          return TargetAWT.from(JBColor.LIGHT_GRAY);
        }
        throw new UnsupportedOperationException(colorKey.toString());
      }
    });
  }

  @Nonnull
  @Override
  public List<Style> getStyles() {
    return Collections.emptyList();
  }
}
