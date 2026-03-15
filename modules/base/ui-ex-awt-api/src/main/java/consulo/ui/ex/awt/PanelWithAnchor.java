package consulo.ui.ex.awt;

import org.jspecify.annotations.Nullable;
import javax.swing.*;

/**
 * @author evgeny.zakrevsky
 */
public interface PanelWithAnchor {
  JComponent getAnchor();

  void setAnchor(@Nullable JComponent anchor);
}
