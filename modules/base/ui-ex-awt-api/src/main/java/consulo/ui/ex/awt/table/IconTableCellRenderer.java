package consulo.ui.ex.awt.table;

import consulo.component.util.Iconable;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public abstract class IconTableCellRenderer<T> extends DefaultTableCellRenderer {
  public static final IconTableCellRenderer<Iconable> ICONABLE = new IconTableCellRenderer<Iconable>() {
    @Nullable
    @Override
    protected Image getIcon(Iconable value, JTable table, int row) {
      return value.getIcon(Iconable.ICON_FLAG_VISIBILITY);
    }
  };

  public static TableCellRenderer create(final Image icon) {
    return new IconTableCellRenderer() {
      @Nullable
      @Override
      protected Image getIcon(Object value, JTable table, int row) {
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
  protected abstract Image getIcon(T value, JTable table, int row);
}