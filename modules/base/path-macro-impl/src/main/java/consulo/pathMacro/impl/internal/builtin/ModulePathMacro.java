/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.dataContext.DataContext;
import consulo.module.Module;
import consulo.module.content.layer.OrderEnumerator;
import consulo.pathMacro.Macro;
import consulo.pathMacro.PathMacroBundle;

public final class ModulePathMacro extends Macro {
  @Override
  public String getName() {
    return "ModuleSourcePath";
  }

  @Override
  public String getDescription() {
    return PathMacroBundle.message("macro.module.source.path");
  }

  @Override
  public String expand(DataContext dataContext) {
    final Module module = dataContext.getData(Module.KEY);
    if (module == null) {
      return null;
    }
    return OrderEnumerator.orderEntries(module).withoutSdk().withoutLibraries().getSourcePathsList().getPathsString();
  }
}
