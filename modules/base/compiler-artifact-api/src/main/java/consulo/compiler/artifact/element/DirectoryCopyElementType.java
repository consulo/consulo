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
import consulo.fileChooser.FileChooserDescriptorFactory;
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
public class DirectoryCopyElementType extends PackagingElementType<DirectoryCopyPackagingElement> {
  public static DirectoryCopyElementType getInstance() {
    return getInstance(DirectoryCopyElementType.class);
  }

  public DirectoryCopyElementType() {
    super("dir-copy", LocalizeValue.localizeTODO("Directory Content"));
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.CopyOfFolder;
  }

  @Nonnull
  public List<? extends DirectoryCopyPackagingElement> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact,
                                                                       @Nonnull CompositePackagingElement<?> parent) {
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleFoldersDescriptor();
    VirtualFile[] files = IdeaFileChooser.chooseFiles(descriptor, context.getProject(), null);
    List<DirectoryCopyPackagingElement> list = new ArrayList<DirectoryCopyPackagingElement>();
    for (VirtualFile file : files) {
      list.add(new DirectoryCopyPackagingElement(file.getPath()));
    }
    return list;
  }

  @Nonnull
  public DirectoryCopyPackagingElement createEmpty(@Nonnull Project project) {
    return new DirectoryCopyPackagingElement();
  }
}
