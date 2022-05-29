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

import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.orderEntry.CustomOrderEntryModel;
import consulo.module.content.layer.orderEntry.CustomOrderEntryTypeProvider;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.util.xml.serializer.InvalidDataException;
import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21-May-22
 */
public class CustomOrderEntryTypeWrapper<M extends CustomOrderEntryModel> implements OrderEntryType<CustomOrderEntryImpl<M>> {
  private CustomOrderEntryTypeProvider<M> myCustomOrderEntryTypeProvider;

  public CustomOrderEntryTypeWrapper(@Nonnull CustomOrderEntryTypeProvider<M> customOrderEntryTypeProvider) {
    myCustomOrderEntryTypeProvider = customOrderEntryTypeProvider;
  }

  public CustomOrderEntryTypeProvider<M> getCustomOrderEntryTypeProvider() {
    return myCustomOrderEntryTypeProvider;
  }

  @Nonnull
  @Override
  public String getId() {
    return myCustomOrderEntryTypeProvider.getId();
  }

  @Nonnull
  @Override
  public CustomOrderEntryImpl<M> loadOrderEntry(@Nonnull Element element, @Nonnull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    M data = myCustomOrderEntryTypeProvider.loadOrderEntry(element, moduleRootLayer);
    data.bind(moduleRootLayer);
    return new CustomOrderEntryImpl<>(this, (ModuleRootLayerImpl) moduleRootLayer, data, false);
  }

  @Override
  public void storeOrderEntry(@Nonnull Element element, @Nonnull CustomOrderEntryImpl<M> orderEntry) {
    M data = orderEntry.getModel();

    myCustomOrderEntryTypeProvider.storeOrderEntry(element, data);
  }
}
