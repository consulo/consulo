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
import consulo.annotation.component.ServiceAPI;
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

import org.jspecify.annotations.Nullable;
import java.io.IOException;

/*
 * Use ProgressManager.executeProcessUnderProgress() to pass modality state if needed
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class DiffContentFactory {
  
  public static DiffContentFactory getInstance() {
    return Application.get().getInstance(DiffContentFactory.class);
  }

  
  public abstract EmptyContent createEmpty();


  
  public abstract DocumentContent create(String text);

  
  public abstract DocumentContent create(String text, @Nullable FileType type);

  
  public abstract DocumentContent create(String text, @Nullable FileType type, boolean respectLineSeparators);

  
  public abstract DocumentContent create(String text, @Nullable VirtualFile highlightFile);

  
  public abstract DocumentContent create(String text, @Nullable DocumentContent referent);


  
  public abstract DocumentContent create(@Nullable Project project, String text);

  
  public abstract DocumentContent create(@Nullable Project project, String text, @Nullable FileType type);

  
  public abstract DocumentContent create(@Nullable Project project, String text, @Nullable FileType type,
                                         boolean respectLineSeparators);

  
  public abstract DocumentContent create(@Nullable Project project, String text, @Nullable VirtualFile highlightFile);

  
  public abstract DocumentContent create(@Nullable Project project, String text, @Nullable DocumentContent referent);


  
  public abstract DocumentContent create(Document document, @Nullable DocumentContent referent);


  
  public abstract DocumentContent create(@Nullable Project project, Document document);

  
  public abstract DocumentContent create(@Nullable Project project, Document document, @Nullable FileType fileType);

  
  public abstract DocumentContent create(@Nullable Project project, Document document, @Nullable VirtualFile file);

  
  public abstract DocumentContent create(@Nullable Project project, Document document, @Nullable DocumentContent referent);


  
  public abstract DiffContent create(@Nullable Project project, VirtualFile file);

  public abstract @Nullable DocumentContent createDocument(@Nullable Project project, VirtualFile file);

  public abstract @Nullable FileContent createFile(@Nullable Project project, VirtualFile file);


  
  public abstract DocumentContent createFragment(@Nullable Project project, Document document, TextRange range);

  
  public abstract DocumentContent createFragment(@Nullable Project project, DocumentContent content, TextRange range);


  
  public abstract DiffContent createClipboardContent();

  
  public abstract DocumentContent createClipboardContent(@Nullable DocumentContent referent);

  
  public abstract DiffContent createClipboardContent(@Nullable Project project);

  
  public abstract DocumentContent createClipboardContent(@Nullable Project project, @Nullable DocumentContent referent);


  
  public abstract DiffContent createFromBytes(@Nullable Project project,
                                              byte[] content,
                                              VirtualFile highlightFile) throws IOException;

  
  public abstract DiffContent createBinary(@Nullable Project project,
                                           byte[] content,
                                           FileType type,
                                           String fileName) throws IOException;


  
  @Deprecated
  public DiffContent createFromBytes(@Nullable Project project,
                                     VirtualFile highlightFile,
                                     byte[] content) throws IOException {
    return createFromBytes(project, content, highlightFile);
  }

  
  @Deprecated
  public DiffContent createBinary(@Nullable Project project,
                                  String fileName,
                                  FileType type,
                                  byte[] content) throws IOException {
    return createBinary(project, content, type, fileName);
  }
}
