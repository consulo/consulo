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
package consulo.compiler.artifact.ui;

import consulo.application.AllIcons;
import consulo.compiler.CompilerBundle;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class DirectoryCopyPresentation extends PackagingElementPresentation {
  private final String mySourcePath;
  private final String mySourceFileName;
  private final VirtualFile myFile;

  public DirectoryCopyPresentation(String filePath) {
    mySourceFileName = PathUtil.getFileName(filePath);

    String parentPath;
    myFile = LocalFileSystem.getInstance().findFileByPath(filePath);
    if (myFile != null) {
      VirtualFile parent = myFile.getParent();
      parentPath = parent != null ? FileUtil.toSystemDependentName(parent.getPath()) : "";
    }
    else {
      parentPath = FileUtil.toSystemDependentName(PathUtil.getParentPath(filePath));
    }

    mySourcePath = parentPath;
  }

  public String getPresentableName() {
    return mySourceFileName;
  }

  public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    presentationData.setIcon(AllIcons.Nodes.CopyOfFolder);
    if (myFile == null || !myFile.isDirectory()) {
      mainAttributes = SimpleTextAttributes.ERROR_ATTRIBUTES;
      VirtualFile parentFile = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(mySourcePath));
      if (parentFile == null) {
        commentAttributes = SimpleTextAttributes.ERROR_ATTRIBUTES;
      }
    }
    presentationData.addText(CompilerBundle.message("node.text.0.directory.content", mySourceFileName), mainAttributes);
    presentationData.addText(" (" + mySourcePath + ")", commentAttributes);
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.DIRECTORY_COPY;
  }
}
