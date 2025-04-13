// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.ui.ModalityState;
import consulo.util.collection.WeakList;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IdeaModalityStateEx extends IdeaModalityState {
  private final WeakList<Object> myModalEntities = new WeakList<>();

  public IdeaModalityStateEx() {
  }

  IdeaModalityStateEx(@Nonnull Collection<Object> modalEntities) {
    myModalEntities.addAll(modalEntities);
  }

  @Nonnull
  List<Object> getModalEntities() {
    return myModalEntities.toStrongList();
  }

  @Nonnull
  public ModalityState appendProgress(@Nonnull ProgressIndicator progress) {
    return appendEntity(progress);
  }

  @Nonnull
  IdeaModalityStateEx appendEntity(@Nonnull Object anEntity) {
    List<Object> modalEntities = getModalEntities();
    List<Object> list = new ArrayList<>(modalEntities.size() + 1);
    list.addAll(modalEntities);
    list.add(anEntity);
    return new IdeaModalityStateEx(list);
  }

  void forceModalEntities(List<Object> entities) {
    myModalEntities.clear();
    myModalEntities.addAll(entities);
  }

  @Override
  public boolean dominates(@Nonnull IdeaModalityState anotherState) {
    if (anotherState == ModalityState.any()) return false;
    if (myModalEntities.isEmpty()) return false;

    List<Object> otherEntities = ((IdeaModalityStateEx)anotherState).getModalEntities();
    for (Object entity : getModalEntities()) {
      if (!otherEntities.contains(entity))
        return true; // I have entity which is absent in anotherState
    }
    return false;
  }

  @Override
  public String toString() {
    return this == nonModal() ? "ModalityState.NON_MODAL" : "ModalityState:{" + StringUtil.join(getModalEntities(),
                                                                                               it -> "[" + it + "]",
                                                                                               ", ") + "}";
  }

  void removeModality(Object modalEntity) {
    myModalEntities.remove(modalEntity);
  }
}