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
package consulo.ide.impl;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ZipUtil;
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 05.06.14
 */
@Logger
public abstract class UnzipNewModuleBuilderProcessor<T extends JComponent> implements NewModuleBuilderProcessor<T> {
  private final String myPath;

  public UnzipNewModuleBuilderProcessor(String path) {
    myPath = path;
  }

  protected void unzip(@NotNull ModifiableRootModel model) {
    InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(myPath);
    if(resourceAsStream == null) {
      UnzipNewModuleBuilderProcessor.LOGGER.error("Resource by path '" + myPath + "' not found");
      return;
    }
    try {
      File tempFile = FileUtil.createTempFile("template", "zip");
      FileUtil.writeToFile(tempFile, FileUtil.loadBytes(resourceAsStream));
      final VirtualFile moduleDir = model.getModule().getModuleDir();

      File outputDir = VfsUtil.virtualToIoFile(moduleDir);

      ZipUtil.extract(tempFile, outputDir, null);
    }
    catch (IOException e) {
      UnzipNewModuleBuilderProcessor.LOGGER.error(e);
    }
  }
}
