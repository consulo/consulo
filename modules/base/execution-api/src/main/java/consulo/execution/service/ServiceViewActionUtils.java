// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.service;

import consulo.language.editor.PlatformDataKeys;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class ServiceViewActionUtils {
  public static final Key<Set<ServiceViewContributor>> CONTRIBUTORS_KEY = Key.create("serviceViewContributors");
  public static final Key<ServiceViewOptions> OPTIONS_KEY = Key.create("ServiceViewTreeOptions");
  public static final Key<Boolean> IS_FROM_TREE_KEY = Key.create("IsFromTreeSource");

  private ServiceViewActionUtils() {
  }

  public static @Nullable <T> T getTarget(@Nonnull AnActionEvent e, @Nonnull Class<T> clazz) {
    Object[] items = e.getData(PlatformDataKeys.SELECTED_ITEMS);
    return items != null && items.length == 1 ? ObjectUtil.tryCast(items[0], clazz) : null;
  }

  public static @Nonnull <T> List<T> getTargets(@Nonnull AnActionEvent e, @Nonnull Class<T> clazz) {
    Object[] items = e.getData(PlatformDataKeys.SELECTED_ITEMS);
    if (items == null) return Collections.emptyList();

    List<T> result = new ArrayList<>();
    for (Object item : items) {
      if (!clazz.isInstance(item)) {
        return Collections.emptyList();
      }
      result.add(clazz.cast(item));
    }
    return result;
  }
}
