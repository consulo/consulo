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
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorLastActionTracker;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import consulo.annotation.inject.NotLazy;

import javax.annotation.Nullable;
import consulo.annotation.inject.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@NotLazy
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
  EditorLastActionTrackerImpl(ActionManager actionManager, EditorFactory editorFactory) {
    myActionManager = actionManager;
    myEditorEventMulticaster = editorFactory.getEventMulticaster();
  }

  @PostConstruct
  public void initComponent() {
    myActionManager.addAnActionListener(this);
    myEditorEventMulticaster.addEditorMouseListener(this);
  }

  @Override
  public void dispose() {
    myEditorEventMulticaster.removeEditorMouseListener(this);
    myActionManager.removeAnActionListener(this);
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
    return action instanceof ActionStub ? ((ActionStub)action).getId() : myActionManager.getId(action);
  }

  private void resetLastAction() {
    myLastActionId = null;
    myLastEditor = null;
  }

  private void registerDisposeHandler(final Editor editor) {
    if (!(editor instanceof DesktopEditorImpl)) {
      return;
    }
    DesktopEditorImpl editorImpl = (DesktopEditorImpl)editor;
    if (editorImpl.replace(DISPOSABLE_SET, null, Boolean.TRUE)) {
      Disposer.register(editorImpl.getDisposable(), new Disposable() {
        @Override
        public void dispose() {
          if (myCurrentEditor == editor) {
            myCurrentEditor = null;
          }
          if (myLastEditor == editor) {
            myLastEditor = null;
          }
        }
      });
    }
  }
}
