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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.project.Project;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

@ExtensionImpl
final class EditorTrackerEditorFactoryListener implements EditorFactoryListener {
  @Inject
  EditorTrackerEditorFactoryListener() {
  }

  @Override
  public void editorCreated(@Nonnull EditorFactoryEvent event) {
    Project project = event.getEditor().getProject();
    if (project != null) {
      EditorTracker.getInstance(project).editorCreated(event);
    }
  }

  @Override
  public void editorReleased(@Nonnull EditorFactoryEvent event) {
    Project project = event.getEditor().getProject();
    if (project != null) {
      EditorTracker.getInstance(project).editorReleased(event);
    }
  }
}
