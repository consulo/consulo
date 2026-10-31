// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.execution.localize.ExecutionLocalize;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.execution.impl.internal.action.ChooseRunConfigurationPopup;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingRunConfigurationProvider;
import consulo.execution.runAnything.RunAnythingCompletionGroup;
import consulo.execution.runAnything.RunAnythingContext;
import consulo.execution.runAnything.RunAnythingGroup;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil.fetchProject;

@ExtensionImpl
public class RunAnythingRunConfigurationProviderImpl extends RunAnythingRunConfigurationProvider {
    
    @Override
    public Collection<ChooseRunConfigurationPopup.ItemWrapper> getValues(DataContext dataContext, String pattern) {
        return getWrappers(dataContext);
    }

    @Override
    public @Nullable String getHelpGroupTitle() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RunAnythingGroup getCompletionGroup() {
        return new RunAnythingCompletionGroup(this, ExecutionLocalize.runAnythingRunConfigurationsGroupTitle());
    }

    
    private static List<ChooseRunConfigurationPopup.ItemWrapper> getWrappers(DataContext dataContext) {
        Project project = fetchProject(dataContext);
        return ChooseRunConfigurationPopup.createFlatSettingsList(project);
    }

    
    @Override
    @RequiredReadAction
    public List<RunAnythingContext> getExecutionContexts(DataContext dataContext) {
        return Collections.emptyList();
    }
}