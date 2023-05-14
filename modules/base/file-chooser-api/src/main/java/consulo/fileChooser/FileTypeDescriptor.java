/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.fileChooser;

import consulo.fileChooser.util.FileChooserUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Konstantin Bulenkov
 */
public class FileTypeDescriptor extends FileChooserDescriptor {

  private final List<String> myExtensions;

  public FileTypeDescriptor(String title, @Nonnull String... extensions) {
    super(true, false, false, true, false, false);
    assert extensions.length > 0 : "There should be at least one extension";
    myExtensions = Arrays.stream(extensions).map(ext -> {
      if (ext.startsWith(".")) {
        return ext;
      }
      return "." + ext;
    }).collect(Collectors.toList());

    setTitle(title);
  }

  @Override
  public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {        
    if (!showHiddenFiles && FileChooserUtil.isFileHidden(file)) {
      return false;
    }
    
    if (file.isDirectory()) {
      return true;
    }

    String name = file.getName();
    for (String extension : myExtensions) {
      if (name.endsWith(extension)) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  public boolean isFileSelectable(VirtualFile file) {
    return !file.isDirectory() && isFileVisible(file, true);
  }
}
