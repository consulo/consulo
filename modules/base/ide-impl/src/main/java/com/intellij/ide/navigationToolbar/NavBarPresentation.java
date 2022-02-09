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
package com.intellij.ide.navigationToolbar;

import consulo.application.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import consulo.application.ApplicationManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import consulo.project.IndexNotReadyException;
import consulo.project.Project;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.application.util.function.Computable;
import consulo.util.lang.function.Condition;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.application.ui.awt.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import consulo.application.ui.awt.JBUI;
import consulo.application.AccessRule;
import consulo.ui.ex.awt.TargetAWT;
import consulo.content.bundle.SdkUtil;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarPresentation {
  private static final SimpleTextAttributes WOLFED = new SimpleTextAttributes(null, null, JBColor.red, SimpleTextAttributes.STYLE_WAVED);

  private final Project myProject;

  public NavBarPresentation(Project project) {
    myProject = project;
  }

  @SuppressWarnings("MethodMayBeStatic")
  @Nullable
  public Image getIcon(final Object object) {
    if (!NavBarModel.isValid(object)) return null;
    if (object instanceof Project) return AllIcons.Nodes.Project;
    if (object instanceof Module) return AllIcons.Nodes.Module;
    try {
      if (object instanceof PsiElement) {
        Image icon = AccessRule.read(() -> ((PsiElement)object).isValid() ? IconDescriptorUpdaters.getIcon(((PsiElement)object), 0) : null);

        if (icon != null && (icon.getHeight() > JBUI.scale(16) || icon.getWidth() > JBUI.scale(16))) {
          icon = ImageEffects.resize(icon, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
        }
        return icon;
      }
    }
    catch (IndexNotReadyException e) {
      return null;
    }
    if (object instanceof ModuleExtensionWithSdkOrderEntry) {
      return SdkUtil.getIcon(((ModuleExtensionWithSdkOrderEntry)object).getSdk());
    }
    if (object instanceof LibraryOrderEntry) return AllIcons.Nodes.PpLibFolder;
    if (object instanceof ModuleOrderEntry) return AllIcons.Nodes.Module;
    return null;
  }

  @Nonnull
  protected String getPresentableText(Object object) {
    return getPresentableText(object, false);
  }

  @Nonnull
  protected String getPresentableText(Object object, boolean forPopup) {
    String text = calcPresentableText(object, forPopup);
    return text.length() > 50 ? text.substring(0, 47) + "..." : text;
  }

  @Nonnull
  public static String calcPresentableText(Object object, boolean forPopup) {
    if (!NavBarModel.isValid(object)) {
      return IdeBundle.message("node.structureview.invalid");
    }
    for (NavBarModelExtension modelExtension : NavBarModelExtension.EP_NAME.getExtensionList()) {
      String text = modelExtension.getPresentableText(object, forPopup);
      if (text != null) return text;
    }
    return object.toString();
  }

  protected SimpleTextAttributes getTextAttributes(final Object object, final boolean selected) {
    if (!NavBarModel.isValid(object)) return SimpleTextAttributes.REGULAR_ATTRIBUTES;
    if (object instanceof PsiElement) {
      if (!ApplicationManager.getApplication().runReadAction((Computable<Boolean>)() -> ((PsiElement)object).isValid())) {
        return SimpleTextAttributes.GRAYED_ATTRIBUTES;
      }
      PsiFile psiFile = ((PsiElement)object).getContainingFile();
      if (psiFile != null) {
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        return new SimpleTextAttributes(null, selected ? null : TargetAWT.to(FileStatusManager.getInstance(myProject).getStatus(virtualFile).getColor()), JBColor.red,
                                        WolfTheProblemSolver.getInstance(myProject).isProblemFile(virtualFile) ? SimpleTextAttributes.STYLE_WAVED : SimpleTextAttributes.STYLE_PLAIN);
      }
      else {
        if (object instanceof PsiDirectory) {
          VirtualFile vDir = ((PsiDirectory)object).getVirtualFile();
          if (vDir.getParent() == null || ProjectRootsUtil.isModuleContentRoot(vDir, myProject)) {
            return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
          }
        }

        if (wolfHasProblemFilesBeneath((PsiElement)object)) {
          return WOLFED;
        }
      }
    }
    else if (object instanceof Module) {
      if (WolfTheProblemSolver.getInstance(myProject).hasProblemFilesBeneath((Module)object)) {
        return WOLFED;
      }

    }
    else if (object instanceof Project) {
      final Project project = (Project)object;
      final Module[] modules = ApplicationManager.getApplication().runReadAction(new Computable<Module[]>() {
        @Override
        public Module[] compute() {
          return ModuleManager.getInstance(project).getModules();
        }
      });
      for (Module module : modules) {
        if (WolfTheProblemSolver.getInstance(project).hasProblemFilesBeneath(module)) {
          return WOLFED;
        }
      }
    }
    return SimpleTextAttributes.REGULAR_ATTRIBUTES;
  }

  public static boolean wolfHasProblemFilesBeneath(final PsiElement scope) {
    return WolfTheProblemSolver.getInstance(scope.getProject()).hasProblemFilesBeneath(new Condition<VirtualFile>() {
      @Override
      public boolean value(final VirtualFile virtualFile) {
        if (scope instanceof PsiDirectory) {
          final PsiDirectory directory = (PsiDirectory)scope;
          if (!VfsUtil.isAncestor(directory.getVirtualFile(), virtualFile, false)) return false;
          return ModuleUtil.findModuleForFile(virtualFile, scope.getProject()) == ModuleUtil.findModuleForPsiElement(scope);
        }
        else if (scope instanceof PsiDirectoryContainer) { // TODO: remove. It doesn't look like we'll have packages in navbar ever again
          final PsiDirectory[] psiDirectories = ((PsiDirectoryContainer)scope).getDirectories();
          for (PsiDirectory directory : psiDirectories) {
            if (VfsUtil.isAncestor(directory.getVirtualFile(), virtualFile, false)) {
              return true;
            }
          }
        }
        return false;
      }
    });
  }
}
