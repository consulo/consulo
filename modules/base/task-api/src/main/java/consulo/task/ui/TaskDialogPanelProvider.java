/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.task.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TaskDialogPanelProvider
{
	private final static ExtensionPointName<TaskDialogPanelProvider> EP_NAME = ExtensionPointName.create(TaskDialogPanelProvider.class);

	public static List<TaskDialogPanel> getOpenTaskPanels(@Nonnull Project project, @Nonnull Task task)
	{
		return ContainerUtil.mapNotNull(EP_NAME.getExtensionList(), provider -> provider.getOpenTaskPanel(project, task));
	}

	public static List<TaskDialogPanel> getCloseTaskPanels(@Nonnull Project project, @Nonnull LocalTask task)
	{
		return ContainerUtil.mapNotNull(EP_NAME.getExtensionList(), provider -> provider.getCloseTaskPanel(project, task));
	}

	@Nullable
	public abstract TaskDialogPanel getOpenTaskPanel(@Nonnull Project project, @Nonnull Task task);

	@Nullable
	public abstract TaskDialogPanel getCloseTaskPanel(@Nonnull Project project, @Nonnull LocalTask task);
}
