// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.uiOld.components.fields;

import consulo.ui.ex.awt.JBScrollBar;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.desktop.awt.uiOld.Expandable;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.function.Function;

import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

public class IntelliJExpandableSupport<Source extends JTextComponent> extends ExpandableSupport<Source> {
  public IntelliJExpandableSupport(@Nonnull Source source, Function<? super String, String> onShow, Function<? super String, String> onHide) {
    super(source, onShow, onHide);
  }

  @Nonnull
  @Override
  protected Content prepare(@Nonnull Source field, @Nonnull Function<? super String, String> onShow) {
    Font font = field.getFont();
    FontMetrics metrics = font == null ? null : field.getFontMetrics(font);
    int height = metrics == null ? 16 : metrics.getHeight();
    Dimension size = new Dimension(height * 32, height * 16);

    JTextArea area = new JTextArea(onShow.apply(field.getText()));
    area.putClientProperty(Expandable.class, this);
    area.setEditable(field.isEditable());
    area.setBackground(field.getBackground());
    area.setForeground(field.getForeground());
    area.setFont(font);
    area.setWrapStyleWord(true);
    area.setLineWrap(true);
    copyCaretPosition(field, area);
    UIUtil.addUndoRedoActions(area);

    JLabel label = ExpandableSupport.createLabel(createCollapseExtension());
    label.setBorder(JBUI.Borders.empty(5, 0, 5, 5));

    JBScrollPane pane = new JBScrollPane(area);
    pane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
    pane.getVerticalScrollBar().add(JBScrollBar.LEADING, label);
    pane.getVerticalScrollBar().setBackground(area.getBackground());

    Insets insets = field.getInsets();
    Insets margin = field.getMargin();
    if (margin != null) {
      insets.top += margin.top;
      insets.left += margin.left;
      insets.right += margin.right;
      insets.bottom += margin.bottom;
    }

    JBInsets.addTo(size, insets);
    JBInsets.addTo(size, pane.getInsets());
    pane.setPreferredSize(size);
    pane.setViewportBorder(insets != null ? createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right) : createEmptyBorder());
    return new Content() {
      @Nonnull
      @Override
      public JComponent getContentComponent() {
        return pane;
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
