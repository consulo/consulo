/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.Set;

@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION)
@ServiceImpl
public class DesktopAWTVisibleEditorsTracker {
  private final Set<Editor> myEditorsVisibleOnCommandStart = new HashSet<>();
  private long myCurrentCommandStart;
  private long myLastCommandFinish;

  public static DesktopAWTVisibleEditorsTracker getInstance() {
    return Application.get().getInstance(DesktopAWTVisibleEditorsTracker.class);
  }

  public boolean wasEditorVisibleOnCommandStart(Editor editor) {
    return myEditorsVisibleOnCommandStart.contains(editor);
  }

  public long getCurrentCommandStart() {
    return myCurrentCommandStart;
  }

  public long getLastCommandFinish() {
    return myLastCommandFinish;
  }

  public void registerEditor(Editor editor) {
    if (editor.getComponent().isShowing()) {
      myEditorsVisibleOnCommandStart.add(editor);
    }

    ((DesktopScrollingModelImpl)editor.getScrollingModel()).finishAnimation();

    myCurrentCommandStart = System.currentTimeMillis();
  }

  public void reset() {
    myEditorsVisibleOnCommandStart.clear();
    myLastCommandFinish = System.currentTimeMillis();
  }
}
