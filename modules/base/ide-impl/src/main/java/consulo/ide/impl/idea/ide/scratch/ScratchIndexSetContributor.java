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
import consulo.application.Application;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

@ExtensionImpl
public class ScratchIndexSetContributor extends IndexableSetContributor {
  private final Application myApplication;
  private final Provider<ScratchFileService> myScratchFileServiceProvider;
  private final LocalFileSystem myLocalFileSystem;

  @Inject
  public ScratchIndexSetContributor(Application application,
                                    Provider<ScratchFileService> scratchFileServiceProvider,
                                    VirtualFileManager virtualFileManager) {
    myApplication = application;
    myScratchFileServiceProvider = scratchFileServiceProvider;
    myLocalFileSystem = LocalFileSystem.get(virtualFileManager);
  }

  @Nonnull
  @Override
  public Set<VirtualFile> getAdditionalRootsToIndex() {
    ScratchFileService scratchFileService = myScratchFileServiceProvider.get();
    HashSet<VirtualFile> result = new HashSet<>();
    myApplication.getExtensionPoint(RootType.class).forEachExtensionSafe(rootType -> {
      if (rootType.isHidden()) return;
      ContainerUtil.addIfNotNull(result, myLocalFileSystem.findFileByPath(scratchFileService.getRootPath(rootType)));
    });
    return result;
  }
}
