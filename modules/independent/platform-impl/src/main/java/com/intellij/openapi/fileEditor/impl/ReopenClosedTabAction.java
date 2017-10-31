/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.fileEditor.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import consulo.annotations.RequiredDispatchThread;
import consulo.fileEditor.impl.EditorSplitters;
import consulo.fileEditor.impl.EditorWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ReopenClosedTabAction extends AnAction {
  public ReopenClosedTabAction() {
    super("Reopen Closed Tab");
  }

  @RequiredDispatchThread
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final EditorWindow window = getEditorWindow(e);
    if (window != null) {
      window.restoreClosedTab();
    }
  }

  @Nullable
  private static EditorWindow getEditorWindow(AnActionEvent e) {
    final Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    if (component != null) {
      EditorSplitters splitters = DataManager.getInstance().getDataContext(component).getData(EditorSplitters.KEY);
      if (splitters != null) {
        return splitters.getCurrentWindow();
      }
    }
    return null;
  }

  @RequiredDispatchThread
  @Override
  public void update(@NotNull AnActionEvent e) {
    final EditorWindow window = getEditorWindow(e);
    e.getPresentation().setEnabled(window != null && window.hasClosedTabs());
  }
}
