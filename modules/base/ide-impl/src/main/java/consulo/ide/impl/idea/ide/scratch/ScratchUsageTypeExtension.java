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
import consulo.application.util.ConcurrentFactoryMap;
import consulo.language.editor.scratch.RootType;
import consulo.language.editor.scratch.ScratchFileService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.usage.UsageType;
import consulo.usage.UsageTypeProvider;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;
import java.util.concurrent.ConcurrentMap;

@ExtensionImpl(order = "last")
public class ScratchUsageTypeExtension implements UsageTypeProvider {
  private static final ConcurrentMap<RootType, UsageType> ourUsageTypes = ConcurrentFactoryMap.createMap(key -> new UsageType("Usage in " + key.getDisplayName()));

  @Nullable
  @Override
  public UsageType getUsageType(PsiElement element) {
    VirtualFile file = PsiUtilCore.getVirtualFile(element);
    RootType rootType = ScratchFileService.getInstance().getRootType(file);
    return rootType == null ? null : ourUsageTypes.get(rootType);
  }
}
