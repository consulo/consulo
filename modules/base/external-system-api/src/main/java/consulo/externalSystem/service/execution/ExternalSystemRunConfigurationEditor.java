package consulo.externalSystem.service.execution;

import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.ui.awt.PaintAwarePanel;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 * @since 23.05.13 18:34
 */
public class ExternalSystemRunConfigurationEditor extends SettingsEditor<ExternalSystemRunConfiguration> {

  @Nonnull
  private final ExternalSystemTaskSettingsControl myControl;

  public ExternalSystemRunConfigurationEditor(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    myControl = new ExternalSystemTaskSettingsControl(project, externalSystemId);
  }

  @Override
  protected void resetEditorFrom(ExternalSystemRunConfiguration s) {
    myControl.setOriginalSettings(s.getSettings());
    myControl.reset();
  }

  @Override
  protected void applyEditorTo(ExternalSystemRunConfiguration s) throws ConfigurationException {
    myControl.apply(s.getSettings());
  }

  @Nonnull
  @Override
  protected JComponent createEditor() {
    PaintAwarePanel result = new PaintAwarePanel(new GridBagLayout());
    myControl.fillUi(this, result, 0);
    return result;
  }

  @Override
  protected void disposeEditor() {
    myControl.disposeUIResources();
  }
}
