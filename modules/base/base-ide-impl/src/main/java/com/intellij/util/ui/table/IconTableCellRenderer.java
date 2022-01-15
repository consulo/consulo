package com.intellij.util.ui.table;

import com.intellij.openapi.util.Iconable;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public abstract class IconTableCellRenderer<T> extends DefaultTableCellRenderer {
  public static final IconTableCellRenderer<Iconable> ICONABLE = new IconTableCellRenderer<Iconable>() {
    @Nullable
    @Override
    protected Image getIcon(@Nonnull Iconable value, JTable table, int row) {
      return value.getIcon(Iconable.ICON_FLAG_VISIBILITY);
    }
  };

  public static TableCellRenderer create(@Nonnull final Image icon) {
    return new IconTableCellRenderer() {
      @Nullable
      @Override
      protected Image getIcon(@Nonnull Object value, JTable table, int row) {
        return icon;
      }
    };
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
    super.getTableCellRendererComponent(table, value, selected, focus, row, column);
    //noinspection unchecked
    setIcon(value == null ? null : TargetAWT.to(getIcon((T)value, table, row)));
    if (isCenterAlignment()) {
      setHorizontalAlignment(CENTER);
      setVerticalAlignment(CENTER);
    }
    return this;
  }

  protected boolean isCenterAlignment() {
    return false;
  }

  @Nullable
  protected abstract Image getIcon(@Nonnull T value, JTable table, int row);
}