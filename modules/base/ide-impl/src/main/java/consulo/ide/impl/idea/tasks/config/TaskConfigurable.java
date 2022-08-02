package consulo.ide.impl.idea.tasks.config;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.ide.impl.idea.openapi.options.binding.BindControl;
import consulo.ide.impl.idea.openapi.options.binding.BindableConfigurable;
import consulo.ide.impl.idea.openapi.options.binding.ControlBinder;
import consulo.ide.impl.idea.openapi.util.NotNullLazyValue;
import consulo.task.TaskManager;
import consulo.ide.impl.idea.tasks.impl.TaskManagerImpl;
import consulo.project.Project;
import consulo.task.TaskSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.internal.GuiUtils;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class TaskConfigurable extends BindableConfigurable implements Configurable.NoScroll, ProjectConfigurable, NonDefaultProjectConfigurable {

  private JPanel myPanel;

  @BindControl("updateEnabled")
  private JCheckBox myUpdateCheckBox;

  @BindControl("updateIssuesCount")
  private JTextField myUpdateCount;

  @BindControl("updateInterval")
  private JTextField myUpdateInterval;

  @BindControl("taskHistoryLength")
  private JTextField myHistoryLength;
  private JPanel myCacheSettings;

  @BindControl("saveContextOnCommit")
  private JCheckBox mySaveContextOnCommit;

  @BindControl("changelistNameFormat")
  private JTextField myChangelistNameFormat;
  private JBCheckBox myAlwaysDisplayTaskCombo;
  private JTextField myConnectionTimeout;

  private final Project myProject;
  private final NotNullLazyValue<ControlBinder> myControlBinder = new NotNullLazyValue<ControlBinder>() {
    @Nonnull
    @Override
    protected ControlBinder compute() {
      return new ControlBinder(getConfig());
    }
  };

  @Inject
  public TaskConfigurable(Project project) {
    super();
    myProject = project;
    myUpdateCheckBox.addActionListener(e -> enableCachePanel());
  }

  private TaskManagerImpl.Config getConfig() {
    return ((TaskManagerImpl)TaskManager.getManager(myProject)).getState();
  }

  @Override
  protected ControlBinder getBinder() {
    return myControlBinder.getValue();
  }

  private void enableCachePanel() {
    GuiUtils.enableChildren(myCacheSettings, myUpdateCheckBox.isSelected());
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    super.reset();
    enableCachePanel();
    myAlwaysDisplayTaskCombo.setSelected(TaskSettings.getInstance().ALWAYS_DISPLAY_COMBO);
    myConnectionTimeout.setText(Integer.toString(TaskSettings.getInstance().CONNECTION_TIMEOUT));
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    boolean oldUpdateEnabled = getConfig().updateEnabled;
    super.apply();
    if (getConfig().updateEnabled && !oldUpdateEnabled) {
      TaskManager.getManager(myProject).updateIssues(null);
    }
    TaskSettings.getInstance().ALWAYS_DISPLAY_COMBO = myAlwaysDisplayTaskCombo.isSelected();
    TaskSettings.getInstance().CONNECTION_TIMEOUT = Integer.valueOf(myConnectionTimeout.getText());
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return super.isModified() ||
           TaskSettings.getInstance().ALWAYS_DISPLAY_COMBO != myAlwaysDisplayTaskCombo.isSelected() ||
           TaskSettings.getInstance().CONNECTION_TIMEOUT != Integer.valueOf(myConnectionTimeout.getText());
  }

  @Nonnull
  @Override
  @Nls
  public String getDisplayName() {
    return "Tasks";
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    bindAnnotations();
    return myPanel;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
  }

  @Override
  @Nonnull
  public String getId() {
    return StandardConfigurableIds.TASKS_GROUP;
  }
}
