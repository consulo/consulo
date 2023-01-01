/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.psi.stub.todo;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.index.io.DataIndexer;
import consulo.language.psi.stub.FileContent;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 23-Jun-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TodoIndexer extends DataIndexer<TodoIndexEntry, Integer, FileContent> {
  ExtensionPointCacheKey<TodoIndexer, Map<FileType, TodoIndexer>> KEY = ExtensionPointCacheKey.groupBy("BinaryFileStubBuilder", TodoIndexer::getFileType);

  @Nullable
  static TodoIndexer forFileType(FileType fileType) {
    ExtensionPoint<TodoIndexer> extensionPoint = Application.get().getExtensionPoint(TodoIndexer.class);
    Map<FileType, TodoIndexer> map = extensionPoint.getOrBuildCache(KEY);
    return map.get(fileType);
  }

  @Nonnull
  FileType getFileType();
}
