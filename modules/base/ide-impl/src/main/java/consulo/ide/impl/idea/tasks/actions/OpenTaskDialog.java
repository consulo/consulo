/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package consulo.ide.impl.idea.tasks.actions;

import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.logging.Logger;
import consulo.ide.impl.idea.openapi.options.binding.BindControl;
import consulo.ide.impl.idea.openapi.options.binding.ControlBinder;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.ide.impl.idea.tasks.impl.TaskStateCombo;
import consulo.task.util.TaskUtil;
import consulo.project.Project;
import consulo.task.*;
import consulo.task.ui.TaskDialogPanel;
import consulo.task.ui.TaskDialogPanelProvider;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class OpenTaskDialog extends DialogWrapper
{
	private final static Logger LOG = Logger.getInstance(OpenTaskDialog.class);
	private static final String UPDATE_STATE_ENABLED = "tasks.open.task.update.state.enabled";

	private JPanel myPanel;
	@BindControl(value = "clearContext", instant = true)
	private JCheckBox myClearContext;
	private JLabel myTaskNameLabel;
	private JBCheckBox myUpdateState;
	private TaskStateCombo myTaskStateCombo;
	private JPanel myAdditionalPanel;

	private final Project myProject;
	private final Task myTask;
	private final List<TaskDialogPanel> myPanels;

	public OpenTaskDialog(@Nonnull final Project project, @Nonnull final Task task)
	{
		super(project, false);
		myProject = project;
		myTask = task;
		setTitle("Open Task");
		myTaskNameLabel.setText(TaskUtil.getTrimmedSummary(task));
		myTaskNameLabel.setIcon(TargetAWT.to(task.getIcon()));

		TaskManagerImpl taskManager = (TaskManagerImpl) TaskManager.getManager(myProject);
		ControlBinder binder = new ControlBinder(taskManager.getState());
		binder.bindAnnotations(this);
		binder.reset();

		if(!TaskStateCombo.stateUpdatesSupportedFor(task))
		{
			myUpdateState.setVisible(false);
			myTaskStateCombo.setVisible(false);
		}
		final boolean stateUpdatesEnabled = PropertiesComponent.getInstance(project).getBoolean(UPDATE_STATE_ENABLED, false);
		myUpdateState.setSelected(stateUpdatesEnabled);
		myUpdateState.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final boolean selected = myUpdateState.isSelected();
				PropertiesComponent.getInstance(project).setValue(UPDATE_STATE_ENABLED, String.valueOf(selected));
				updateFields();
				if(selected)
				{
					myTaskStateCombo.scheduleUpdateOnce();
				}
			}
		});

		TaskManagerImpl.Config state = taskManager.getState();
		myClearContext.setSelected(state.clearContext);

		updateFields();
		if(myUpdateState.isSelected())
		{
			myTaskStateCombo.scheduleUpdateOnce();
		}

		myAdditionalPanel.setLayout(new BoxLayout(myAdditionalPanel, BoxLayout.Y_AXIS));
		myPanels = TaskDialogPanelProvider.getOpenTaskPanels(project, task);
		for(TaskDialogPanel panel : myPanels)
		{
			myAdditionalPanel.add(panel.getPanel());
		}
		init();
	}

	private void updateFields()
	{
		myTaskStateCombo.setEnabled(myUpdateState.isSelected());
	}

	@Override
	protected void doOKAction()
	{
		createTask();
		super.doOKAction();
	}

	public void createTask()
	{
		final TaskManagerImpl taskManager = (TaskManagerImpl) TaskManager.getManager(myProject);

		if(myUpdateState.isSelected())
		{
			final CustomTaskState taskState = myTaskStateCombo.getSelectedState();
			final TaskRepository repository = myTask.getRepository();
			if(repository != null && taskState != null)
			{
				try
				{
					repository.setTaskState(myTask, taskState);
					repository.setPreferredOpenTaskState(taskState);
				}
				catch(Exception ex)
				{
					Messages.showErrorDialog(myProject, ex.getMessage(), "Cannot Set State For Issue");
					LOG.warn(ex);
				}
			}
		}
		taskManager.activateTask(myTask, isClearContext());
		if(myTask.getType() == TaskType.EXCEPTION && AnalyzeTaskStacktraceAction.hasTexts(myTask))
		{
			AnalyzeTaskStacktraceAction.analyzeStacktrace(myTask, myProject);
		}

		for(TaskDialogPanel panel : myPanels)
		{
			panel.commit();
		}
	}

	private boolean isClearContext()
	{
		return myClearContext.isSelected();
	}

	@NonNls
	protected String getDimensionServiceKey()
	{
		return "SimpleOpenTaskDialog";
	}

	@Override
	public JComponent getPreferredFocusedComponent()
	{
		for(TaskDialogPanel panel : myPanels)
		{
			final JComponent component = panel.getPreferredFocusedComponent();
			if(component != null)
			{
				return component;
			}
		}
		if(myTaskStateCombo.isVisible() && myTaskStateCombo.isEnabled())
		{
			return myTaskStateCombo.getComboBox();
		}
		return null;
	}

	@Nullable
	@Override
	protected ValidationInfo doValidate()
	{
		for(TaskDialogPanel panel : myPanels)
		{
			ValidationInfo validate = panel.validate();
			if(validate != null)
			{
				return validate;
			}
		}
		return null;
	}

	protected JComponent createCenterPanel()
	{
		return myPanel;
	}

	private void createUIComponents()
	{
		myTaskStateCombo = new TaskStateCombo(myProject, myTask)
		{
			@Nullable
			@Override
			protected CustomTaskState getPreferredState(@Nonnull TaskRepository repository, @Nonnull Collection<CustomTaskState> available)
			{
				return repository.getPreferredOpenTaskState();
			}
		};
	}
}
