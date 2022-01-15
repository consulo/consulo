// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.Matcher;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class RunAnythingCompletionGroup<V, P extends RunAnythingProvider<V>> extends RunAnythingGroupBase {
  public static final Collection<RunAnythingGroup> MAIN_GROUPS = createCompletionGroups();

  @Nonnull
  private final P myProvider;

  public RunAnythingCompletionGroup(@Nonnull P provider) {
    myProvider = provider;
  }

  @Nonnull
  protected P getProvider() {
    return myProvider;
  }

  @Nonnull
  @Override
  public String getTitle() {
    return ObjectUtils.assertNotNull(getProvider().getCompletionGroupTitle());
  }

  @Nonnull
  @Override
  public Collection<RunAnythingItem> getGroupItems(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    P provider = getProvider();
    return ContainerUtil.map(provider.getValues(dataContext, pattern), value -> provider.getMainListItem(dataContext, value));
  }

  @Nullable
  @Override
  protected Matcher getMatcher(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    return getProvider().getMatcher(dataContext, pattern);
  }

  public static Collection<RunAnythingGroup> createCompletionGroups() {
    return RunAnythingProvider.EP_NAME.getExtensionList().stream().map(RunAnythingCompletionGroup::createCompletionGroup).filter(Objects::nonNull).distinct().collect(Collectors.toList());
  }

  @Nullable
  public static RunAnythingGroup createCompletionGroup(@Nonnull RunAnythingProvider provider) {
    String title = provider.getCompletionGroupTitle();
    if (title == null) {
      return null;
    }

    if (RunAnythingGeneralGroup.GENERAL_GROUP_TITLE.equals(title)) {
      return RunAnythingGeneralGroup.INSTANCE;
    }

    //noinspection unchecked
    return new RunAnythingCompletionGroup(provider);
  }
}