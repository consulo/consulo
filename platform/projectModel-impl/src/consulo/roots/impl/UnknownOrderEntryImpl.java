/*
 * Copyright 2013-2014 must-be.org
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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.roots.orderEntry.OrderEntryType;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class UnknownOrderEntryImpl extends OrderEntryBaseImpl implements ClonableOrderEntry {
  public UnknownOrderEntryImpl(@NotNull OrderEntryType<?> provider, @NotNull ModuleRootLayerImpl rootLayer) {
    super(provider, rootLayer);
  }

  @NotNull
  @Override
  public VirtualFile[] getFiles(OrderRootType type) {
    return VirtualFile.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public String[] getUrls(OrderRootType rootType) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Unknown Order Entry. Type: " + StringUtil.capitalize(getType().getId());
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @NotNull
  @Override
  public Module getOwnerModule() {
    return getRootModel().getModule();
  }

  @Override
  public <R> R accept(RootPolicy<R> policy, @Nullable R initialValue) {
    return policy.visitOrderEntry(this, initialValue);
  }

  @Override
  public boolean isEquivalentTo(@NotNull OrderEntry other) {
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
