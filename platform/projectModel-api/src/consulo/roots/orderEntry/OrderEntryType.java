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
package consulo.roots.orderEntry;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public interface OrderEntryType<T extends OrderEntry> {
  ExtensionPointName<OrderEntryType> EP_NAME = ExtensionPointName.create("consulo.orderEntryType");

  @NotNull
  String getId();

  @NotNull
  T loadOrderEntry(@NotNull Element element, @NotNull ModuleRootLayer moduleRootLayer) throws InvalidDataException;

  void storeOrderEntry(@NotNull Element element, @NotNull T orderEntry);
}
