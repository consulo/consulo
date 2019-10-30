/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.laf.extend.textBox;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.Expandable;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.fields.ExpandableSupport;
import com.intellij.ui.components.fields.IntelliJExpandableSupport;
import com.intellij.util.Function;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-04-26
 *
 * This is another implementation of {@link ExpandableSupport}
 *
 * Inspiried by {@link IntelliJExpandableSupport} but there differents
 * - collapse button is not part scrollbar - it have old right panel
 */
public class ConsuloExpandableSupport<T extends JTextComponent> extends ExpandableSupport<T> {
  public ConsuloExpandableSupport(@Nonnull T jTextComponent, Function<? super String, String> onShow, Function<? super String, String> onHide) {
    super(jTextComponent, onShow, onHide);
  }

  @Nonnull
  @Override
  protected Content prepare(@Nonnull T field, @Nonnull Function<? super String, String> onShow) {
    Font font = field.getFont();
    FontMetrics metrics = font == null ? null : field.getFontMetrics(font);
    int height = metrics == null ? 16 : metrics.getHeight();
    Dimension size = new Dimension(field.getWidth(), height * 16);

    JPanel mainPanel = new JPanel(new BorderLayout());

    JTextArea area = new JTextArea(onShow.fun(field.getText()));
    area.putClientProperty(Expandable.class, this);
    area.setEditable(field.isEditable());
    //area.setBackground(field.getBackground());
    //area.setForeground(field.getForeground());
    area.setFont(font);
    area.setWrapStyleWord(true);
    area.setLineWrap(true);
    IntelliJExpandableSupport.copyCaretPosition(field, area);
    UIUtil.addUndoRedoActions(area);

    JLabel label = ExpandableSupport.createLabel(createCollapseExtension());
    label.setBorder(JBUI.Borders.empty(5));

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(area, true);

    mainPanel.add(scrollPane, BorderLayout.CENTER);

    JPanel eastPanel = new JPanel(new VerticalFlowLayout(0, 0));
    eastPanel.add(label);
    mainPanel.add(eastPanel, BorderLayout.EAST);

    scrollPane.setPreferredSize(size);

    return new Content() {
      @Nonnull
      @Override
      public JComponent getContentComponent() {
        return mainPanel;
      }

      @Override
      public JComponent getFocusableComponent() {
        return area;
      }

      @Override
      public void cancel(@Nonnull Function<? super String, String> onHide) {
        if (field.isEditable()) {
          field.setText(onHide.fun(area.getText()));
          IntelliJExpandableSupport.copyCaretPosition(area, field);
        }
      }
    };
  }
}
