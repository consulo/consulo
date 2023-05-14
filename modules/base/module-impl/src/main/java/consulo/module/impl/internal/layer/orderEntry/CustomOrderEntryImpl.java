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
package consulo.module.impl.internal.layer.orderEntry;

import consulo.content.RootProvider;
import consulo.module.content.layer.orderEntry.*;
import consulo.module.impl.internal.ProjectRootManagerImpl;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 21-May-22
 */
public class CustomOrderEntryImpl<M extends CustomOrderEntryModel> extends LibraryOrderEntryBaseImpl implements CustomOrderEntry<M>, ClonableOrderEntry {
  private final M myModel;

  public CustomOrderEntryImpl(@Nonnull OrderEntryType<?> provider, @Nonnull ModuleRootLayerImpl rootLayer, @Nonnull M data, boolean init) {
    super(provider, rootLayer, ProjectRootManagerImpl.getInstanceImpl(rootLayer.getProject()));
    myModel = data;

    if (init) {
      init();

      myProjectRootManagerImpl.addOrderWithTracking(this);
    }
  }

  @Nullable
  @Override
  public Object getEqualObject() {
    return myModel.getEqualObject();
  }

  @Override
  public boolean isEquivalentTo(@Nonnull OrderEntry other) {
    if (other instanceof CustomOrderEntry otherCustomOrderEntry) {
      CustomOrderEntryTypeWrapper<?> type = (CustomOrderEntryTypeWrapper<?>)otherCustomOrderEntry.getType();

      if (!Objects.equals(type.getId(), getType().getId())) {
        return false;
      }

      CustomOrderEntryModel otherModel = otherCustomOrderEntry.getModel();
      return myModel.isEquivalentTo(otherModel);
    }
    return false;
  }

  @Nonnull
  @Override
  public M getModel() {
    return myModel;
  }

  @Nullable
  @Override
  protected RootProvider getRootProvider() {
    return myModel.getRootProvider();
  }

  @Nonnull
  @Override
  public String getPresentableName() {
    return myModel.getPresentableName();
  }

  @Override
  public boolean isValid() {
    return myModel.isValid();
  }

  @Override
  public <R> R accept(RootPolicy<R> policy, @Nullable R initialValue) {
    return policy.visitCustomOrderEntry(this, initialValue);
  }

  @Override
  public boolean isSynthetic() {
    return myModel.isSynthetic();
  }

  @Override
  public void dispose() {
    super.dispose();

    myProjectRootManagerImpl.removeOrderWithTracking(this);
  }

  @Override
  @SuppressWarnings("unchecked")
  public OrderEntry cloneEntry(ModuleRootLayerImpl layer) {
    assert !isDisposed();

    M cloneModel = (M)myModel.clone();
    cloneModel.bind(layer);

    return new CustomOrderEntryImpl<M>(getType(), layer, cloneModel, true);
  }
}
