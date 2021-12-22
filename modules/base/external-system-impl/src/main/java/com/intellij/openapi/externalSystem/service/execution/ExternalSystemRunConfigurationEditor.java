package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
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
