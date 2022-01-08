/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.vcs.changes;

import consulo.ui.annotation.RequiredUIAccess;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public abstract class ChangeListManagerEx extends ChangeListManager {
  public abstract boolean isInUpdate();

  @Nonnull
  public abstract Collection<LocalChangeList> getAffectedLists(@Nonnull Collection<? extends Change> changes);

  @Nullable
  public abstract LocalChangeList getIdentityChangeList(@Nonnull Change change);

  @Nonnull
  public abstract Collection<LocalChangeList> getInvolvedListsFilterChanges(@Nonnull Collection<Change> changes, @Nonnull List<Change> validChanges);

  @Nonnull
  public abstract LocalChangeList addChangeList(@Nonnull String name, @Nullable String comment, @Nullable Object data);

  /**
   * Blocks modal dialogs that we don't want to popup during some process, for example, above the commit dialog.
   * They will be shown when notifications are unblocked.
   */
  @RequiredUIAccess
  public abstract void blockModalNotifications();

  @RequiredUIAccess
  public abstract void unblockModalNotifications();

  /**
   * Temporarily disable CLM update
   * For example, to preserve FilePath->ChangeList mapping during "stash-do_smth-unstash" routine.
   */
  public abstract void freeze(@Nonnull String reason);

  public abstract void unfreeze();
}