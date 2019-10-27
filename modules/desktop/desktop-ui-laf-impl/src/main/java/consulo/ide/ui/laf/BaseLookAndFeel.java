package consulo.ide.ui.laf;

import com.intellij.ide.ui.laf.LafManagerImplUtil;
import consulo.desktop.util.awt.laf.BuildInLookAndFeel;

import javax.annotation.Nonnull;
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
