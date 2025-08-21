package consulo.task.impl.internal.timeTracking;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.awt.internal.GuiUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

/**
 * @author Evgeny.Zakrevsky
 * @since 2012-11-19
 */
@ExtensionImpl
public class TimeTrackingConfigurable implements SearchableConfigurable, Configurable.NoScroll, ProjectConfigurable, NonDefaultProjectConfigurable {
  private JCheckBox myEnableTimeTrackingCheckBox;
  private JTextField myTimeTrackingSuspendDelay;
  private JPanel myTimeTrackingSettings;
  private JPanel myPanel;
  private Project myProject;

  @Inject
  public TimeTrackingConfigurable(Project project) {
    myProject = project;
    myEnableTimeTrackingCheckBox.addActionListener(e -> enableTimeTrackingPanel());
  }

  private void enableTimeTrackingPanel() {
    GuiUtils.enableChildren(myTimeTrackingSettings, myEnableTimeTrackingCheckBox.isSelected());
  }

  private TimeTrackingManager.Config getConfig() {
    return TimeTrackingManager.getInstance(myProject).getState();
  }

  @Override
  public void reset() {
    myEnableTimeTrackingCheckBox.setSelected(getConfig().enabled);
    myTimeTrackingSuspendDelay.setText(String.valueOf(getConfig().suspendDelayInSeconds));
    enableTimeTrackingPanel();
  }

  @Override
  public void disposeUIResources() {
  }


  @Override
  public boolean isModified() {
    return myEnableTimeTrackingCheckBox.isSelected() != getConfig().enabled || !myTimeTrackingSuspendDelay.getText().equals(String.valueOf(getConfig().suspendDelayInSeconds));
  }

  @Override
  public void apply() {
    boolean oldTimeTrackingEnabled = getConfig().enabled;
    getConfig().enabled = myEnableTimeTrackingCheckBox.isSelected();
    if (getConfig().enabled != oldTimeTrackingEnabled) {
      TimeTrackingManager.getInstance(myProject).updateTimeTrackingToolWindow();
    }
    try {
      getConfig().suspendDelayInSeconds = Integer.parseInt(myTimeTrackingSuspendDelay.getText());
    }
    catch (NumberFormatException ignored) {
    }
  }

  @Nonnull
  @Override
  public String getId() {
    return "tasks.timeTracking";
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Nls
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Time Tracking");
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.TASKS_GROUP;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myPanel;
  }
}
