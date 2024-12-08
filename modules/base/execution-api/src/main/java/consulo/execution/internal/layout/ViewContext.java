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

package consulo.execution.internal.layout;

import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.ui.ex.action.ActionManager;
import consulo.project.Project;
import consulo.util.concurrent.ActionCallback;
import consulo.component.util.ActiveRunnable;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface ViewContext extends Disposable {
  @Deprecated
  Key<Content[]> CONTENT_KEY = Content.KEY_OF_ARRAY;
  Key<ViewContext> CONTEXT_KEY = Key.create("runnerUiContext");

  String CELL_TOOLBAR_PLACE = "debuggerCellToolbar";
  String TAB_TOOLBAR_PLACE = "debuggerTabToolbar";

  String CELL_POPUP_PLACE = "debuggerCellPopup";
  String TAB_POPUP_PLACE = "debuggerTabPopup";

  CellTransform.Facade getCellTransform();

  @Nullable
  Tab getTabFor(final Grid grid);

  View getStateFor(@Nonnull Content content);

  void saveUiState();

  Project getProject();

  ContentManager getContentManager();

  ActionManager getActionManager();

  IdeFocusManager getFocusManager();

  RunnerLayoutUi getRunnerLayoutUi();

  GridCell findCellFor(@Nonnull final Content content);

  Grid findGridFor(@Nonnull Content content);

  ActionCallback select(Content content, boolean requestFocus);

  boolean isStateBeingRestored();

  void setStateIsBeingRestored(boolean state, final Object requestor);

  void validate(Content content, ActiveRunnable toRestore);

  void restoreLayout();

  boolean isMinimizeActionEnabled();

  boolean isMoveToGridActionEnabled();

  boolean isToDisposeRemovedContent();
}
