/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import consulo.virtualFileSystem.VirtualFile;

public class FileNameWithoutAllExtensions extends FileNameMacro {
  @Override
  public String getName() {
    return "FileNameWithoutAllExtensions";
  }

  @Override
  public String getDescription() {
    return PathMacroBundle.message("macro.file.name.without.all.extensions");
  }

  @Override
  public String expand(DataContext dataContext) {
    VirtualFile file = dataContext.getData(VirtualFile.KEY);
    if (file == null) {
      return null;
    }
    String fileName = file.getName();
    int index = fileName.indexOf('.');
    return index > 0 ? fileName.substring(0, index) : fileName;
  }
}
