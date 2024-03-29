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
package consulo.versionControlSystem.change.commited;

import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;

import java.util.List;

public class VcsCommittedViewAuxiliary {

  @Nonnull
  private final List<AnAction> myToolbarActions;
  @Nonnull
  private final List<AnAction> myPopupActions;
  @Nonnull
  private final Runnable myCalledOnViewDispose;

  public VcsCommittedViewAuxiliary(@Nonnull List<AnAction> popupActions, @Nonnull Runnable calledOnViewDispose,
                                   @Nonnull List<AnAction> toolbarActions) {
    myToolbarActions = toolbarActions;
    myPopupActions = popupActions;
    myCalledOnViewDispose = calledOnViewDispose;
  }

  @Nonnull
  public List<AnAction> getPopupActions() {
    return myPopupActions;
  }

  @Nonnull
  public Runnable getCalledOnViewDispose() {
    return myCalledOnViewDispose;
  }

  @Nonnull
  public List<AnAction> getToolbarActions() {
    return myToolbarActions;
  }
}
