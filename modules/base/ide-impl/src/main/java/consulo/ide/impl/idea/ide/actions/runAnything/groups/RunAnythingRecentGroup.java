// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.groups;

import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingCache;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingProvider;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItem;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

public class RunAnythingRecentGroup extends RunAnythingGroupBase {
  public static final RunAnythingRecentGroup INSTANCE = new RunAnythingRecentGroup();

  private RunAnythingRecentGroup() {
  }

  @Nonnull
  @Override
  public String getTitle() {
    return IdeLocalize.runAnythingRecentGroupTitle().get();
  }

  @Nonnull
  @Override
  public Collection<RunAnythingItem> getGroupItems(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    Project project = dataContext.getData(Project.KEY);
    assert project != null;

    Collection<RunAnythingItem> collector = new ArrayList<>();
    for (String command : ContainerUtil.iterateBackward(RunAnythingCache.getInstance(project).getState().getCommands())) {
      for (RunAnythingProvider provider : RunAnythingProvider.EP_NAME.getExtensions()) {
        Object matchingValue = provider.findMatchingValue(dataContext, command);
        if (matchingValue != null) {
          //noinspection unchecked
          collector.add(provider.getMainListItem(dataContext, matchingValue));
          break;
        }
      }
    }

    return collector;
  }

  @Override
  protected int getMaxInitialItems() {
    return 10;
  }
}