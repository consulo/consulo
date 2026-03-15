/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.diff.internal;

import consulo.diff.DiffContentFactory;
import consulo.diff.DiffFilePath;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public abstract class DiffContentFactoryEx extends DiffContentFactory {
  
  public static DiffContentFactoryEx getInstanceEx() {
    return (DiffContentFactoryEx)DiffContentFactory.getInstance();
  }


  
  public abstract DocumentContent create(@Nullable Project project, String text, DiffFilePath filePath);


  
  public abstract DiffContent createFromBytes(@Nullable Project project,
                                              byte[] content,
                                              DiffFilePath filePath) throws IOException;

  
  public abstract DiffContent createFromBytes(@Nullable Project project,
                                              byte[] content,
                                              VirtualFile highlightFile) throws IOException;


  
  public abstract DocumentContent createDocumentFromBytes(@Nullable Project project,
                                                          byte[] content,
                                                          DiffFilePath filePath);

  
  public abstract DocumentContent createDocumentFromBytes(@Nullable Project project,
                                                          byte[] content,
                                                          VirtualFile highlightFile);
}
