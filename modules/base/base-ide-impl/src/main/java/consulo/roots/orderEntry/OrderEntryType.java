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
package consulo.roots.orderEntry;

import com.intellij.openapi.extensions.ExtensionPointName;
import consulo.roots.ModuleRootLayer;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Element;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public interface OrderEntryType<T extends OrderEntry> {
  ExtensionPointName<OrderEntryType> EP_NAME = ExtensionPointName.create("com.intellij.orderEntryType");

  @Nonnull
  String getId();

  @Nonnull
  T loadOrderEntry(@Nonnull Element element, @Nonnull ModuleRootLayer moduleRootLayer) throws InvalidDataException;

  void storeOrderEntry(@Nonnull Element element, @Nonnull T orderEntry);
}
