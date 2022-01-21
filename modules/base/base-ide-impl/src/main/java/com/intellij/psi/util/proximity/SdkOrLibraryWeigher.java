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
package com.intellij.psi.util.proximity;

import consulo.project.Project;
import consulo.module.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.layer.orderEntry.OrderEntry;
import consulo.project.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.ProximityLocation;
import com.intellij.psi.util.PsiUtilCore;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author peter
*/
public class SdkOrLibraryWeigher extends ProximityWeigher {

  @Override
  public Comparable weigh(@Nonnull final PsiElement element, @Nonnull final ProximityLocation location) {
    Project project = location.getProject();
    return project == null ? null : isSdkElement(element, project);
  }

  public static boolean isSdkElement(PsiElement element, @Nonnull final Project project) {
    final VirtualFile file = PsiUtilCore.getVirtualFile(element);
    if (file != null) {
      List<OrderEntry> orderEntries = ProjectRootManager.getInstance(project).getFileIndex().getOrderEntriesForFile(file);
      if (!orderEntries.isEmpty() && orderEntries.get(0) instanceof ModuleExtensionWithSdkOrderEntry) {
        return true;
      }
    }
    return false;
  }
}
