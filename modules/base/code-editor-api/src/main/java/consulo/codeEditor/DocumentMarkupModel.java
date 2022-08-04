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
package consulo.codeEditor;

import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.document.Document;
import consulo.project.Project;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages per-project markup models of documents.
 *
 * @author yole
 */
public final class DocumentMarkupModel {
  private DocumentMarkupModel() {
  }

  /**
   * Returns the markup model for the specified project. A document can have multiple markup
   * models for different projects if the file to which it corresponds belongs to multiple projects
   * opened in different IDE frames at the same time.
   *
   * @param document the document for which the markup model is requested.
   * @param project  the project for which the markup model is requested, or null if the default markup
   *                 model is requested.
   * @return the markup model instance.
   * @see Editor#getMarkupModel()
   */
  @Nullable
  @Contract("_, _, true -> !null")
  public static MarkupModelEx forDocument(@Nonnull Document document, @Nullable Project project, boolean create) {
    return CodeEditorInternalHelper.getInstance().forDocument(document, project, create);
  }
}
