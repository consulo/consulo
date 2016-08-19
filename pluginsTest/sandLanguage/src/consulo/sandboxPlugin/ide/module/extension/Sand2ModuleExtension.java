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
package consulo.sandboxPlugin.ide.module.extension;

import consulo.roots.ModuleRootLayer;
import consulo.extension.impl.ModuleExtensionImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 30.08.14
 */
public class Sand2ModuleExtension extends ModuleExtensionImpl<Sand2ModuleExtension> {
  public Sand2ModuleExtension(@NotNull String id, @NotNull ModuleRootLayer moduleRootLayer) {
    super(id, moduleRootLayer);
  }
}
