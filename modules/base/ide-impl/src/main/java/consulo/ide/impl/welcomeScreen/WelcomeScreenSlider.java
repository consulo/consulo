package consulo.ide.impl.welcomeScreen;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.ui.ex.TitlelessDecorator;
import consulo.util.dataholder.Key;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-08-02
 */
@Deprecated
@DeprecationInfo("AWT version")
public interface WelcomeScreenSlider {
    Key<WelcomeScreenSlider> KEY = Key.create(WelcomeScreenSlider.class);

    void setTitle(String title);

    void removeSlide(JComponent target);

    TitlelessDecorator getTitlelessDecorator();

    Disposable getDisposable();
}
