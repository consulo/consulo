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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.scratch.RootType;
import consulo.language.editor.scratch.ScratchFileService;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

@ExtensionImpl
public class ScratchIndexSetContributor extends IndexableSetContributor {
  @Nonnull
  @Override
  public Set<VirtualFile> getAdditionalRootsToIndex() {
    ScratchFileService instance = ScratchFileService.getInstance();
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    HashSet<VirtualFile> result = new HashSet<>();
    for (RootType rootType : RootType.getAllRootTypes()) {
      if (rootType.isHidden()) continue;
      ContainerUtil.addIfNotNull(result, fileSystem.findFileByPath(instance.getRootPath(rootType)));
    }
    return result;
  }
}
