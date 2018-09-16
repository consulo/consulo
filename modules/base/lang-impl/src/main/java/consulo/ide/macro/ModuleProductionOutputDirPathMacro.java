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
package consulo.ide.macro;

import com.intellij.ide.IdeBundle;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
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
    return IdeBundle.message("macro.module.production.output.dir.path");
  }
}
