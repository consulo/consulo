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
package consulo.compiler.artifact.element;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.fileChooser.IdeaFileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
* @author nik
*/
@ExtensionImpl
public class FileCopyElementType extends PackagingElementType<FileCopyPackagingElement> {
  public static FileCopyElementType getInstance() {
    return getInstance(FileCopyElementType.class);
  }

  public FileCopyElementType() {
    super("file-copy", LocalizeValue.localizeTODO("File"));
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.FileTypes.Text;
  }

  @Override
  @Nonnull
  public List<? extends FileCopyPackagingElement> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact,
                                                                  @Nonnull CompositePackagingElement<?> parent) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, true, false, true);
    VirtualFile[] files = IdeaFileChooser.chooseFiles(descriptor, context.getProject(), null);
    List<FileCopyPackagingElement> list = new ArrayList<>();
    for (VirtualFile file : files) {
      list.add(new FileCopyPackagingElement(file.getPath()));
    }
    return list;
  }

  @Override
  @Nonnull
  public FileCopyPackagingElement createEmpty(@Nonnull Project project) {
    return new FileCopyPackagingElement();
  }
}
