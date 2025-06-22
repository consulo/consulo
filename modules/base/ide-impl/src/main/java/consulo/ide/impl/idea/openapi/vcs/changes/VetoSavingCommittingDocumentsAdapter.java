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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.CommitHelper;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 * @since 2006-09-05
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class VetoSavingCommittingDocumentsAdapter {
  static final Object SAVE_DENIED = new Object();

  private final FileDocumentManager myFileDocumentManager;

  @Inject
  public VetoSavingCommittingDocumentsAdapter(final FileDocumentManager fileDocumentManager) {
    myFileDocumentManager = fileDocumentManager;

    Application.get().getMessageBus().connect().subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
      @Override
      @RequiredUIAccess
      public void beforeAllDocumentsSaving() {
        Map<Document, Project> documentsToWarn = getDocumentsBeingCommitted();
        if (!documentsToWarn.isEmpty()) {
          boolean allowSave = showAllowSaveDialog(documentsToWarn);
          updateSaveability(documentsToWarn, allowSave);
        }
      }
    });
  }

  private Map<Document, Project> getDocumentsBeingCommitted() {
    Map<Document, Project> documentsToWarn = new HashMap<>();
    for (Document unsavedDocument : myFileDocumentManager.getUnsavedDocuments()) {
      final Object data = unsavedDocument.getUserData(CommitHelper.DOCUMENT_BEING_COMMITTED_KEY);
      if (data instanceof Project project) {
        documentsToWarn.put(unsavedDocument, project);
      }
    }
    return documentsToWarn;
  }

  private static void updateSaveability(Map<Document, Project> documentsToWarn, boolean allowSave) {
    Object newValue = allowSave ? null : SAVE_DENIED;
    for (Document document : documentsToWarn.keySet()) {
      Project oldData = documentsToWarn.get(document);
      //the committing thread could have finished already and file is not being committed anymore
      document.replace(CommitHelper.DOCUMENT_BEING_COMMITTED_KEY, oldData, newValue);
    }
  }

  @RequiredUIAccess
  boolean showAllowSaveDialog(Map<Document, Project> documentsToWarn) {
    StringBuilder messageBuilder = new StringBuilder(
      "The following " + (documentsToWarn.size() == 1 ? "file is" : "files are") + " currently being committed to the VCS. " +
      "Saving now could cause inconsistent data to be committed.\n"
    );
    for (Document document : documentsToWarn.keySet()) {
      final VirtualFile file = myFileDocumentManager.getFile(document);
      messageBuilder.append(FileUtil.toSystemDependentName(file.getPath())).append("\n");
    }
    messageBuilder.append("Save the ").append(documentsToWarn.size() == 1 ? "file" : "files").append(" now?");

    Project project = documentsToWarn.values().iterator().next();
    int rc = Messages.showOkCancelDialog(
      project,
      messageBuilder.toString(),
      "Save Files During Commit",
      "Save Now",
      "Postpone Save",
      UIUtil.getQuestionIcon()
    );
    return rc == 0;
  }
}
