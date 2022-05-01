package consulo.ide.impl.idea.ide.passwordSafe.config;

import consulo.ide.impl.idea.ide.passwordSafe.PasswordSafe;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * A configurable for password safe
 */
public class PasswordSafeConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  /**
   * The settings for the password safe
   */
  final PasswordSafeSettings mySettings;
  /**
   * The password safe service
   */
  private final PasswordSafe myPasswordSafe;
  /**
   * The option panel to use
   */
  PasswordSafeOptionsPanel myPanel = null;

  /**
   * The constructor
   *
   * @param settings the password safe settings
   */
  @Inject
  public PasswordSafeConfigurable(@Nonnull PasswordSafeSettings settings, @Nonnull PasswordSafe passwordSafe) {
    mySettings = settings;
    myPasswordSafe = passwordSafe;
  }

  /**
   * {@inheritDoc}
   */
  @Nls
  public String getDisplayName() {
    return "Passwords";
  }

  /**
   * {@inheritDoc}
   */
  public JComponent createComponent() {
    myPanel = new PasswordSafeOptionsPanel(myPasswordSafe);
    myPanel.load(mySettings);
    return myPanel.getRoot();  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * {@inheritDoc}
   */
  public boolean isModified() {
    return myPanel.isModified(mySettings);  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * {@inheritDoc}
   */
  public void apply() throws ConfigurationException {
    myPanel.save(mySettings);
  }

  /**
   * {@inheritDoc}
   */
  public void reset() {
    myPanel.load(mySettings);
  }

  /**
   * {@inheritDoc}
   */
  public void disposeUIResources() {
    myPanel = null;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  public String getId() {
    return "application.passwordSafe";
  }

  /**
   * {@inheritDoc}
   */
  public Runnable enableSearch(String option) {
    return null;
  }
}
