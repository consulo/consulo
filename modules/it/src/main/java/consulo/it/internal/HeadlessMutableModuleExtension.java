/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.disposer.Disposable;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.Component;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
public class HeadlessMutableModuleExtension extends HeadlessModuleExtension implements MutableModuleExtension<HeadlessModuleExtension> {
    public HeadlessMutableModuleExtension(String id, ModuleRootLayer rootLayer) {
        super(id, rootLayer);
    }

    @Override
    public @Nullable Component createConfigurationComponent(Disposable uiDisposable, Runnable updateOnCheck) {
        return null;
    }

    @Override
    public void setEnabled(boolean val) {
        myIsEnabled = val;
    }

    @Override
    public boolean isModified(HeadlessModuleExtension originalExtension) {
        return isModifiedImpl(originalExtension);
    }
}
