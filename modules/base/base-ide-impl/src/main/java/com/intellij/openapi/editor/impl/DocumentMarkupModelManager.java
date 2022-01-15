/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.logging.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposer;
import com.intellij.util.containers.WeakList;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

import jakarta.inject.Singleton;

/**
 * @author max
 */
@Singleton
public class DocumentMarkupModelManager {
  private static final Logger LOG = Logger.getInstance(DocumentMarkupModelManager.class);

  private final WeakList<Document> myDocumentSet = new WeakList<Document>();
  private volatile boolean myDisposed;

  public static DocumentMarkupModelManager getInstance(Project project) {
    return project.getComponent(DocumentMarkupModelManager.class);
  }

  private Project myProject;

  @Inject
  public DocumentMarkupModelManager(@Nonnull Project project) {
    myProject = project;
    Disposer.register(project, () -> cleanupProjectMarkups());
  }

  public void registerDocument(Document document) {
    LOG.assertTrue(!myDisposed);
    myDocumentSet.add(document);
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  private void cleanupProjectMarkups() {
    if (!myDisposed) {
      myDisposed = true;
      for (Document document : myDocumentSet.toStrongList()) {
        DocumentMarkupModel.removeMarkupModel(document, myProject);
      }
    }
  }
}
