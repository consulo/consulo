package consulo.ide.impl.welcomeScreen;

import consulo.disposer.Disposable;
import consulo.ui.ex.awt.TitlelessDecorator;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-08-02
 */
public interface WelcomeScreenSlider {
    Key<WelcomeScreenSlider> KEY = Key.create(WelcomeScreenSlider.class);

    void setTitle(@Nonnull String title);

    void removeSlide(@Nonnull JComponent target);

    TitlelessDecorator getTitlelessDecorator();

    Disposable getDisposable();
}
