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
package consulo.language.impl.internal.util.proximity;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.util.proximity.ProximityLocation;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.util.proximity.ProximityWeigher;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author peter
*/
@ExtensionImpl(id = "sdkOrLibrary", order = "after sameModule")
public class SdkOrLibraryWeigher extends ProximityWeigher {

  @Override
  public Comparable weigh(@Nonnull PsiElement element, @Nonnull ProximityLocation location) {
    Project project = location.getProject();
    return project == null ? null : isSdkElement(element, project);
  }

  public static boolean isSdkElement(PsiElement element, @Nonnull Project project) {
    VirtualFile file = PsiUtilCore.getVirtualFile(element);
    if (file != null) {
      List<OrderEntry> orderEntries = ProjectRootManager.getInstance(project).getFileIndex().getOrderEntriesForFile(file);
      if (!orderEntries.isEmpty() && orderEntries.get(0) instanceof ModuleExtensionWithSdkOrderEntry) {
        return true;
      }
    }
    return false;
  }
}
