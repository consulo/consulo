/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.impl.VisibleEditorsTracker;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 05-Aug-22
 */
@TopicImpl(ComponentScope.APPLICATION)
public class DesktopAWTVisibleEditorsTrackerCommandListener implements CommandListener {
  private final Provider<VisibleEditorsTracker> myEditorsTrackerProvider;
  private final Provider<EditorFactory> myEditorFactoryProvider;

  @Inject
  public DesktopAWTVisibleEditorsTrackerCommandListener(Provider<VisibleEditorsTracker> editorsTrackerProvider, Provider<EditorFactory> editorFactoryProvider) {
    myEditorsTrackerProvider = editorsTrackerProvider;
    myEditorFactoryProvider = editorFactoryProvider;
  }

  @Override
  public void commandStarted(CommandEvent event) {
    VisibleEditorsTracker editorsTracker = myEditorsTrackerProvider.get();
    for (Editor editor : myEditorFactoryProvider.get().getAllEditors()) {
      editorsTracker.registerEditor(editor);
    }
  }

  @Override
  public void commandFinished(CommandEvent event) {
    myEditorsTrackerProvider.get().reset();
  }
}
