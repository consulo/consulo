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
package com.intellij.openapi.ui.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.DialogWrapperPeer;
import com.intellij.openapi.ui.DialogWrapperPeerFactory;
import consulo.awt.TargetAWT;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

@Singleton
public class DialogWrapperPeerFactoryImpl extends DialogWrapperPeerFactory {
  @Override
  public DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, @Nullable Project project, boolean canBeParent) {
    return new DialogWrapperPeerImpl(wrapper, project, canBeParent);
  }

  @Override
  public DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, @Nullable Project project, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType) {
    return new DialogWrapperPeerImpl(wrapper, project, canBeParent, ideModalityType);
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