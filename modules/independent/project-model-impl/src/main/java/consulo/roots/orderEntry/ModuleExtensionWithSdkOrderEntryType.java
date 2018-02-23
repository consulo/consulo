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

import com.intellij.openapi.roots.impl.ModuleExtensionWithSdkOrderEntryImpl;
import com.intellij.openapi.util.InvalidDataException;
import consulo.roots.ModuleRootLayer;
import consulo.roots.impl.ModuleRootLayerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class ModuleExtensionWithSdkOrderEntryType implements OrderEntryType<ModuleExtensionWithSdkOrderEntryImpl> {
  @Nonnull
  public static ModuleExtensionWithSdkOrderEntryType getInstance() {
    return EP_NAME.findExtension(ModuleExtensionWithSdkOrderEntryType.class);
  }

  @NonNls
  public static final String EXTENSION_ID_ATTRIBUTE = "extension-id";

  @Nonnull
  @Override
  public String getId() {
    return "module-extension-sdk";
  }

  @Nonnull
  @Override
  public ModuleExtensionWithSdkOrderEntryImpl loadOrderEntry(@Nonnull Element element, @Nonnull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    String moduleExtensionId = element.getAttributeValue(EXTENSION_ID_ATTRIBUTE);
    if (moduleExtensionId == null) {
      throw new InvalidDataException();
    }
    return new ModuleExtensionWithSdkOrderEntryImpl(moduleExtensionId, (ModuleRootLayerImpl)moduleRootLayer, false);
  }

  @Override
  public void storeOrderEntry(@Nonnull Element element, @Nonnull ModuleExtensionWithSdkOrderEntryImpl orderEntry) {
    element.setAttribute(EXTENSION_ID_ATTRIBUTE, orderEntry.getModuleExtensionId());
  }
}
