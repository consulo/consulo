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
package consulo.ide.ui.laf.modern;

import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class ModernProgressBarUI extends DarculaProgressBarUI {
  public static ComponentUI createUI(JComponent c) {
    return new ModernProgressBarUI();
  }

  @Override
  protected Color getFinishedColor() {
    return UIManager.getColor("ProgressBar.stepColor2");
  }

  @Override
  protected Color getStartColor() {
    return getFinishedColor();
  }

  @Override
  protected Color getEndColor() {
    return getStartColor().darker();
  }

  @Override
  protected int getDefaultWidth() {
    return 12;
  }

  @Override
  protected Shape getShapedRect(float x, float y, float w, float h, float ar) {
    return new Rectangle2D.Float(x, y, w, h);
  }
}
