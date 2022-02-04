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

import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.editor.impl.DesktopEditorMarkupModelImpl;
import com.intellij.ui.components.JBLayeredPane;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-06-19
 */
public class DesktopEditorLayeredPanel {
  private final DesktopEditorImpl myEditor;

  private final JBLayeredPane myLayeredPane;

  private int myLayerPosition = JLayeredPane.POPUP_LAYER;

  public DesktopEditorLayeredPanel(DesktopEditorImpl editor) {
    myEditor = editor;
    myLayeredPane = new JBLayeredPane() {
      @Override
      public void doLayout() {
        Rectangle bounds = getBounds();

        int alignPadding = getAlignPadding();

        for (Component component : getComponents()) {
          if (component instanceof DesktopEditorPanelLayer) {
            DesktopEditorPanelLayer layer = (DesktopEditorPanelLayer)component;

            Dimension preferredSize = component.getPreferredSize();

            component.setBounds(bounds.width - preferredSize.width - alignPadding, layer.getPositionYInLayer(), preferredSize.width, preferredSize.height);
          }
          else {
            component.setBounds(0, 0, bounds.width, bounds.height);
          }
        }
      }

      @Override
      public Dimension getPreferredSize() {
        return myEditor.getScrollPane().getPreferredSize();
      }
    };
  }

  private int getAlignPadding() {
    JScrollBar bar = myEditor.getVerticalScrollBar();

    int padding = bar.getWidth();
    DesktopEditorMarkupModelImpl markupModel = (DesktopEditorMarkupModelImpl)myEditor.getMarkupModel();
    DesktopEditorErrorPanel errorPanel = markupModel.getErrorPanel();
    if (errorPanel != null) {
      padding += errorPanel.getWidth();
    }
    return padding;
  }

  public void addLayerPanel(JComponent panel) {
    myLayeredPane.add(panel, new Integer(myLayerPosition ++));
  }

  public void setMainPanel(JComponent rootPanel) {
    myLayeredPane.add(rootPanel, JBLayeredPane.DEFAULT_LAYER);
  }

  @Nonnull
  public JComponent getPanel() {
    return myLayeredPane;
  }
}
