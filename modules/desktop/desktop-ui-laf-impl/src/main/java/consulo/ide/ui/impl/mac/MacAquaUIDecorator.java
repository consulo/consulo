/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.ui.impl.mac;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.ide.ui.laf.intellij.IntelliJLaf;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ui.SwingUIDecorator;
import consulo.ide.ui.impl.DefaultUIDecorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-07-23
 */
public class MacAquaUIDecorator implements SwingUIDecorator {
  @Override
  public boolean decorateSidebarTree(@Nonnull JTree tree) {
    DefaultUIDecorator.decorateTree0(tree, getSidebarColor());
    return true;
  }

  @Nullable
  @Override
  public Color getSidebarColor() {
    Color color = IntelliJLaf.isGraphite() ? DarculaUIUtil.MAC_GRAPHITE_COLOR : DarculaUIUtil.MAC_REGULAR_COLOR;

    return ColorUtil.desaturate(color, 8);
  }

  @Override
  public boolean isAvaliable() {
    return UIUtil.isUnderAquaLookAndFeel();
  }
}
