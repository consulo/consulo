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
package consulo.ide.newModule;

import consulo.ide.util.ZipUtil;
import consulo.logging.Logger;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public abstract class UnzipNewModuleBuilderProcessor<C extends NewModuleWizardContext> implements NewModuleBuilderProcessor<C> {
  private static final Logger LOG = Logger.getInstance(UnzipNewModuleBuilderProcessor.class);

  private final String myPath;

  public UnzipNewModuleBuilderProcessor(String path) {
    myPath = path;
  }

  protected void unzip(@Nonnull ModifiableRootModel model) {
    InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(myPath);
    if (resourceAsStream == null) {
      LOG.error("Resource by path '" + myPath + "' not found");
      return;
    }
    try {
      File tempFile = FileUtil.createTempFile("template", "zip");
      Files.copy(resourceAsStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      final VirtualFile moduleDir = model.getModule().getModuleDir();

      File outputDir = VirtualFileUtil.virtualToIoFile(moduleDir);

      ZipUtil.extract(tempFile, outputDir, null);
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }
}
