// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ide.runAnything.RunAnythingContext;
import consulo.ide.runAnything.RunAnythingCompletionGroup;
import consulo.ide.runAnything.RunAnythingGroup;
import consulo.ide.runAnything.RunAnythingItem;
import consulo.ide.runAnything.RunAnythingItemBase;
import consulo.ide.localize.IdeLocalize;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ExtensionImpl
public class RunAnythingRecentProjectProvider extends RunAnythingAnActionProvider<AnAction> {
    
    @Override
    public Collection<AnAction> getValues(DataContext dataContext, String pattern) {
        return Arrays.stream(RecentProjectsManager.getInstance().getRecentProjectsActions(false)).collect(Collectors.toList());
    }

    
    @Override
    public RunAnythingItem getMainListItem(DataContext dataContext, AnAction value) {
        if (value instanceof ReopenProjectAction reopenProjectAction) {
            return new RecentProjectElement(
                reopenProjectAction,
                getCommand(value),
                reopenProjectAction.getExtensionIcon()
            );
        }
        return super.getMainListItem(dataContext, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RunAnythingGroup getCompletionGroup() {
        return new RunAnythingCompletionGroup(this, IdeLocalize.runAnythingRecentProjectCompletionGroupTitle());
    }

    
    @Override
    public String getHelpCommandPlaceholder() {
        return IdeLocalize.runAnythingRecentProjectCommandPlaceholder().get();
    }

    
    @Override
    public String getHelpCommand() {
        return "open";
    }

    @Override
    public @Nullable String getHelpGroupTitle() {
        return IdeLocalize.runAnythingRecentProjectHelpGroupTitle().get();
    }

    
    @Override
    public String getCommand(AnAction value) {
        return getHelpCommand() + " " + ObjectUtil.notNull(
            value.getTemplatePresentation().getText(),
            IdeLocalize.runAnythingActionsUndefined().get()
        );
    }

    static class RecentProjectElement extends RunAnythingItemBase {
        
        private final ReopenProjectAction myValue;

        RecentProjectElement(ReopenProjectAction value, String command, @Nullable Image icon) {
            super(command, icon);
            myValue = value;
        }

        @Override
        public @Nullable String getDescription() {
            return myValue.getProjectPath();
        }
    }

    
    @Override
    @RequiredReadAction
    public List<RunAnythingContext> getExecutionContexts(DataContext dataContext) {
        return Collections.emptyList();
    }
}