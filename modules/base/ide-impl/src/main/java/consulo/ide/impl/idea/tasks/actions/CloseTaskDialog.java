/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.ide.impl.idea.tasks.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.task.CustomTaskState;
import consulo.task.LocalTask;
import consulo.task.TaskRepository;
import consulo.ide.impl.idea.tasks.impl.TaskStateCombo;
import consulo.task.util.TaskUtil;
import consulo.task.ui.TaskDialogPanel;
import consulo.task.ui.TaskDialogPanelProvider;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awtUnsafe.TargetAWT;

/**
 * @author Dmitry Avdeev
 */
public class CloseTaskDialog extends DialogWrapper
{
	private static final String UPDATE_STATE_ENABLED = "tasks.close.task.update.state.enabled";

	private final Project myProject;
	private final LocalTask myTask;
	private final List<TaskDialogPanel> myPanels;

	private JPanel myPanel;
	private JLabel myTaskLabel;
	private TaskStateCombo myStateCombo;
	private JBCheckBox myUpdateState;
	private JPanel myAdditionalPanel;

	public CloseTaskDialog(Project project, final LocalTask task)
	{
		super(project, false);
		myProject = project;
		myTask = task;

		setTitle("Close Task");
		myTaskLabel.setText(TaskUtil.getTrimmedSummary(task));
		myTaskLabel.setIcon(TargetAWT.to(task.getIcon()));

		if(!TaskStateCombo.stateUpdatesSupportedFor(task))
		{
			myUpdateState.setVisible(false);
			myStateCombo.setVisible(false);
		}

		final boolean stateUpdatesEnabled = PropertiesComponent.getInstance(myProject).getBoolean(UPDATE_STATE_ENABLED);
		myUpdateState.setSelected(stateUpdatesEnabled);
		myStateCombo.setEnabled(stateUpdatesEnabled);
		myUpdateState.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final boolean selected = myUpdateState.isSelected();
				myStateCombo.setEnabled(selected);
				PropertiesComponent.getInstance(myProject).setValue(UPDATE_STATE_ENABLED, String.valueOf(selected));
				if(selected)
				{
					myStateCombo.scheduleUpdateOnce();
				}
			}
		});

		myStateCombo.showHintLabel(false);
		if(myUpdateState.isSelected())
		{
			myStateCombo.scheduleUpdateOnce();
		}

		myAdditionalPanel.setLayout(new BoxLayout(myAdditionalPanel, BoxLayout.Y_AXIS));
		myPanels = TaskDialogPanelProvider.getCloseTaskPanels(project, task);
		for(TaskDialogPanel panel : myPanels)
		{
			myAdditionalPanel.add(panel.getPanel());
		}

		init();
	}

	@Override
	protected void doOKAction()
	{
		for(TaskDialogPanel panel : myPanels)
		{
			panel.commit();
		}
		super.doOKAction();
	}

	protected JComponent createCenterPanel()
	{
		return myPanel;
	}

	@Nullable
	@Override
	public JComponent getPreferredFocusedComponent()
	{
		return myStateCombo.isVisible() && myUpdateState.isSelected() ? myStateCombo.getComboBox() : null;
	}

	@Nullable
	CustomTaskState getCloseIssueState()
	{
		return myUpdateState.isSelected() ? myStateCombo.getSelectedState() : null;
	}

	private void createUIComponents()
	{
		myStateCombo = new TaskStateCombo(myProject, myTask)
		{
			@Nullable
			@Override
			protected CustomTaskState getPreferredState(@Nonnull TaskRepository repository, @Nonnull Collection<CustomTaskState> available)
			{
				return repository.getPreferredCloseTaskState();
			}
		};
	}
}
