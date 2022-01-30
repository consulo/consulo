package com.intellij.tasks.actions;

import com.intellij.icons.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.tasks.config.TaskRepositoriesConfigurable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * User: Evgeny Zakrevsky
 */

public class ConfigureServersAction extends BaseTaskAction
{
	public ConfigureServersAction()
	{
		super("Configure Servers...", null, AllIcons.General.Settings);
	}

	@RequiredUIAccess
	@Override
	public void actionPerformed(@Nonnull AnActionEvent e)
	{
      TaskRepositoriesConfigurable configurable = new TaskRepositoriesConfigurable(e.getData(CommonDataKeys.PROJECT));

		ShowSettingsUtil.getInstance().editConfigurable(getProject(e), configurable).doWhenDone(this::serversChanged);
	}

	protected void serversChanged()
	{

	}
}
