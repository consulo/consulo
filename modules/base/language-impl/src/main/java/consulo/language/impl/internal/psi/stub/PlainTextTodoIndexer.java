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
package consulo.language.impl.internal.psi.stub;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.stub.todo.PlainTextTodoIndexerBase;
import consulo.language.psi.stub.todo.VersionedTodoIndexer;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class PlainTextTodoIndexer extends PlainTextTodoIndexerBase implements VersionedTodoIndexer {
  @Nonnull
  @Override
  public FileType getFileType() {
    return PlainTextFileType.INSTANCE;
  }
}
