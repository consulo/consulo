package consulo.desktop.awt.ui.plaf.darcula;

import consulo.ui.ex.awt.JBUI;

import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaTableHeaderBorder implements Border, UIResource {

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return JBUI.emptyInsets();
  }

  @Override
  public boolean isBorderOpaque() {
    return false;
  }
}
