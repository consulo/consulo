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
package consulo.codeEditor.impl;

import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.WeakList;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author max
 */
@Singleton
public class DocumentMarkupModelManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(DocumentMarkupModelManager.class);

  private final WeakList<Document> myDocumentSet = new WeakList<>();
  private volatile boolean myDisposed;

  public static DocumentMarkupModelManager getInstance(Project project) {
    return project.getInstance(DocumentMarkupModelManager.class);
  }

  private Project myProject;

  @Inject
  public DocumentMarkupModelManager(@Nonnull Project project) {
    myProject = project;
  }

  public void registerDocument(Document document) {
    LOG.assertTrue(!myDisposed);
    myDocumentSet.add(document);
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public void dispose() {
    if (!myDisposed) {
      myDisposed = true;
      for (Document document : myDocumentSet.toStrongList()) {
        DocumentMarkupModelImpl.removeMarkupModel(document, myProject);
      }
    }
  }
}
