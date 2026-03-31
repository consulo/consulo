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
import consulo.localize.LocalizeValue;
import consulo.pathMacro.Macro;
import consulo.pathMacro.localize.PathMacroLocalize;
import consulo.virtualFileSystem.VirtualFile;

public final class FileDirMacro extends Macro {
  @Override
  public String getName() {
    return "FileDir";
  }

  @Override
  public LocalizeValue getDescription() {
    return PathMacroLocalize.macroFileDirectory();
  }

  @Override
  public String expand(DataContext dataContext) {
    VirtualFile vFile = getVirtualDirOrParent(dataContext);
    return vFile != null ? getPath(vFile) : null;
  }
}
