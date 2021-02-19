// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.findUsages;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.SearchScope;
import consulo.annotation.access.RequiredReadAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class PersistentFindUsagesOptions extends FindUsagesOptions {
  @RequiredReadAction
  public PersistentFindUsagesOptions(@Nonnull Project project) {
    super(project);
  }

  @RequiredReadAction
  public PersistentFindUsagesOptions(@Nonnull Project project, @Nullable DataContext dataContext) {
    super(project, dataContext);
  }

  public PersistentFindUsagesOptions(@Nonnull SearchScope searchScope) {
    super(searchScope);
  }

  public abstract void setDefaults(@Nonnull Project project);

  public abstract void storeDefaults(@Nonnull Project project);

}
