/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.roots.impl.ClonableOrderEntry;
import com.intellij.openapi.roots.impl.OrderEntryBaseImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import consulo.roots.orderEntry.OrderEntryType;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class UnknownOrderEntryImpl extends OrderEntryBaseImpl implements ClonableOrderEntry {
  public UnknownOrderEntryImpl(@Nonnull OrderEntryType<?> provider, @Nonnull ModuleRootLayerImpl rootLayer) {
    super(provider, rootLayer);
  }

  @Nonnull
  @Override
  public VirtualFile[] getFiles(OrderRootType type) {
    return VirtualFile.EMPTY_ARRAY;
  }

  @Nonnull
  @Override
  public String[] getUrls(OrderRootType rootType) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Nonnull
  @Override
  public String getPresentableName() {
    return "Unknown Order Entry. Type: " + getType().getId();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Nonnull
  @Override
  public Module getOwnerModule() {
    return getRootModel().getModule();
  }

  @Override
  public <R> R accept(RootPolicy<R> policy, @javax.annotation.Nullable R initialValue) {
    return policy.visitOrderEntry(this, initialValue);
  }

  @Override
  public boolean isEquivalentTo(@Nonnull OrderEntry other) {
    return getType() == other.getType();
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  public OrderEntry cloneEntry(ModuleRootLayerImpl layer) {
    return new UnknownOrderEntryImpl(getType(), layer);
  }
}
