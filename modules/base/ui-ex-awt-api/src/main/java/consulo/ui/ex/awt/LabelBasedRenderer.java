// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class LabelBasedRenderer extends JLabel {

  public static class List<E> extends LabelBasedRenderer implements ListCellRenderer<E> {
    private static final Border EMPTY = JBUI.Borders.empty(1); // see DefaultListCellRenderer.getNoFocusBorder

    @Nonnull
    @Override
    public Component getListCellRendererComponent(@Nonnull JList<? extends E> list, @Nullable E value, int index, boolean selected, boolean focused) {
      configure(list, value);
      setForeground(UIUtil.getListForeground(selected, focused));
      setBackground(UIUtil.getListBackground(selected, focused));
      setBorder(EMPTY);
      return this;
    }
  }

  public static class Tree extends LabelBasedRenderer implements TreeCellRenderer {
    private static final Border EMPTY = JBUI.Borders.emptyRight(3); // see DefaultTreeCellRenderer.getPreferredSize

    @Nonnull
    @Override
    public Component getTreeCellRendererComponent(@Nonnull JTree tree, @Nullable Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focused) {
      configure(tree, tree.convertValueToText(value, selected, expanded, leaf, row, focused));
      setForeground(UIUtil.getTreeForeground(selected, focused));
      setBackground(selected ? UIUtil.getTreeSelectionBackground(focused) : UIUtil.getTreeBackground());
      setBorder(EMPTY);
      return this;
    }
  }

  void configure(@Nonnull Component component, @Nullable Object value) {
    setComponentOrientation(component.getComponentOrientation());
    setEnabled(component.isEnabled());
    setFont(component.getFont());
    //noinspection HardCodedStringLiteral
    setText(value == null ? "" : value.toString());
    setIcon(null);
  }

  // The following methods are overridden for performance reasons, see DefaultListCellRenderer and DefaultTreeCellRenderer

  @Override
  public void validate() {
  }

  @Override
  public void invalidate() {
  }

  @Override
  public void revalidate() {
  }

  @Override
  public void repaint() {
  }

  @Override
  public void repaint(Rectangle bounds) {
  }

  @Override
  public void repaint(long unused, int x, int y, int width, int height) {
  }

  @Override
  public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, char oldValue, char newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, short oldValue, short newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, int oldValue, int newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, long oldValue, long newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, float oldValue, float newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, double oldValue, double newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
  }

  @Override
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    if ("text".equals(propertyName) || (("font".equals(propertyName) || "foreground".equals(propertyName)) && oldValue != newValue && getClientProperty(BasicHTML.propertyKey) != null)) {

      super.firePropertyChange(propertyName, oldValue, newValue);
    }
  }
}
