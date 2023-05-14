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

package consulo.module.impl.internal.layer.orderEntry;

import consulo.content.OrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.orderEntry.ModuleSourceOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.project.ProjectBundle;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;

/**
 *  @author dsl
 */
public class ModuleSourceOrderEntryImpl extends OrderEntryBaseImpl implements ModuleSourceOrderEntry, ClonableOrderEntry {
  public ModuleSourceOrderEntryImpl(ModuleRootLayerImpl rootModel) {
    super(ModuleSourceOrderEntryType.getInstance(), rootModel);
  }

  @Override
  public boolean isValid() {
    return !isDisposed();
  }

  @Override
  @Nonnull
  public Module getOwnerModule() {
    return myModuleRootLayer.getModule();
  }

  @Override
  public <R> R accept(RootPolicy<R> policy, R initialValue) {
    return policy.visitModuleSourceOrderEntry(this, initialValue);
  }

  @Override
  public boolean isEquivalentTo(@Nonnull OrderEntry other) {
    return other instanceof ModuleSourceOrderEntry;
  }

  @Override
  @Nonnull
  public String getPresentableName() {
    return ProjectBundle.message("project.root.module.source");
  }

  @Override
  @Nonnull
  public VirtualFile[] getFiles(OrderRootType type) {
    if (type == SourcesOrderRootType.getInstance()) {
      return myModuleRootLayer.getContentFolderFiles(LanguageContentFolderScopes.productionAndTest());
    }
    return VirtualFile.EMPTY_ARRAY;
  }

  @Override
  @Nonnull
  public String[] getUrls(OrderRootType type) {
    final ArrayList<String> result = new ArrayList<String>();
    if (type == SourcesOrderRootType.getInstance()) {
      final ContentEntry[] content = myModuleRootLayer.getContentEntries();
      for (ContentEntry contentEntry : content) {
        Collections.addAll(result, contentEntry.getFolderUrls(LanguageContentFolderScopes.productionAndTest()));
      }
      return ArrayUtil.toStringArray(result);
    }
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Override
  public OrderEntry cloneEntry(ModuleRootLayerImpl rootModel) {
    return new ModuleSourceOrderEntryImpl(rootModel);
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }
}
