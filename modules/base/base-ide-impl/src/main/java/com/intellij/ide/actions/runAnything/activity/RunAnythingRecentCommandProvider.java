// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.ide.actions.runAnything.RunAnythingCache;
import com.intellij.openapi.actionSystem.DataContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

import static com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject;

public class RunAnythingRecentCommandProvider extends RunAnythingCommandProvider {
  @Nonnull
  @Override
  public Collection<String> getValues(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    return RunAnythingCache.getInstance(fetchProject(dataContext)).getState().getCommands();
  }

  @Nullable
  @Override
  public String getHelpGroupTitle() {
    return null;
  }
}