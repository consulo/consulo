package consulo.desktop.awt.ui.plaf.darcula;

import consulo.ui.ex.awt.JBUI;

import javax.swing.plaf.BorderUIResource;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaMenuItemBorder extends BorderUIResource.EmptyBorderUIResource {
  public DarculaMenuItemBorder() {
    super(JBUI.insets(2));
  }
}
