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

import com.intellij.openapi.roots.impl.ModuleSourceOrderEntryImpl;
import com.intellij.openapi.util.InvalidDataException;
import consulo.roots.ModuleRootLayer;
import consulo.roots.impl.ModuleRootLayerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class ModuleSourceOrderEntryType implements OrderEntryType<ModuleSourceOrderEntryImpl> {
  @NotNull
  public static ModuleSourceOrderEntryType getInstance() {
    return EP_NAME.findExtension(ModuleSourceOrderEntryType.class);
  }

  @NotNull
  @Override
  public String getId() {
    return "sourceFolder";
  }

  @NotNull
  @Override
  public ModuleSourceOrderEntryImpl loadOrderEntry(@NotNull Element element, @NotNull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    return new ModuleSourceOrderEntryImpl((ModuleRootLayerImpl)moduleRootLayer);
  }

  @Override
  public void storeOrderEntry(@NotNull Element element, @NotNull ModuleSourceOrderEntryImpl orderEntry) {
  }
}
