package consulo.ui.ex.awt;

import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author evgeny.zakrevsky
 */
public class JBCheckBox extends JCheckBox implements AnchorableComponent {
  private JComponent myAnchor;

  public JBCheckBox() {
    this(null);
  }

  public JBCheckBox(@Nullable String text) {
    this(text, false);
  }

  public JBCheckBox(@Nullable String text, boolean selected) {
    super(text, null, selected);
  }

  @Override
  public JComponent getAnchor() {
    return myAnchor;
  }

  @Override
  public void setAnchor(@Nullable JComponent anchor) {
    this.myAnchor = anchor;
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    if (myAnchor != null && myAnchor != this) {
      Dimension anchorSize = myAnchor.getPreferredSize();
      size.width = Math.max(size.width, anchorSize.width);
      size.height = Math.max(size.height, anchorSize.height);
    }                                                                                
    return size;
  }

  @Override
  public Dimension getMinimumSize() {
    Dimension size = super.getMinimumSize();
    if (myAnchor != null && myAnchor != this) {
      Dimension anchorSize = myAnchor.getMinimumSize();
      size.width = Math.max(size.width, anchorSize.width);
      size.height = Math.max(size.height, anchorSize.height);
    }
    return size;
  }
}
