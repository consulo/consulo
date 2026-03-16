// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.execution.executor.Executor;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.impl.internal.action.ChooseRunConfigurationPopup;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingRunConfigurationItem;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil;
import consulo.ide.runAnything.RunAnythingItem;
import consulo.dataContext.DataContext;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.runAnything.RunAnythingProviderBase;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

import static consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY;
import static consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil.fetchProject;

public abstract class RunAnythingRunConfigurationProvider extends RunAnythingProviderBase<ChooseRunConfigurationPopup.ItemWrapper> {
    
    @Override
    public String getCommand(ChooseRunConfigurationPopup.ItemWrapper value) {
        return value.getText();
    }

    @Override
    public void execute(DataContext dataContext, ChooseRunConfigurationPopup.ItemWrapper wrapper) {
        Executor executor = dataContext.getData(EXECUTOR_KEY);
        assert executor != null;

        if (wrapper.getValue() instanceof RunnerAndConfigurationSettings configurationSettings
            && !RunManager.getInstance(fetchProject(dataContext)).hasSettings(configurationSettings)) {
            RunManager.getInstance(fetchProject(dataContext)).addConfiguration(configurationSettings);
        }

        wrapper.perform(fetchProject(dataContext), executor, dataContext);
    }

    @Override
    public Image getIcon(ChooseRunConfigurationPopup.@Nullable ItemWrapper value) {
        return value.getIcon();
    }

    @Nullable
    @Override
    public String getAdText() {
        return IdeLocalize.runAnythingAdRunInContext(RunAnythingUtil.PRESSED_ALT) + ", " +
            IdeLocalize.runAnythingAdRunWithDebug(RunAnythingUtil.SHIFT_SHORTCUT_TEXT);
    }

    
    @Override
    public RunAnythingItem getMainListItem(DataContext dataContext, ChooseRunConfigurationPopup.ItemWrapper value) {
        return new RunAnythingRunConfigurationItem(value, value.getIcon());
    }
}