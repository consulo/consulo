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
package consulo.ide.impl.idea.compiler.impl;

import consulo.compiler.scope.CompileScope;
import consulo.compiler.scope.CompilerContentIterator;
import consulo.compiler.util.ExportableUserDataHolderBase;
import consulo.logging.Logger;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.module.Module;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.project.Project;
import consulo.content.ContentIterator;
import consulo.content.FileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class OneProjectItemCompileScope extends ExportableUserDataHolderBase implements CompileScope{
  private static final Logger LOG = Logger.getInstance(OneProjectItemCompileScope.class);
  private final Project myProject;
  private final VirtualFile myFile;
  private final String myUrl;

  public OneProjectItemCompileScope(Project project, VirtualFile file) {
    myProject = project;
    myFile = file;
    final String url = file.getUrl();
    myUrl = file.isDirectory()? url + "/" : url;
  }

  @Nonnull
  public VirtualFile[] getFiles(final FileType fileType, final boolean inSourceOnly) {
    final List<VirtualFile> files = new ArrayList<VirtualFile>(1);
    final FileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    final ContentIterator iterator = new CompilerContentIterator(fileType, projectFileIndex, inSourceOnly, files);
    if (myFile.isDirectory()){
      projectFileIndex.iterateContentUnderDirectory(myFile, iterator);
    }
    else{
      iterator.processFile(myFile);
    }
    return VfsUtil.toVirtualFileArray(files);
  }

  public boolean belongs(String url) {
    if (myFile.isDirectory()){
      return FileUtil.startsWith(url, myUrl);
    }
    return FileUtil.pathsEqual(url, myUrl);
  }

  @Nonnull
  public Module[] getAffectedModules() {
    final Module module = ModuleUtil.findModuleForFile(myFile, myProject);
    if (module == null) {
      LOG.error("Module is null for file " + myFile.getPresentableUrl());
      return Module.EMPTY_ARRAY;
    }
    return new Module[] {module};
  }

}
