/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.editor.impl;

import com.intellij.ui.components.panels.NonOpaquePanel;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-06-19
 */
public class StatusComponentContainer extends NonOpaquePanel implements DesktopEditorPanelLayer {
  public void setComponent(JComponent component) {
    add(component, BorderLayout.CENTER);
  }

  public JPanel getPanel() {
    return this;
  }

  @Override
  public int getPositionYInLayer() {
    return 0;
  }
}
