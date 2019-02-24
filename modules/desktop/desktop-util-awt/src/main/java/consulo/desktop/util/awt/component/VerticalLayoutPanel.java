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
package consulo.desktop.util.awt.component;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import javax.annotation.Nonnull;

import java.awt.*;

/**
 * @author VISTALL
 */
public class VerticalLayoutPanel extends JBPanel<VerticalLayoutPanel> {
  public VerticalLayoutPanel() {
    this(0, 0);
  }

  public VerticalLayoutPanel(int hgap, int vgap) {
    super(new VerticalFlowLayout(JBUI.scale(hgap), JBUI.scale(vgap)));
  }

  @Nonnull
  public VerticalLayoutPanel addComponent(Component comp) {
    add(comp);
    return this;
  }
}
