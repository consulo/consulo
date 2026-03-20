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
package consulo.module.impl.internal.layer.orderEntry;

import consulo.content.OrderRootType;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class UnknownOrderEntryImpl extends OrderEntryBaseImpl implements ClonableOrderEntry {
  public UnknownOrderEntryImpl(OrderEntryType<?> provider, ModuleRootLayerImpl rootLayer) {
    super(provider, rootLayer);
  }

  
  @Override
  public VirtualFile[] getFiles(OrderRootType type) {
    return VirtualFile.EMPTY_ARRAY;
  }

  
  @Override
  public String[] getUrls(OrderRootType rootType) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  
  @Override
  public String getPresentableName() {
    return "Unknown Order Entry. Type: " + getType().getId();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  
  @Override
  public Module getOwnerModule() {
    return getRootModel().getModule();
  }

  @Override
  public <R> R accept(RootPolicy<R> policy, @Nullable R initialValue) {
    return policy.visitOrderEntry(this, initialValue);
  }

  @Override
  public boolean isEquivalentTo(OrderEntry other) {
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
