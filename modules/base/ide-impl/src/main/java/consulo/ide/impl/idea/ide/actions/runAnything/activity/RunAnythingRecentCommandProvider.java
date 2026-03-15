// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingCacheImpl;
import consulo.ide.internal.RunAnythingCache;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

import static consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil.fetchProject;

@ExtensionImpl(order = "last")
public class RunAnythingRecentCommandProvider extends RunAnythingCommandProvider {
    
    @Override
    public Collection<String> getValues(DataContext dataContext, String pattern) {
        return ((RunAnythingCacheImpl) RunAnythingCache.getInstance(fetchProject(dataContext))).getState().getCommands();
    }

    @Nullable
    @Override
    public String getHelpGroupTitle() {
        return null;
    }
}