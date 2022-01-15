/*
 * Copyright 2013-2020 consulo.io
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
package consulo.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.impl.EditorTracker;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.TraverseUtil;
import consulo.wm.util.IdeFrameUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 07/12/2020
 */
@Singleton
public class UnifiedEditorTrackerImpl extends EditorTracker {
  private static final Logger LOG = Logger.getInstance(UnifiedEditorTrackerImpl.class);

  @Inject
  public UnifiedEditorTrackerImpl(@Nonnull Project project, @Nonnull Provider<WindowManager> windowManagerProvider) {
    super(project, windowManagerProvider);
  }

  @RequiredUIAccess
  @Override
  protected void editorCreated(@Nonnull EditorFactoryEvent editorFactoryEvent) {
    final Editor editor = editorFactoryEvent.getEditor();
    if ((editor.getProject() != null && editor.getProject() != myProject) || myProject.isDisposedOrDisposeInProgress()) {
      return;
    }

    final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
    if (psiFile == null) return;

    Component uiComponent = editor.getUIComponent();
    uiComponent.addAttachListener(e -> {
      registerEditor(editor);
    });

    // TODO [VISTALL] focus handling

    registerEditor(editor);

    Window window = windowByEditor(editor);
    if (window == null) {
      return;
    }

    setActiveWindow(window);
  }

  @Override
  protected void unregisterEditor(Editor editor) {
    Window oldWindow = myEditorToWindowMap.remove(editor);
    if (oldWindow != null) {
      List<Editor> editorsList = myWindowToEditorsMap.get(oldWindow);
      boolean removed = editorsList.remove(editor);
      LOG.assertTrue(removed);

      if (editorsList.isEmpty()) {
        myWindowToEditorsMap.remove(oldWindow);
      }
    }
  }

  @RequiredUIAccess
  private void registerEditor(@Nonnull Editor editor) {
    unregisterEditor(editor);

    Window window = windowByEditor(editor);
    if (window == null) {
      return;
    }

    myEditorToWindowMap.put(editor, window);
    List<Editor> list = myWindowToEditorsMap.get(window);
    if (list == null) {
      list = new ArrayList<>();
      myWindowToEditorsMap.put(window, list);
    }
    list.add(editor);

    if (myActiveWindow == window) {
      setActiveWindow(window); // to fire event
    }
  }

  private Window windowByEditor(Editor editor) {
    Window window = TraverseUtil.getWindowAncestor(editor.getUIComponent());
    if (window != null) {
      IdeFrame ideFrame = window.getUserData(IdeFrame.KEY);
      if (IdeFrameUtil.isRootFrame(ideFrame)) {
        if (myProject != ideFrame.getProject()) {
          return null;
        }
      }
    }
    return window;
  }
}
