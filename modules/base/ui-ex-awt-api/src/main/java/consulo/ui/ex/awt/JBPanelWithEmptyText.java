/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.ui.ex.awt.util.JBSwingUtilities;

import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * @author gregsh
 */
public class JBPanelWithEmptyText extends JBPanel<JBPanelWithEmptyText> implements ComponentWithEmptyText {

  private final StatusText myEmptyText = new StatusText() {
    @Override
    protected boolean isStatusVisible() {
      return UIUtil.uiChildren(JBPanelWithEmptyText.this).filter(Component::isVisible).isEmpty();
    }
  };

  public JBPanelWithEmptyText() {
    super();
  }

  public JBPanelWithEmptyText(LayoutManager layout) {
    super(layout);
  }

  @Nonnull
  @Override
  public StatusText getEmptyText() {
    return myEmptyText;
  }

  @Nonnull
  public JBPanelWithEmptyText withEmptyText(String str) {
    myEmptyText.setText(str);
    return this;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    myEmptyText.paint(this, g);
  }

  @Override
  protected Graphics getComponentGraphics(Graphics graphics) {
    return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(graphics));
  }
}
