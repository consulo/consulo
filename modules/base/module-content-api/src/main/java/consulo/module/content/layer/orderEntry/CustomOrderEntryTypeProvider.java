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
package consulo.module.content.layer.orderEntry;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionList;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.util.xml.serializer.InvalidDataException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17-May-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CustomOrderEntryTypeProvider<M extends CustomOrderEntryModel> {
  ExtensionList<CustomOrderEntryTypeProvider, Application> EP = ExtensionList.of(CustomOrderEntryTypeProvider.class);

  @Nonnull
  String getId();

  @Nonnull
  M loadOrderEntry(@Nonnull Element element, @Nonnull ModuleRootLayer moduleRootLayer) throws InvalidDataException;

  void storeOrderEntry(@Nonnull Element element, @Nonnull M data);
}
