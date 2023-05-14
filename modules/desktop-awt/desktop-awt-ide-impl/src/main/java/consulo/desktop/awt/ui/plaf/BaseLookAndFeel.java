package consulo.desktop.awt.ui.plaf;

import consulo.ui.ex.awt.BuildInLookAndFeel;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;

/**
 * @author VISTALL
 * @since 2019-08-11
 */
public abstract class BaseLookAndFeel extends BasicLookAndFeel implements BuildInLookAndFeel {
  @Override
  public final UIDefaults getDefaults() {
    UIDefaults superDefaults = super.getDefaults();

    UIDefaults defaultsImpl = getDefaultsImpl(superDefaults);

    LafManagerImplUtil.insertCustomComponentUI(defaultsImpl);
    defaultsImpl.put("ClassLoader", getClass().getClassLoader());

    return defaultsImpl;
  }

  @Nonnull
  public UIDefaults getDefaultsImpl(UIDefaults superDefaults) {
    return superDefaults;
  }
}
