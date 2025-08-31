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

package consulo.ide.impl.idea.codeInsight.hint;

import consulo.application.AllIcons;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.util.List;

public class ElementLocationUtil {

  private ElementLocationUtil() {
  }

  public static void customizeElementLabel(PsiElement element, JLabel label) {
    if (element != null) {
      PsiFile file = element.getContainingFile();
      VirtualFile vfile = file == null ? null : file.getVirtualFile();

      if (vfile == null) {
        label.setText("");
        label.setIcon(null);

        return;
      }

      ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
      Module module = fileIndex.getModuleForFile(vfile);

      if (module != null) {
        label.setText(module.getName());
        label.setIcon(TargetAWT.to(AllIcons.Nodes.Module));
      }
      else {
        List<OrderEntry> entries = fileIndex.getOrderEntriesForFile(vfile);

        OrderEntry entry = null;

        for (OrderEntry order : entries) {
          if (order instanceof LibraryOrderEntry || order instanceof ModuleExtensionWithSdkOrderEntry) {
            entry = order;
            break;
          }
        }

        if (entry != null) {
          label.setText(entry.getPresentableName());
          label.setIcon(TargetAWT.to(AllIcons.Nodes.PpLibFolder));
        }
      }
    }
  }
}
