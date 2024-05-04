package consulo.ide.impl.wm.impl.status;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 16/08/2021
 */
public class UnifiedInfoAndProgressPanel implements Disposable {
  private DockLayout myLayout;

  private Label myStatusLabel;

  @RequiredUIAccess
  public UnifiedInfoAndProgressPanel() {
    myLayout = DockLayout.create();

    myStatusLabel = Label.create();

    myLayout.left(myStatusLabel);
  }

  @RequiredUIAccess
  public void setStatusText(String text) {
    myStatusLabel.setText(LocalizeValue.of(text));
  }

  @Nonnull
  public Component getUIComponent() {
    return myLayout;
  }

  @Override
  public void dispose() {

  }
}
