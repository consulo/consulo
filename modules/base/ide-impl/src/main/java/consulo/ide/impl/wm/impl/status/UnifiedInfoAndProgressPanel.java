package consulo.ide.impl.wm.impl.status;

import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 16/08/2021
 */
public class UnifiedInfoAndProgressPanel implements CustomStatusBarWidget {
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
  @Override
  public String ID() {
    return "InfoAndProgressPanel";
  }

  @Nullable
  @Override
  public WidgetPresentation getPresentation() {
    return null;
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {

  }

  @Override
  public void dispose() {

  }

  @Nullable
  @Override
  public Component getUIComponent() {
    return myLayout;
  }

  @Override
  public boolean isUnified() {
    return true;
  }
}
