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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 21-May-22
 */
public class CustomOrderEntryImpl<M extends CustomOrderEntryModel> extends LibraryOrderEntryBaseImpl implements CustomOrderEntry<M>, ClonableOrderEntry {
  private final M myModel;

  public CustomOrderEntryImpl(@Nonnull OrderEntryType<?> provider, @Nonnull ModuleRootLayerImpl rootLayer, @Nonnull M data) {
    super(provider, rootLayer, ProjectRootManagerImpl.getInstanceImpl(rootLayer.getProject()));
    myModel = data;
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
  @SuppressWarnings("unchecked")
  public OrderEntry cloneEntry(ModuleRootLayerImpl layer) {
    M cloneModel = (M)myModel.clone();
    cloneModel.bind(layer);

    return new CustomOrderEntryImpl<M>(getType(), myModuleRootLayer, cloneModel);
  }
}
