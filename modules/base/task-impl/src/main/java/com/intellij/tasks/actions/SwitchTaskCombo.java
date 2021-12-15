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

package com.intellij.tasks.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.config.TaskSettings;
import consulo.actionSystem.ex.ComboBoxButtonImpl;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class SwitchTaskCombo extends ComboBoxAction implements DumbAware
{
	@Nonnull
	public JComponent createCustomComponent(final Presentation presentation)
	{
		ComboBoxButtonImpl button = new ComboBoxButtonImpl(this, presentation);
		return button;
	}

	@Nonnull
	@Override
	public JBPopup createPopup(@Nonnull JComponent component, @Nonnull DataContext context, @Nonnull Runnable onDispose)
	{
		return SwitchTaskAction.createPopup(context, onDispose, false);
	}

	@RequiredUIAccess
	@Override
	public void update(@Nonnull AnActionEvent e)
	{
		Presentation presentation = e.getPresentation();
		Project project = e.getData(CommonDataKeys.PROJECT);
		ComboBoxButtonImpl button = (ComboBoxButtonImpl) presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY);
		if(project == null || project.isDefault() || project.isDisposed() || button == null)
		{
			presentation.setEnabled(false);
			presentation.setText("");
			presentation.setIcon(null);
		}
		else
		{
			TaskManager taskManager = TaskManager.getManager(project);
			LocalTask activeTask = taskManager.getActiveTask();
			presentation.setVisible(true);
			presentation.setEnabled(true);

			if(isImplicit(activeTask) && taskManager.getAllRepositories().length == 0 && !TaskSettings.getInstance().ALWAYS_DISPLAY_COMBO)
			{
				presentation.setVisible(false);
			}
			else
			{
				String s = getText(activeTask);
				presentation.setText(s);
				presentation.setIcon(activeTask.getIcon());
				presentation.setDescription(activeTask.getSummary());
			}
		}
	}

	@Nonnull
	@Override
	protected DefaultActionGroup createPopupActionGroup(JComponent button)
	{
		throw new IllegalArgumentException();
	}

	private static boolean isImplicit(LocalTask activeTask)
	{
		return activeTask.isDefault() && Comparing.equal(activeTask.getCreated(), activeTask.getUpdated());
	}

	private static String getText(LocalTask activeTask)
	{
		String text = activeTask.getPresentableName();
		return StringUtil.first(text, 50, true);
	}
}