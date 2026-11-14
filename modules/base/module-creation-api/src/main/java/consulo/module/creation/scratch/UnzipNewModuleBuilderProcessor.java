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
package consulo.module.creation.scratch;

import consulo.logging.Logger;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.creation.NewModuleWizardContext;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

  /**
   * Extracts the template into the module directory - on the machine of the module.
   */
  protected void unzip(ModifiableRootModel model) {
    VirtualFile moduleDir = model.getModule().getModuleDir();
    if (moduleDir == null) {
      LOG.error("Module '" + model.getModule().getName() + "' has no directory");
      return;
    }
    try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(myPath)) {
      if (resourceAsStream == null) {
        LOG.error("Resource by path '" + myPath + "' not found");
        return;
      }
      Path outputDir = moduleDir.toNioPath();
      try (ZipInputStream zip = new ZipInputStream(resourceAsStream)) {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
          Path target = outputDir.resolve(entry.getName()).normalize();
          if (!target.startsWith(outputDir)) {
            continue;
          }
          if (entry.isDirectory()) {
            Files.createDirectories(target);
          }
          else {
            Path parent = target.getParent();
            if (parent != null) {
              Files.createDirectories(parent);
            }
            Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }
}
