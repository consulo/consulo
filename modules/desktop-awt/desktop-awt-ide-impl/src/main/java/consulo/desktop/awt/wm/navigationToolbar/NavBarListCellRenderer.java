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
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.UIUtil;
import consulo.colorScheme.TextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.colorScheme.EffectType;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.StandardColors;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import java.awt.*;

/**
* @author Konstantin Bulenkov
*/
public class NavBarListCellRenderer extends ColoredListCellRenderer {
  private final Project myProject;
  private final NavBarPanel myPanel;

  NavBarListCellRenderer(Project project, NavBarPanel panel) {
    myProject = project;
    myPanel = panel;
  }

  @Override
  protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
    setFocusBorderAroundIcon(false);
    String name = myPanel.getPresentation().getPresentableText(value);

    ColorValue color = TargetAWT.from(list.getForeground());
    boolean isProblemFile = false;
    if (value instanceof PsiElement) {
      PsiElement psiElement = (PsiElement)value;
      PsiFile psiFile = psiElement.getContainingFile();
      if (psiFile != null) {
        VirtualFile vFile = psiFile.getVirtualFile();
        if (vFile != null) {
          if (WolfTheProblemSolver.getInstance(myProject).isProblemFile(vFile)) {
            isProblemFile = true;
          }
          FileStatus status = FileStatusManager.getInstance(myProject).getStatus(vFile);
          color = status.getColor();
        }
      }
      else {
        isProblemFile = NavBarPresentation.wolfHasProblemFilesBeneath(psiElement);
      }
    }
    else if (value instanceof Module) {
      Module module = (Module)value;
      isProblemFile = WolfTheProblemSolver.getInstance(myProject).hasProblemFilesBeneath(module);
    }
    else if (value instanceof Project) {
      Module[] modules = ModuleManager.getInstance((Project)value).getModules();
      for (Module module : modules) {
        if (WolfTheProblemSolver.getInstance(myProject).hasProblemFilesBeneath(module)) {
          isProblemFile = true;
          break;
        }
      }
    }
    SimpleTextAttributes nameAttributes;
    if (isProblemFile) {
      TextAttributes attributes = new TextAttributes(color, null, StandardColors.RED, EffectType.WAVE_UNDERSCORE, Font.PLAIN);
      nameAttributes = TextAttributesUtil.fromTextAttributes(attributes);
    }
    else {
      nameAttributes = new SimpleTextAttributes(Font.PLAIN, TargetAWT.to(color));
    }
    append(name, nameAttributes);
    // manually set icon opaque to prevent background artifacts
    setIconOpaque(false);
    setIcon(myPanel.getPresentation().getIcon(value));
    setPaintFocusBorder(false);
    setBackground(selected ? UIUtil.getListSelectionBackground() : UIUtil.getListBackground());
  }
}
