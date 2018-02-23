/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.dvcs.push;

import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.RepositoryManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vcs.AbstractVcs;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base class to provide vcs-specific info
 */

public abstract class PushSupport<Repo extends Repository, Source extends PushSource, Target extends PushTarget> {

  public static final ExtensionPointName<PushSupport<? extends Repository, ? extends PushSource, ? extends PushTarget>> PUSH_SUPPORT_EP =
          ExtensionPointName.create("com.intellij.pushSupport");

  @Nonnull
  public abstract AbstractVcs getVcs();

  @Nonnull
  public abstract Pusher<Repo, Source, Target> getPusher();

  @Nonnull
  public abstract OutgoingCommitsProvider<Repo, Source, Target> getOutgoingCommitsProvider();

  /**
   * @return Default push destination
   */
  @Nullable
  public abstract Target getDefaultTarget(@Nonnull Repo repository);

  /**
   * @return current source(branch) for repository
   */
  @Nonnull
  public abstract Source getSource(@Nonnull Repo repository);

  /**
   * @return RepositoryManager for vcs
   */
  @Nonnull
  public abstract RepositoryManager<Repo> getRepositoryManager();

  @javax.annotation.Nullable
  public VcsPushOptionsPanel createOptionsPanel() {
    return null;
  }

  @Nonnull
  public abstract PushTargetPanel<Target> createTargetPanel(@Nonnull Repo repository, @Nullable Target defaultTarget);

  public boolean shouldRequestIncomingChangesForNotCheckedRepositories() {
    return true;
  }

  /**
   * Returns true if force push is allowed now in the selected repository.
   * <p/>
   * Force push may be completely disabled for the project which is checked by {@link #isForcePushEnabled()},
   * or it might depend e.g. on the branch user is pushing to.
   */
  public abstract boolean isForcePushAllowed(@Nonnull Repo repo, Target target);

  /**
   * Checks if force push is allowed for this VCS at all.
   * <p/>
   * If it is not allowed for all PushSupports in the project, the "Force Push" button will be invisible.
   */
  public abstract boolean isForcePushEnabled();

  public abstract boolean isSilentForcePushAllowed(@Nonnull Target target);

  public abstract void saveSilentForcePushTarget(@Nonnull Target target);

  public boolean mayChangeTargetsSync() {
    return false;
  }
}
