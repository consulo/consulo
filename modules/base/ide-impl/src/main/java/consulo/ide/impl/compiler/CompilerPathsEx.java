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
package consulo.ide.impl.compiler;

import consulo.compiler.CompilerPaths;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;
import java.util.Collection;

public class CompilerPathsEx extends CompilerPaths {
  public static final Key<Boolean> CLEAR_ALL_OUTPUTS_KEY = Key.create("_should_clear_all_outputs_");

  public static File getZippedOutputPath(Project project, String outputDirectoryPath) {
    final File outputDir = new File(outputDirectoryPath);
    return new File(getZipStoreDirectory(project), "_" + outputDir.getName() + Integer.toHexString(outputDirectoryPath.hashCode()) + ".zip");
  }

  public static File getZipStoreDirectory(Project project) {
    return new File(getCompilerSystemDirectory(project), ".zip");
  }

  public static class FileVisitor {
    protected void accept(final VirtualFile file, final String fileRoot, final String filePath) {
      if (file.isDirectory()) {
        acceptDirectory(file, fileRoot, filePath);
      }
      else {
        acceptFile(file, fileRoot, filePath);
      }
    }

    protected void acceptFile(VirtualFile file, String fileRoot, String filePath) {
    }

    protected void acceptDirectory(final VirtualFile file, final String fileRoot, final String filePath) {
      ProgressManager.checkCanceled();
      final VirtualFile[] children = file.getChildren();
      for (final VirtualFile child : children) {
        final String name = child.getName();
        final String _filePath;
        final StringBuilder buf = new StringBuilder();
        buf.append(filePath).append("/").append(name);
        _filePath = buf.toString();
        accept(child, fileRoot, _filePath);
      }
    }
  }

  public static void visitFiles(final Collection<VirtualFile> directories, final FileVisitor visitor) {
    for (final VirtualFile outputDir : directories) {
      ApplicationManager.getApplication().runReadAction(() -> {
        final String path = outputDir.getPath();
        visitor.accept(outputDir, path, path);
      });
    }
  }
}
