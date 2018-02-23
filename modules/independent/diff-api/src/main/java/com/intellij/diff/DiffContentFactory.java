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
package com.intellij.diff;

import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.contents.EmptyContent;
import com.intellij.diff.contents.FileContent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;

/*
 * Use ProgressManager.executeProcessUnderProgress() to pass modality state if needed
 */
public abstract class DiffContentFactory {
  @Nonnull
  public static DiffContentFactory getInstance() {
    return ServiceManager.getService(DiffContentFactory.class);
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
  public abstract DocumentContent create(@Nonnull String text, @javax.annotation.Nullable VirtualFile highlightFile);

  @Nonnull
  public abstract DocumentContent create(@Nonnull String text, @javax.annotation.Nullable DocumentContent referent);


  @Nonnull
  public abstract DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull String text);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull String text, @javax.annotation.Nullable FileType type);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull String text, @javax.annotation.Nullable FileType type,
                                         boolean respectLineSeparators);

  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull String text, @javax.annotation.Nullable VirtualFile highlightFile);

  @Nonnull
  public abstract DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull String text, @Nullable DocumentContent referent);


  @Nonnull
  public abstract DocumentContent create(@Nonnull Document document, @javax.annotation.Nullable DocumentContent referent);


  @Nonnull
  public abstract DocumentContent create(@Nullable Project project, @Nonnull Document document);

  @Nonnull
  public abstract DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull Document document, @javax.annotation.Nullable FileType fileType);

  @Nonnull
  public abstract DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull Document document, @javax.annotation.Nullable VirtualFile file);

  @Nonnull
  public abstract DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull Document document, @Nullable DocumentContent referent);


  @Nonnull
  public abstract DiffContent create(@javax.annotation.Nullable Project project, @Nonnull VirtualFile file);

  @javax.annotation.Nullable
  public abstract DocumentContent createDocument(@javax.annotation.Nullable Project project, @Nonnull VirtualFile file);

  @Nullable
  public abstract FileContent createFile(@javax.annotation.Nullable Project project, @Nonnull VirtualFile file);


  @Nonnull
  public abstract DocumentContent createFragment(@javax.annotation.Nullable Project project, @Nonnull Document document, @Nonnull TextRange range);

  @Nonnull
  public abstract DocumentContent createFragment(@javax.annotation.Nullable Project project, @Nonnull DocumentContent content, @Nonnull TextRange range);


  @Nonnull
  public abstract DiffContent createClipboardContent();

  @Nonnull
  public abstract DocumentContent createClipboardContent(@javax.annotation.Nullable DocumentContent referent);

  @Nonnull
  public abstract DiffContent createClipboardContent(@Nullable Project project);

  @Nonnull
  public abstract DocumentContent createClipboardContent(@Nullable Project project, @javax.annotation.Nullable DocumentContent referent);


  @Nonnull
  public abstract DiffContent createFromBytes(@javax.annotation.Nullable Project project,
                                              @Nonnull byte[] content,
                                              @Nonnull VirtualFile highlightFile) throws IOException;

  @Nonnull
  public abstract DiffContent createBinary(@javax.annotation.Nullable Project project,
                                           @Nonnull byte[] content,
                                           @Nonnull FileType type,
                                           @Nonnull String fileName) throws IOException;


  @Nonnull
  @Deprecated
  public DiffContent createFromBytes(@javax.annotation.Nullable Project project,
                                     @Nonnull VirtualFile highlightFile,
                                     @Nonnull byte[] content) throws IOException {
    return createFromBytes(project, content, highlightFile);
  }

  @Nonnull
  @Deprecated
  public DiffContent createBinary(@javax.annotation.Nullable Project project,
                                  @Nonnull String fileName,
                                  @Nonnull FileType type,
                                  @Nonnull byte[] content) throws IOException {
    return createBinary(project, content, type, fileName);
  }
}
