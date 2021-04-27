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
package consulo.sandboxPlugin.ide.module.extension;

import com.intellij.openapi.projectRoots.SdkType;
import consulo.module.extension.impl.ModuleExtensionWithSdkImpl;
import consulo.roots.ModuleRootLayer;
import consulo.sandboxPlugin.ide.bundle.SandBundleType;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandModuleExtension extends ModuleExtensionWithSdkImpl<SandModuleExtension> {
  public SandModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer rootModel) {
    super(id, rootModel);
  }

  @Nonnull
  @Override
  public Class<? extends SdkType> getSdkTypeClass() {
    return SandBundleType.class;
  }
}
