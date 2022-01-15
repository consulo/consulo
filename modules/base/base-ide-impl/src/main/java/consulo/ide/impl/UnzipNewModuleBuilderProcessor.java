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
package consulo.ide.impl;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ZipUtil;
import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.ide.wizard.newModule.NewModuleWizardContext;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
      FileUtil.writeToFile(tempFile, FileUtil.loadBytes(resourceAsStream));
      final VirtualFile moduleDir = model.getModule().getModuleDir();

      File outputDir = VfsUtil.virtualToIoFile(moduleDir);

      ZipUtil.extract(tempFile, outputDir, null);
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }
}
