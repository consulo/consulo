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

import consulo.content.bundle.SdkType;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionWithSdkBase;

/**
 * Carries an SDK, so that a test can give a module the SDK order entry the platform indexes SDK roots from.
 *
 * @author VISTALL
 */
public class HeadlessModuleExtension extends ModuleExtensionWithSdkBase<HeadlessModuleExtension> {
    public HeadlessModuleExtension(String id, ModuleRootLayer rootLayer) {
        super(id, rootLayer);
    }

    @Override
    public Class<? extends SdkType> getSdkTypeClass() {
        return HeadlessSdkType.class;
    }
}
