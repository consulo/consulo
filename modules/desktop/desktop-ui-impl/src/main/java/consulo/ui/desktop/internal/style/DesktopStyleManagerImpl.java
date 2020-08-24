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
package consulo.ui.desktop.internal.style;

import com.intellij.ide.ui.DesktopAntialiasingTypeUtil;
import com.intellij.ide.ui.LafManager;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ui.AntialiasingType;
import consulo.ui.impl.style.StyleManagerImpl;
import consulo.ui.style.Style;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 06-Nov-17
 */
public class DesktopStyleManagerImpl extends StyleManagerImpl {
  public static final DesktopStyleManagerImpl ourInstance = new DesktopStyleManagerImpl();

  private DesktopStyleManagerImpl() {
  }

  @Override
  public void setCurrentStyle(@Nonnull Style newStyle) {
    LafManager lafManager = LafManager.getInstance();
    Style oldStyle = lafManager.getCurrentStyle();
    lafManager.setCurrentStyle(newStyle);
    fireStyleChanged(oldStyle, newStyle);
  }

  @Nonnull
  @Override
  public List<Style> getStyles() {
    return LafManager.getInstance().getStyles();
  }

  @Nonnull
  @Override
  public Style getCurrentStyle() {
    return LafManager.getInstance().getCurrentStyle();
  }

  @Override
  public void refreshUI() {
    LafManager.getInstance().updateUI();
  }

  @Override
  public void refreshAntialiasingType(@Nonnull AntialiasingType antialiasingType) {
    for (Window w : Window.getWindows()) {
      for (JComponent c : UIUtil.uiTraverser(w).filter(JComponent.class)) {
        GraphicsUtil.setAntialiasingType(c, DesktopAntialiasingTypeUtil.getAntialiasingTypeForSwingComponent());
      }
    }
  }
}
