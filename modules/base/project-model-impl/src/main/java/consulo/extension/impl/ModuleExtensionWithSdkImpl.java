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
package consulo.extension.impl;

import consulo.annotation.DeprecationInfo;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.roots.ModuleRootLayer;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12:42/19.05.13
 */
@Deprecated
@DeprecationInfo("Use consulo.module.extension.impl.ModuleExtensionWithSdkImpl")
public abstract class ModuleExtensionWithSdkImpl<T extends ModuleExtensionWithSdk<T>> extends consulo.module.extension.impl.ModuleExtensionWithSdkImpl<T> {
  public ModuleExtensionWithSdkImpl(@Nonnull String id, @Nonnull ModuleRootLayer rootModel) {
    super(id, rootModel);
  }
}
