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
import consulo.pathMacro.PathMacroBundle;

import java.io.File;

public final class FileDirRelativeToProjectRootMacro2 extends FileDirRelativeToProjectRootMacro {
  @Override
  public String getName() {
    return "/FileDirRelativeToProjectRoot";
  }

  @Override
  public String getDescription() {
    return PathMacroBundle.message("macro.file.dir.relative.to.root.fwd.slash");
  }

  @Override
  public String expand(DataContext dataContext) {
    String s = super.expand(dataContext);
    return s != null ? s.replace(File.separatorChar, '/') : null;
  }
}
