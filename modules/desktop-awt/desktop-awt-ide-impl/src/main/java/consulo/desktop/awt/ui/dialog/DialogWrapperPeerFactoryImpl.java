/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.dialog;

import consulo.application.ApplicationManager;
import consulo.component.ComponentManager;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.internal.DialogWrapperPeer;
import consulo.ui.ex.awt.internal.DialogWrapperPeerFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

@Singleton
public class DialogWrapperPeerFactoryImpl extends DialogWrapperPeerFactory {
  @Override
  public DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, @Nullable ComponentManager project, boolean canBeParent) {
    return createPeer(wrapper, project, canBeParent, DialogWrapper.IdeModalityType.IDE);
  }

  @Override
  public DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, @Nullable ComponentManager project, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType) {
    consulo.ui.Window owner = null;
    if (ApplicationManager.getApplication() != null) {
      owner = project != null ? WindowManager.getInstance().suggestParentWindow((Project)project) : WindowManager.getInstance().findVisibleWindow();
    }

    return createPeer(wrapper, owner, canBeParent, ideModalityType);
  }

  @Override
  public DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, boolean canBeParent) {
    return new DialogWrapperPeerImpl(wrapper, canBeParent);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  public DialogWrapperPeer createPeer(@Nonnull final DialogWrapper wrapper, final boolean canBeParent, final boolean applicationModalIfPossible) {
    return new DialogWrapperPeerImpl(wrapper, null, canBeParent, applicationModalIfPossible);
  }

  @Override
  public DialogWrapperPeer createPeer(@Nonnull final DialogWrapper wrapper, final consulo.ui.Window owner, final boolean canBeParent, final boolean applicationModalIfPossible) {
    return new DialogWrapperPeerImpl(wrapper, TargetAWT.to(owner), canBeParent, applicationModalIfPossible);
  }

  @Override
  public DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, @Nonnull Component parent, boolean canBeParent) {
    return new DialogWrapperPeerImpl(wrapper, parent, canBeParent);
  }

  @Override
  public DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType) {
    return new DialogWrapperPeerImpl(wrapper, (Window)null, canBeParent, ideModalityType);
  }

  @Override
  public DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, consulo.ui.Window owner, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType) {
    return new DialogWrapperPeerImpl(wrapper, TargetAWT.to(owner), canBeParent, ideModalityType);
  }
}