// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.execution.impl.internal.action.ChooseRunConfigurationPopup;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingRunConfigurationProvider;
import consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingCompletionGroup;
import consulo.ide.runAnything.RunAnythingContext;
import consulo.ide.runAnything.RunAnythingGroup;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil.fetchProject;

@ExtensionImpl
public class RunAnythingRunConfigurationProviderImpl extends RunAnythingRunConfigurationProvider {
    @Nonnull
    @Override
    public Collection<ChooseRunConfigurationPopup.ItemWrapper> getValues(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        return getWrappers(dataContext);
    }

    @Nullable
    @Override
    public String getHelpGroupTitle() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RunAnythingGroup getCompletionGroup() {
        return new RunAnythingCompletionGroup(this, IdeLocalize.runAnythingRunConfigurationsGroupTitle());
    }

    @Nonnull
    private static List<ChooseRunConfigurationPopup.ItemWrapper> getWrappers(@Nonnull DataContext dataContext) {
        Project project = fetchProject(dataContext);
        return ChooseRunConfigurationPopup.createFlatSettingsList(project);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public List<RunAnythingContext> getExecutionContexts(@Nonnull DataContext dataContext) {
        return Collections.emptyList();
    }
}