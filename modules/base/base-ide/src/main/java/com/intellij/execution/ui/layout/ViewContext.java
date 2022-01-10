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

package com.intellij.execution.ui.layout;

import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.ActiveRunnable;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ViewContext extends Disposable {
  Key<Content[]> CONTENT_KEY = Key.create("runnerContents");
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
