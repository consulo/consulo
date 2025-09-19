/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.FileDocumentSynchronizationVetoer;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.project.Project;
import consulo.project.ProjectManager;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl
public class LookupDocumentSavingVetoer extends FileDocumentSynchronizationVetoer {
  @Override
  public boolean maySaveDocument(@Nonnull Document document, boolean isSaveExplicit) {
    if (ApplicationManager.getApplication().isDisposed() || isSaveExplicit) {
      return true;
    }

    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (!project.isInitialized() || project.isDisposed()) {
        continue;
      }
      LookupEx lookup = LookupManager.getInstance(project).getActiveLookup();
      if (lookup != null) {
        Editor editor = EditorWindow.getTopLevelEditor(lookup.getEditor());
        if (editor.getDocument() == document) {
          return false;
        }
      }
    }
    return true;
  }

}
