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
package consulo.module.impl.internal.layer.orderEntry;

import consulo.annotation.component.ExtensionImpl;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.util.xml.serializer.InvalidDataException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2014-08-21
 */
@ExtensionImpl
public class ModuleSourceOrderEntryType implements OrderEntryType<ModuleSourceOrderEntryImpl> {
    public static final String ID = "sourceFolder";

    @Nonnull
    public static ModuleSourceOrderEntryType getInstance() {
        return EP_NAME.findExtensionOrFail(ModuleSourceOrderEntryType.class);
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public ModuleSourceOrderEntryImpl loadOrderEntry(
        @Nonnull Element element,
        @Nonnull ModuleRootLayer moduleRootLayer
    ) throws InvalidDataException {
        return new ModuleSourceOrderEntryImpl((ModuleRootLayerImpl)moduleRootLayer);
    }

    @Override
    public void storeOrderEntry(@Nonnull Element element, @Nonnull ModuleSourceOrderEntryImpl orderEntry) {
    }
}
