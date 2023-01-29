// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.groups;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingProvider;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItem;
import consulo.dataContext.DataContext;
import consulo.component.extension.ExtensionPointName;
import consulo.ide.impl.idea.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 'Run Anything' popup help section is divided into groups by categories.
 * E.g. 'ruby' help group contains 'ruby' related run configuration commands, 'rvm use #sdk_version' commands etc.
 */

@ExtensionAPI(ComponentScope.APPLICATION)
public class RunAnythingHelpGroup<P extends RunAnythingProvider> extends RunAnythingGroupBase {
  public static final ExtensionPointName<RunAnythingGroup> EP_NAME = ExtensionPointName.create(RunAnythingGroup.class);

  @Nonnull
  private String myTitle = "undefined";
  @Nonnull
  private Collection<P> myProviders = ContainerUtil.emptyList();

  public RunAnythingHelpGroup(@Nonnull String title, @Nonnull Collection<P> providers) {
    myTitle = title;
    myProviders = providers;
  }

  /**
   * @deprecated API compatibility
   */
  @Deprecated
  public RunAnythingHelpGroup() {
  }

  @Nonnull
  @Override
  public String getTitle() {
    return myTitle;
  }

  /**
   * Returns collections of providers each of them is expecting to provide not null {@link RunAnythingProvider#getHelpItem(DataContext)}
   * See also {@code RunAnythingProviderBase.getHelp*()} methods.
   *
   * @deprecated please use {@link RunAnythingProvider#getHelpGroupTitle()} instead
   */
  @Deprecated
  @Nonnull
  public Collection<P> getProviders() {
    return myProviders;
  }

  @Nonnull
  @Override
  public Collection<RunAnythingItem> getGroupItems(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    return getProviders().stream().map(provider -> provider.getHelpItem(dataContext)).filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  protected int getMaxInitialItems() {
    return 15;
  }
}