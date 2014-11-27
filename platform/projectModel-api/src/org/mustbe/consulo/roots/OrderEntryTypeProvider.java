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
package org.mustbe.consulo.roots;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public interface OrderEntryTypeProvider<T extends OrderEntry> {
  ExtensionPointName<OrderEntryTypeProvider> EP_NAME = ExtensionPointName.create("com.intellij.orderEntryTypeProvider");

  @NotNull
  String getId();

  @NotNull
  T loadOrderEntry(@NotNull Element element, @NotNull ModuleRootLayer moduleRootLayer) throws InvalidDataException;

  void storeOrderEntry(@NotNull Element element, @NotNull T orderEntry);

  void navigate(@NotNull T orderEntry);

  @NotNull
  CellAppearanceEx getCellAppearance(@NotNull T orderEntry);
}
