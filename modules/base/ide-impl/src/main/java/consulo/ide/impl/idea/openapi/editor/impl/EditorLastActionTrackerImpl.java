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
package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorEventMulticaster;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.editor.EditorLastActionTracker;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.internal.ActionStubBase;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;

@Singleton
@ServiceImpl
public class EditorLastActionTrackerImpl extends EditorLastActionTracker implements Disposable,
                                                                                    AnActionListener,
                                                                                    EditorMouseListener {
  private static final Key<Boolean> DISPOSABLE_SET = Key.create("EditorLastActionTracker.dispose.handler.set");

  private final ActionManager myActionManager;
  private final EditorEventMulticaster myEditorEventMulticaster;

  private String myLastActionId;
  private Editor myCurrentEditor;
  private Editor myLastEditor;

  @Inject
  EditorLastActionTrackerImpl(Application application, ActionManager actionManager, EditorFactory editorFactory) {
    myActionManager = actionManager;
    application.getMessageBus().connect(this).subscribe(AnActionListener.class, this);
    myEditorEventMulticaster = editorFactory.getEventMulticaster();
    myEditorEventMulticaster.addEditorMouseListener(this, this);
  }

  @Override
  public void dispose() {
    myEditorEventMulticaster.removeEditorMouseListener(this);
  }

  @Override
  @Nullable
  public String getLastActionId() {
    return myLastActionId;
  }

  @Override
  public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
    myCurrentEditor = dataContext.getData(CommonDataKeys.EDITOR);
    registerDisposeHandler(myCurrentEditor);
    if (myCurrentEditor != myLastEditor) {
      resetLastAction();
    }
  }

  @Override
  public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
    myLastActionId = getActionId(action);
    myLastEditor = myCurrentEditor;
    myCurrentEditor = null;
  }

  @Override
  public void beforeEditorTyping(char c, DataContext dataContext) {
    resetLastAction();
  }

  @Override
  public void mousePressed(EditorMouseEvent e) {
    resetLastAction();
  }

  @Override
  public void mouseClicked(EditorMouseEvent e) {
    resetLastAction();
  }

  @Override
  public void mouseReleased(EditorMouseEvent e) {
    resetLastAction();
  }

  @Override
  public void mouseEntered(EditorMouseEvent e) {

  }

  @Override
  public void mouseExited(EditorMouseEvent e) {

  }

  private String getActionId(AnAction action) {
    return action instanceof ActionStubBase ? ((ActionStubBase)action).getId() : myActionManager.getId(action);
  }

  private void resetLastAction() {
    myLastActionId = null;
    myLastEditor = null;
  }

  private void registerDisposeHandler(final Editor editor) {
    if (!(editor instanceof CodeEditorBase)) {
      return;
    }
    CodeEditorBase editorImpl = (CodeEditorBase)editor;
    if (editorImpl.replace(DISPOSABLE_SET, null, Boolean.TRUE)) {
      Disposer.register(editorImpl.getDisposable(), () -> {
        if (myCurrentEditor == editor) {
          myCurrentEditor = null;
        }
        if (myLastEditor == editor) {
          myLastEditor = null;
        }
      });
    }
  }
}
