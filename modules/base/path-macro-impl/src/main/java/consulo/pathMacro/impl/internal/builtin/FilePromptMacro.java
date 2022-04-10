/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.pathMacro.PathMacroBundle;
import consulo.pathMacro.PromptingMacro;
import consulo.pathMacro.SecondQueueExpandMacro;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author yole
 */
public class FilePromptMacro extends PromptingMacro implements SecondQueueExpandMacro {
  @Override
  public String getName() {
    return "FilePrompt";
  }

  @Override
  public String getDescription() {
    return "Shows a file chooser dialog";
  }

  @Override
  protected String promptUser(DataContext dataContext) {
    Project project = dataContext.getData(Project.KEY);
    final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
    final VirtualFile[] result = IdeaFileChooser.chooseFiles(descriptor, project, null);
    return result.length == 1 ? FileUtil.toSystemDependentName(result[0].getPath()) : null;
  }

  @Override
  public void cachePreview(DataContext dataContext) {
    myCachedPreview = PathMacroBundle.message("macro.fileprompt.preview");
  }
}
