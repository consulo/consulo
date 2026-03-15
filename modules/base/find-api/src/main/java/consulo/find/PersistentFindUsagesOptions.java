// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.find;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataContext;
import consulo.project.Project;

import org.jspecify.annotations.Nullable;

public abstract class PersistentFindUsagesOptions extends FindUsagesOptions {
  @RequiredReadAction
  public PersistentFindUsagesOptions(Project project) {
    super(project);
  }

  @RequiredReadAction
  public PersistentFindUsagesOptions(Project project, @Nullable DataContext dataContext) {
    super(project, dataContext);
  }

  public PersistentFindUsagesOptions(SearchScope searchScope) {
    super(searchScope);
  }

  public abstract void setDefaults(Project project);

  public abstract void storeDefaults(Project project);
}
