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

package consulo.language.editor.ui;

import consulo.application.AllIcons;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PsiElementModuleRenderer extends DefaultListCellRenderer {
  private String myText;

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    final Component listCellRendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    customizeCellRenderer(value, index, isSelected, cellHasFocus);
    return listCellRendererComponent;
  }

  @Override
  public String getText() {
    return myText;
  }

  protected void customizeCellRenderer(Object value, int index, boolean selected, boolean hasFocus) {
    myText = "";
    if (value instanceof PsiElement) {
      PsiElement element = (PsiElement)value;
      if (element.isValid()) {
        PsiFile psiFile = element.getContainingFile();
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
        boolean isInLibraries = false;
        if (psiFile != null) {
          VirtualFile vFile = psiFile.getVirtualFile();
          if (vFile != null) {
            isInLibraries = fileIndex.isInLibrarySource(vFile) || fileIndex.isInLibraryClasses(vFile);
            if (isInLibraries) {
              showLibraryLocation(fileIndex, vFile);
            }
          }
        }
        if (module != null && !isInLibraries) {
          showProjectLocation(psiFile, module, fileIndex);
        }
      }
    }

    setText(myText);
    setBorder(BorderFactory.createEmptyBorder(0, 0, 0, UIUtil.getListCellHPadding()));
    setHorizontalTextPosition(SwingConstants.LEFT);
    setBackground(selected ? UIUtil.getListSelectionBackground() : UIUtil.getListBackground());
    setForeground(selected ? UIUtil.getListSelectionForeground() : UIUtil.getInactiveTextColor());
  }

  private void showProjectLocation(PsiFile psiFile, Module module, ProjectFileIndex fileIndex) {
    boolean inTestSource = false;
    if (psiFile != null) {
      VirtualFile vFile = psiFile.getVirtualFile();
      if (vFile != null) {
        inTestSource = fileIndex.isInTestSourceContent(vFile);
      }
    }
    myText = module.getName();
    if (inTestSource) {
      setIcon(TargetAWT.to(AllIcons.Nodes.TestPackage));
    }
    else {
      setIcon(TargetAWT.to(AllIcons.Nodes.Module));
    }
  }

  private void showLibraryLocation(ProjectFileIndex fileIndex, VirtualFile vFile) {
    setIcon(TargetAWT.to(AllIcons.Nodes.PpLibFolder));
    for (OrderEntry order : fileIndex.getOrderEntriesForFile(vFile)) {
      if (order instanceof LibraryOrderEntry || order instanceof ModuleExtensionWithSdkOrderEntry) {
        myText = getPresentableName(order, vFile);
        break;
      }
    }

    myText = myText.substring(myText.lastIndexOf(File.separatorChar) + 1);
    VirtualFile archiveFile = ArchiveVfsUtil.getVirtualFileForArchive(vFile);
    if (archiveFile != null && !myText.equals(archiveFile.getName())) {
      myText += " (" + archiveFile.getName() + ")";
    }
  }

  protected String getPresentableName(final OrderEntry order, final VirtualFile vFile) {
    return order.getPresentableName();
  }
}
