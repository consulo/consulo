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
package org.mustbe.consulo.sandLanguage.ide.module.extension;

import com.intellij.openapi.roots.ModuleRootLayer;
import org.consulo.module.extension.impl.ModuleExtensionImpl;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.ContentFoldersSupport;
import org.mustbe.consulo.roots.impl.WebResourcesFolderTypeProvider;

/**
 * @author VISTALL
 * @since 19.03.14
 */
@ContentFoldersSupport(value = {WebResourcesFolderTypeProvider.class})
public class SandModuleExtension extends ModuleExtensionImpl<SandModuleExtension> {
  public SandModuleExtension(@NotNull String id, @NotNull ModuleRootLayer rootModel) {
    super(id, rootModel);
  }
}
