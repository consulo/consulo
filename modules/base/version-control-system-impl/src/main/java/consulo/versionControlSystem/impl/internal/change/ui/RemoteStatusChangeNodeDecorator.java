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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.impl.internal.change.RemoteRevisionsCache;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangeNodeDecorator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RemoteStatusChangeNodeDecorator implements ChangeNodeDecorator {
  private final RemoteRevisionsCache myRemoteRevisionsCache;
  private final ChangeListRemoteState myListState;
  private final int myIdx;

  public RemoteStatusChangeNodeDecorator(@Nonnull RemoteRevisionsCache remoteRevisionsCache) {
    this(remoteRevisionsCache, null, -1);
  }

  public RemoteStatusChangeNodeDecorator(@Nonnull RemoteRevisionsCache remoteRevisionsCache,
                                         @Nullable ChangeListRemoteState listRemoteState,
                                         int idx) {
    myRemoteRevisionsCache = remoteRevisionsCache;
    myListState = listRemoteState;
    myIdx = idx;
  }

  @Override
  public void decorate(Change change, SimpleColoredComponent component, boolean isShowFlatten) {
    boolean state = myRemoteRevisionsCache.isUpToDate(change);
    if (myListState != null) myListState.report(myIdx, state);
    if (!state) {
      component.append(" ");
      component.append(VcsBundle.message("change.nodetitle.change.is.outdated"), SimpleTextAttributes.ERROR_ATTRIBUTES);
    }
  }
}
