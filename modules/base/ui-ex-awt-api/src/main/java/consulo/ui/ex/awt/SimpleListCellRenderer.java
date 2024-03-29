// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.awt.hacking.DefaultLookupHacking;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import java.awt.*;
import java.util.function.Function;

/**
 * JBLabel-based (text and icon) list cell renderer.
 *
 * @author gregsh
 * @see ColoredListCellRenderer for more complex SimpleColoredComponent-based variant.
 */
public abstract class SimpleListCellRenderer<T> extends JBLabel implements ListCellRenderer<T> {

  @Nonnull
  public static <T> SimpleListCellRenderer<T> create(@Nonnull String nullValue, @Nonnull Function<? super T, String> getText) {
    return new SimpleListCellRenderer<T>() {
      @Override
      public void customize(@Nonnull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
        setText(value == null ? nullValue : getText.apply(value));
      }
    };
  }

  @Nonnull
  public static <T> SimpleListCellRenderer<T> create(@Nonnull Customizer<? super T> customizer) {
    return new SimpleListCellRenderer<T>() {
      @Override
      public void customize(@Nonnull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
        customizer.customize(this, value, index);
      }
    };
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
    setComponentOrientation(list.getComponentOrientation());
    setBorder(JBUI.Borders.empty(1));
    Color bg, fg;
    JList.DropLocation dropLocation = list.getDropLocation();
    if (dropLocation != null && !dropLocation.isInsert() && dropLocation.getIndex() == index) {
      bg = DefaultLookupHacking.getColor(this, ui, "List.dropCellBackground");
      fg = DefaultLookupHacking.getColor(this, ui, "List.dropCellForeground");
      isSelected = true;
    }
    else {
      bg = isSelected ? list.getSelectionBackground() : list.getBackground();
      fg = isSelected ? list.getSelectionForeground() : list.getForeground();
    }
    setBackground(bg);
    setForeground(fg);
    setFont(list.getFont());
    setText("");
    setIcon((Icon)null);
    customize(list, value, index, isSelected, cellHasFocus);
    setOpaque(isSelected);
    return this;
  }

  public abstract void customize(@Nonnull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus);

  @Override
  public Dimension getPreferredSize() {
    if (StringUtil.isNotEmpty(getText())) {
      return super.getPreferredSize();
    }
    setText(" ");
    Dimension size = super.getPreferredSize();
    setText("");
    return size;
  }

  @FunctionalInterface
  public interface Customizer<T> {
    void customize(@Nonnull JBLabel label, T value, int index);
  }

  // @formatter:off
  @Override
  public void validate() {
  }

  @Override
  public void invalidate() {
  }

  @Override
  public void repaint() {
  }

  @Override
  public void revalidate() {
  }

  @Override
  public void repaint(long tm, int x, int y, int width, int height) {
  }

  @Override
  public void repaint(Rectangle r) {
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
    if (propertyName == "text" || ((propertyName == "font" || propertyName == "foreground") && oldValue != newValue && getClientProperty(BasicHTML.propertyKey) != null)) {
      super.firePropertyChange(propertyName, oldValue, newValue);
    }
  }
  // @formatter:on
}
