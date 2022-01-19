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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 05.09.2006
 * Time: 20:07:21
 */
package com.intellij.openapi.vcs.changes;

import com.intellij.AppTopics;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import consulo.util.dataholder.UserDataHolderEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.changes.ui.CommitHelper;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.util.containers.ContainerUtil;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class VetoSavingCommittingDocumentsAdapter {
  static final Object SAVE_DENIED = new Object();

  private final FileDocumentManager myFileDocumentManager;

  @Inject
  public VetoSavingCommittingDocumentsAdapter(final FileDocumentManager fileDocumentManager) {
    myFileDocumentManager = fileDocumentManager;

    ApplicationManager.getApplication().getMessageBus().connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerAdapter() {
      @Override
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
    Map<Document, Project> documentsToWarn = ContainerUtil.newHashMap();
    for (Document unsavedDocument : myFileDocumentManager.getUnsavedDocuments()) {
      final Object data = unsavedDocument.getUserData(CommitHelper.DOCUMENT_BEING_COMMITTED_KEY);
      if (data instanceof Project) {
        documentsToWarn.put(unsavedDocument, (Project)data);
      }
    }
    return documentsToWarn;
  }

  private static void updateSaveability(Map<Document, Project> documentsToWarn, boolean allowSave) {
    Object newValue = allowSave ? null : SAVE_DENIED;
    for (Document document : documentsToWarn.keySet()) {
      Project oldData = documentsToWarn.get(document);
      //the committing thread could have finished already and file is not being committed anymore
      ((UserDataHolderEx)document).replace(CommitHelper.DOCUMENT_BEING_COMMITTED_KEY, oldData, newValue);
    }
  }

  boolean showAllowSaveDialog(Map<Document, Project> documentsToWarn) {
    StringBuilder messageBuilder = new StringBuilder("The following " + (documentsToWarn.size() == 1 ? "file is" : "files are") +
                                                     " currently being committed to the VCS. " +
                                                     "Saving now could cause inconsistent data to be committed.\n");
    for (Document document : documentsToWarn.keySet()) {
      final VirtualFile file = myFileDocumentManager.getFile(document);
      messageBuilder.append(FileUtil.toSystemDependentName(file.getPath())).append("\n");
    }
    messageBuilder.append("Save the ").append(documentsToWarn.size() == 1 ? "file" : "files").append(" now?");

    Project project = documentsToWarn.values().iterator().next();
    int rc = Messages.showOkCancelDialog(project, messageBuilder.toString(), "Save Files During Commit", "Save Now", "Postpone Save",
                                         Messages.getQuestionIcon());
    return rc == 0;
  }
}
