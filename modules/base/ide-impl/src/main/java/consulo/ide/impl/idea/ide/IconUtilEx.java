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
package consulo.ide.impl.idea.ide;

import consulo.application.AllIcons;
import consulo.module.Module;
import consulo.project.Project;
import consulo.component.util.Iconable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.virtualFileSystem.VfsIconUtil;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.image.Image;

public class IconUtilEx {

  public static Image getIcon(Object object, @Iconable.IconFlags int flags, Project project) {
    if (object instanceof PsiElement) {
      return IconDescriptorUpdaters.getIcon(((PsiElement)object), flags);
    }
    if (object instanceof Module) {
      return AllIcons.Nodes.Module;
    }
    if (object instanceof VirtualFile) {
      VirtualFile file = (VirtualFile)object;
      return VfsIconUtil.getIcon(file, flags, project);
    }
    return null;
  }
}
