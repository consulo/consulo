// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.awt.tree;

import jakarta.annotation.Nullable;

import java.util.List;

public abstract class BaseTreeModel<T> extends AbstractTreeModel implements ChildrenProvider<T> {
  @Override
  public boolean isLeaf(Object object) {
    return 0 == getChildCount(object);
  }

  @Override
  public int getChildCount(Object object) {
    List<? extends T> list = getChildren(object);
    return list == null ? 0 : list.size();
  }

  @Override
  @Nullable
  public Object getChild(Object object, int index) {
    if (index < 0) return null;
    List<? extends T> list = getChildren(object);
    return list != null && index < list.size() ? list.get(index) : null;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public int getIndexOfChild(Object object, Object child) {
    List<? extends T> list = getChildren(object);
    return list == null ? -1 : list.indexOf(child);
  }
}
