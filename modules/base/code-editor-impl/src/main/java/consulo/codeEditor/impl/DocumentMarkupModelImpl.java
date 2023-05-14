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
package consulo.codeEditor.impl;

import consulo.annotation.DeprecationInfo;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.language.file.inject.DocumentWindow;
import consulo.project.Project;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages per-project markup models of documents.
 *
 * @author yole
 */
@Deprecated
@DeprecationInfo("Use DocumentMarkupModel")
public class DocumentMarkupModelImpl {
  private static final Key<MarkupModelEx> MARKUP_MODEL_KEY = Key.create("DocumentMarkupModel.MarkupModel");
  private static final Key<ConcurrentMap<Project, MarkupModelImpl>> MARKUP_MODEL_MAP_KEY = Key.create("DocumentMarkupModel.MarkupModelMap");

  private DocumentMarkupModelImpl() {
  }

  /**
   * Returns the markup model for the specified project. A document can have multiple markup
   * models for different projects if the file to which it corresponds belongs to multiple projects
   * opened in different IDEA frames at the same time.
   *
   * @param document the document for which the markup model is requested.
   * @param project the project for which the markup model is requested, or null if the default markup
   *                model is requested.
   * @return the markup model instance.
   * @see Editor#getMarkupModel()
   */
  public static MarkupModelEx forDocument(@Nonnull Document document, @Nullable Project project, boolean create) {
    if (document instanceof DocumentWindow) {
      final Document delegate = ((DocumentWindow)document).getDelegate();
      final MarkupModelEx baseMarkupModel = forDocument(delegate, project, true);
      return new MarkupModelWindow(baseMarkupModel, (DocumentWindow) document);
    }

    if (project == null) {
      MarkupModelEx markupModel = document.getUserData(MARKUP_MODEL_KEY);
      if (create && markupModel == null) {
        MarkupModelEx newModel = new MarkupModelImpl((DocumentEx)document);
        if ((markupModel = document.putUserDataIfAbsent(MARKUP_MODEL_KEY, newModel)) != newModel) {
          newModel.dispose();
        }
      }
      return markupModel;
    }

    final DocumentMarkupModelManager documentMarkupModelManager =
            project.isDisposed() ? null : DocumentMarkupModelManager.getInstance(project);
    if (documentMarkupModelManager == null || documentMarkupModelManager.isDisposed() || project.isDisposed()) {
      return new EmptyMarkupModel(document);
    }

    ConcurrentMap<Project, MarkupModelImpl> markupModelMap = getMarkupModelMap(document);

    MarkupModelImpl model = markupModelMap.get(project);
    if (create && model == null) {
      MarkupModelImpl newModel = new MarkupModelImpl((DocumentEx)document);
      if ((model = Maps.cacheOrGet(markupModelMap, project, newModel)) == newModel) {
        documentMarkupModelManager.registerDocument(document);
      }
      else {
        newModel.dispose();
      }
    }

    return model;
  }

  private static ConcurrentMap<Project, MarkupModelImpl> getMarkupModelMap(@Nonnull Document document) {
    ConcurrentMap<Project, MarkupModelImpl> markupModelMap = document.getUserData(MARKUP_MODEL_MAP_KEY);
    if (markupModelMap == null) {
      ConcurrentMap<Project, MarkupModelImpl> newMap = new ConcurrentHashMap<>();
      markupModelMap = document.putUserDataIfAbsent(MARKUP_MODEL_MAP_KEY, newMap);
    }
    return markupModelMap;
  }

  static void removeMarkupModel(@Nonnull Document document, @Nonnull Project project) {
    MarkupModelImpl removed = getMarkupModelMap(document).remove(project);
    if (removed != null) {
      removed.dispose();
    }
  }
}
