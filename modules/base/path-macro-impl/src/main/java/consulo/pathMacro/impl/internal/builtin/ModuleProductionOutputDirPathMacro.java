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
package consulo.pathMacro.impl.internal.builtin;

import consulo.pathMacro.PathMacroBundle;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.content.ProductionContentFolderTypeProvider;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18.08.14
 */
public class ModuleProductionOutputDirPathMacro extends ModuleOutputDirPathMacro {
  @Nonnull
  @Override
  public ContentFolderTypeProvider getContentFolderTypeProvider() {
    return ProductionContentFolderTypeProvider.getInstance();
  }

  @Override
  public String getName() {
    return "ModuleProductionOutputDirPath";
  }

  @Override
  public String getDescription() {
    return PathMacroBundle.message("macro.module.production.output.dir.path");
  }
}
