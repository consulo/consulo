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
package consulo.ide.impl.idea.packaging.impl.elements;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.fileChooser.IdeaFileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementType;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
* @author nik
*/
@ExtensionImpl
public class ExtractedDirectoryElementType extends PackagingElementType<ExtractedDirectoryPackagingElement> {
  public static ExtractedDirectoryElementType getInstance() {
    return getInstance(ExtractedDirectoryElementType.class);
  }

  public ExtractedDirectoryElementType() {
    super("extracted-dir", "Extracted Directory");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.ExtractedFolder;
  }

  @Nonnull
  public List<? extends PackagingElement<?>> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact,
                                                             @Nonnull CompositePackagingElement<?> parent) {
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, true, false, true, true) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        if (file.isInLocalFileSystem() && file.isDirectory()) return false;
        return super.isFileSelectable(file);
      }
    };
    final VirtualFile[] files = IdeaFileChooser.chooseFiles(descriptor, context.getProject(), null);
    final List<PackagingElement<?>> list = new ArrayList<PackagingElement<?>>();
    final PackagingElementFactory factory = PackagingElementFactory.getInstance(context.getProject());
    for (VirtualFile file : files) {
      list.add(factory.createExtractedDirectory(file));
    }
    return list;
  }

  @Nonnull
  public ExtractedDirectoryPackagingElement createEmpty(@Nonnull Project project) {
    return new ExtractedDirectoryPackagingElement();
  }
}
