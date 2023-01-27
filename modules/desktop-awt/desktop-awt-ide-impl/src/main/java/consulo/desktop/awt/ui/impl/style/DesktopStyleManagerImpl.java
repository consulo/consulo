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
package consulo.desktop.awt.ui.impl.style;

import consulo.ide.impl.idea.ide.ui.LafManager;
import consulo.ui.AntialiasingType;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.impl.style.StyleManagerImpl;
import consulo.ui.style.Style;
import consulo.util.lang.lazy.LazyValue;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 06-Nov-17
 */
public class DesktopStyleManagerImpl extends StyleManagerImpl {
  public static final DesktopStyleManagerImpl ourInstance = new DesktopStyleManagerImpl();

  private final Supplier<LafManager> myLafManager = LazyValue.notNull(LafManager::getInstance);

  private DesktopStyleManagerImpl() {
  }

  @Override
  public void setCurrentStyle(@Nonnull Style newStyle) {
    LafManager lafManager = myLafManager.get();
    Style oldStyle = lafManager.getCurrentStyle();
    lafManager.setCurrentStyle(newStyle);
    fireStyleChanged(oldStyle, newStyle);
  }

  @Nonnull
  @Override
  public List<Style> getStyles() {
    return myLafManager.get().getStyles();
  }

  @Nonnull
  @Override
  public Style getCurrentStyle() {
    return myLafManager.get().getCurrentStyle();
  }

  @Override
  public void refreshUI() {
    myLafManager.get().updateUI();
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
