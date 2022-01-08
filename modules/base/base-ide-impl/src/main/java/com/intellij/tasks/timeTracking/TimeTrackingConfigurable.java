package com.intellij.tasks.timeTracking;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: Evgeny.Zakrevsky
 * Date: 11/19/12
 */
public class TimeTrackingConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private JCheckBox myEnableTimeTrackingCheckBox;
  private JTextField myTimeTrackingSuspendDelay;
  private JPanel myTimeTrackingSettings;
  private JPanel myPanel;
  private Project myProject;

  @Inject
  public TimeTrackingConfigurable(Project project) {
    myProject = project;
    myEnableTimeTrackingCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        enableTimeTrackingPanel();
      }
    });
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
    return myEnableTimeTrackingCheckBox.isSelected() != getConfig().enabled ||
           !myTimeTrackingSuspendDelay.getText().equals(String.valueOf(getConfig().suspendDelayInSeconds));
  }

  @Override
  public void apply() {
    boolean oldTimeTrackingEnabled = getConfig().enabled;
    getConfig().enabled = myEnableTimeTrackingCheckBox.isSelected();
    if (getConfig().enabled != oldTimeTrackingEnabled) {
      TimeTrackingManager.getInstance(myProject).updateTimeTrackingToolWindow();
    }
    try{
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
  public Runnable enableSearch(final String option) {
    return null;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Time Tracking";
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myPanel;
  }
}
