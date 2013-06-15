/*
 * Copyright 2013 Consulo.org
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
package org.consulo.idea.util.projectWizard;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportProvider;
import org.consulo.idea.IdeaConstants;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 18:48/14.06.13
 */
public class IdeaProjectImportProvider extends ProjectImportProvider {
  public IdeaProjectImportProvider() {
    super(new IdeaProjectImportBuilder());
  }

  @Override
  public boolean canImport(VirtualFile fileOrDirectory, @Nullable Project project) {
    if (fileOrDirectory.isDirectory()) {
      return true;
    }
    else {
      return canImportFromFile(fileOrDirectory);
    }
  }

  @Override
  @Nullable
  public Icon getIconForFile(VirtualFile file) {
    final VirtualFile child = file.findChild(IdeaConstants.PROJECT_DIR);
    if(child != null) {
      return getIcon();
    }
    return null;
  }

  @Nullable
  @Override
  public String getFileSample() {
    return "<b>IntelliJ IDEA</b> project directory (.idea)";
  }
}
