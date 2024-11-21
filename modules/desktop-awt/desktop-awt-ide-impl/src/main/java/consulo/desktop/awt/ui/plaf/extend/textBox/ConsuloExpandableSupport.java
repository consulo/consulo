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
package consulo.desktop.awt.ui.plaf.extend.textBox;

import consulo.desktop.awt.uiOld.Expandable;
import consulo.desktop.awt.uiOld.components.fields.ExpandableSupport;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalFlowLayout;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-04-26
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

        JTextArea area = new JTextArea(onShow.apply(field.getText()));
        area.putClientProperty(Expandable.class, this);
        area.setEditable(field.isEditable());
        //area.setBackground(field.getBackground());
        //area.setForeground(field.getForeground());
        area.setFont(font);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        copyCaretPosition(field, area);
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
                    field.setText(onHide.apply(area.getText()));
                    copyCaretPosition(area, field);
                }
            }
        };
    }

    public static void copyCaretPosition(JTextComponent source, JTextComponent destination) {
        try {
            destination.setCaretPosition(source.getCaretPosition());
        }
        catch (Exception ignored) {
        }
    }
}
