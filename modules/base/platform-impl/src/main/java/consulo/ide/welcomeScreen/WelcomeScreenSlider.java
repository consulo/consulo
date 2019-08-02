package consulo.ide.welcomeScreen;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-08-02
 */
public interface WelcomeScreenSlider {
  void setTitle(@Nonnull String title);

  void removeSlide(@Nonnull JComponent target);
}
