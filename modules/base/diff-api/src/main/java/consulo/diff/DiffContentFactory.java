/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.Application;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.EmptyContent;
import consulo.diff.content.FileContent;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/*
 * Use ProgressManager.executeProcessUnderProgress() to pass modality state if needed
 */
@Service(ComponentScope.APPLICATION)
public abstract class DiffContentFactory {
  @Nonnull
  public static DiffContentFactory getInstance() {
    return Application.get().getInstance(DiffContentFactory.class);
  }

  @Nonnull
  public abstract EmptyContent createEmpty();


  @Nonnull
  public abstract DocumentContent create(@Nonnull String text);

  @Nonnull
  public abstract DocumentContent create(@Nonnull String text, @Nullable FileType type);

  @Nonnull
  public abstract DocumentContent create(@Nonnull String text, @Nullable FileType type, boolean respectLineSeparators);

  @Nonnull
  public abstract DocumentContent create(@Nonnull String text, @Nullable VirtualFile highlightFile);

  @Nonnull
  public abstract DocumentContent create(@Nonnull String text, @Nullable DocumentContent referent);


  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull String text);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull String text, @Nullable FileType type);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull String text, @Nullable FileType type,
                                         boolean respectLineSeparators);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull String text, @Nullable VirtualFile highlightFile);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull String text, @Nullable DocumentContent referent);


  @Nonnull
  public abstract DocumentContent create(@Nonnull Document document, @Nullable DocumentContent referent);


  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull Document document);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull Document document, @Nullable FileType fileType);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull Document document, @Nullable VirtualFile file);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull Document document, @Nullable DocumentContent referent);


  @Nonnull
  public abstract DiffContent create(@Nullable Project project, @Nonnull VirtualFile file);

  @Nullable
  public abstract DocumentContent createDocument(@Nullable Project project, @Nonnull VirtualFile file);

  @Nullable
  public abstract FileContent createFile(@Nullable Project project, @Nonnull VirtualFile file);


  @Nonnull
  public abstract DocumentContent createFragment(@Nullable Project project, @Nonnull Document document, @Nonnull TextRange range);

  @Nonnull
  public abstract DocumentContent createFragment(@Nullable Project project, @Nonnull DocumentContent content, @Nonnull TextRange range);


  @Nonnull
  public abstract DiffContent createClipboardContent();

  @Nonnull
  public abstract DocumentContent createClipboardContent(@Nullable DocumentContent referent);

  @Nonnull
  public abstract DiffContent createClipboardContent(@Nullable Project project);

  @Nonnull
  public abstract DocumentContent createClipboardContent(@Nullable Project project, @Nullable DocumentContent referent);


  @Nonnull
  public abstract DiffContent createFromBytes(@Nullable Project project,
                                              @Nonnull byte[] content,
                                              @Nonnull VirtualFile highlightFile) throws IOException;

  @Nonnull
  public abstract DiffContent createBinary(@Nullable Project project,
                                           @Nonnull byte[] content,
                                           @Nonnull FileType type,
                                           @Nonnull String fileName) throws IOException;


  @Nonnull
  @Deprecated
  public DiffContent createFromBytes(@Nullable Project project,
                                     @Nonnull VirtualFile highlightFile,
                                     @Nonnull byte[] content) throws IOException {
    return createFromBytes(project, content, highlightFile);
  }

  @Nonnull
  @Deprecated
  public DiffContent createBinary(@Nullable Project project,
                                  @Nonnull String fileName,
                                  @Nonnull FileType type,
                                  @Nonnull byte[] content) throws IOException {
    return createBinary(project, content, type, fileName);
  }
}
