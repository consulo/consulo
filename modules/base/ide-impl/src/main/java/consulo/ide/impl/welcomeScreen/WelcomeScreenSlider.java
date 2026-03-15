package consulo.ide.impl.welcomeScreen;

import consulo.disposer.Disposable;
import consulo.ui.ex.awt.TitlelessDecorator;
import consulo.util.dataholder.Key;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-08-02
 */
public interface WelcomeScreenSlider {
    Key<WelcomeScreenSlider> KEY = Key.create(WelcomeScreenSlider.class);

    void setTitle(String title);

    void removeSlide(JComponent target);

    TitlelessDecorator getTitlelessDecorator();

    Disposable getDisposable();
}
