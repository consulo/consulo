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
package consulo.ui.ex.awt.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.ui.Window;
import consulo.ui.ex.awt.DialogWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

@Service(ComponentScope.APPLICATION)
public abstract class DialogWrapperPeerFactory {
  @Nonnull
  public static DialogWrapperPeerFactory getInstance() {
    return Application.get().getInstance(DialogWrapperPeerFactory.class);
  }

  public abstract DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, @Nullable ComponentManager project, boolean canBeParent);

  public abstract DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, boolean canBeParent);

  public abstract DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, @Nullable ComponentManager project, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType);

  /**
   * @see DialogWrapper#DialogWrapper(boolean, boolean)
   */
  @Deprecated
  public abstract DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, boolean canBeParent, boolean applicationModalIfPossible);

  @Deprecated
  public abstract DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, Window owner, boolean canBeParent, boolean applicationModalIfPossible);

  @Deprecated
  public abstract DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, @Nonnull Component parent, boolean canBeParent);

  public abstract DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType);

  public abstract DialogWrapperPeer createPeer(@Nonnull DialogWrapper wrapper, Window owner, boolean canBeParent, DialogWrapper.IdeModalityType ideModalityType);
}