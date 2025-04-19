/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BinaryFileStubBuilder {
    ExtensionPointCacheKey<BinaryFileStubBuilder, Map<FileType, BinaryFileStubBuilder>> KEY =
        ExtensionPointCacheKey.groupBy("BinaryFileStubBuilder", BinaryFileStubBuilder::getFileType);

    @Nullable
    static BinaryFileStubBuilder forFileType(FileType fileType) {
        ExtensionPoint<BinaryFileStubBuilder> extensionPoint = Application.get().getExtensionPoint(BinaryFileStubBuilder.class);
        Map<FileType, BinaryFileStubBuilder> map = extensionPoint.getOrBuildCache(KEY);
        return map.get(fileType);
    }

    @Nonnull
    FileType getFileType();

    boolean acceptsFile(VirtualFile file);

    @Nullable
    Stub buildStubTree(FileContent fileContent);

    int getStubVersion();
}