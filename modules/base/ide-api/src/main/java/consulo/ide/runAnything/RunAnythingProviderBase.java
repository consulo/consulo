// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.runAnything;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.matcher.Matcher;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class provides ability to run an arbitrary activity for matched 'Run Anything' input text
 */
public abstract class RunAnythingProviderBase<V> implements RunAnythingProvider<V> {
    
    @Override
    public Collection<V> getValues(DataContext dataContext, String pattern) {
        return Collections.emptyList();
    }

    @Override
    public @Nullable V findMatchingValue(DataContext dataContext, String pattern) {
        return getValues(dataContext, pattern).stream()
            .filter(value -> StringUtil.equals(pattern, getCommand(value)))
            .findFirst()
            .orElse(null);
    }

    @Override
    public @Nullable Image getIcon(V value) {
        return null;
    }

    @Override
    public @Nullable String getAdText() {
        return null;
    }

    
    @Override
    public RunAnythingItem getMainListItem(DataContext dataContext, V value) {
        return new RunAnythingItemBase(getCommand(value), getIcon(value));
    }

    @Nullable
    @Override
    public RunAnythingHelpItem getHelpItem(DataContext dataContext) {
        String placeholder = getHelpCommandPlaceholder();
        String commandPrefix = getHelpCommand();
        if (placeholder == null || commandPrefix == null) {
            return null;
        }
        return new RunAnythingHelpItem(placeholder, commandPrefix, getHelpDescription(), getHelpIcon());
    }

    @Override
    public RunAnythingGroup getCompletionGroup() {
        return null;
    }

    @Nullable
    @Override
    public Matcher getMatcher(DataContext dataContext, String pattern) {
        return null;
    }

    
    @Override
    @RequiredReadAction
    public List<RunAnythingContext> getExecutionContexts(DataContext dataContext) {
        return RunAnythingContext.allContexts(dataContext.getData(Project.KEY));
    }

    public @Nullable Image getHelpIcon() {
        return Image.empty(16);
    }

    public @Nullable String getHelpDescription() {
        return null;
    }

    public @Nullable String getHelpCommandPlaceholder() {
        return getHelpCommand();
    }

    /**
     * Null means no help command
     *
     * @return Help command.
     */
    public @Nullable String getHelpCommand() {
        return null;
    }
}