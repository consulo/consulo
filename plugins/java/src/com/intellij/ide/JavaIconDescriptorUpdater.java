/*
 * Copyright 2013 Consulo.org
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
package com.intellij.ide;

import com.intellij.codeInsight.TestFrameworks;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.psi.util.PsiMethodUtil;
import org.consulo.java.platform.util.JavaProjectRootsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 0:45/19.07.13
 */
public class JavaIconDescriptorUpdater implements IconDescriptorUpdater {
  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    if (element instanceof PsiClass) {
      if(processedFile(element, iconDescriptor)) {
        return;
      }

      if(element instanceof PsiTypeParameter) {
        iconDescriptor.setMainIcon(AllIcons.Nodes.TypeAlias);
        return;
      }
      final PsiClass psiClass = (PsiClass)element;
      if(psiClass.isEnum()) {
        iconDescriptor.setMainIcon(AllIcons.Nodes.Enum);
      }
      else if(psiClass.isAnnotationType()) {
        iconDescriptor.setMainIcon(AllIcons.Nodes.Annotationtype);
      }
      else if(psiClass.isInterface()) {
        iconDescriptor.setMainIcon(AllIcons.Nodes.Interface);
      }
      else if(psiClass instanceof PsiAnonymousClass) {
        iconDescriptor.setMainIcon(AllIcons.Nodes.AnonymousClass);
      }
      else {
        final boolean abst = psiClass.hasModifierProperty(PsiModifier.ABSTRACT);
        iconDescriptor.setMainIcon(abst ? AllIcons.Nodes.AbstractClass : AllIcons.Nodes.Class);

        if (!DumbService.getInstance(element.getProject()).isDumb()) {
          final PsiManager manager = psiClass.getManager();
          final PsiClass javaLangTrowable =
            JavaPsiFacade.getInstance(manager.getProject()).findClass(CommonClassNames.JAVA_LANG_THROWABLE, psiClass.getResolveScope());
          final boolean isException = javaLangTrowable != null && InheritanceUtil.isInheritorOrSelf(psiClass, javaLangTrowable, true);
          if (isException) {
            iconDescriptor.setMainIcon(abst ? AllIcons.Nodes.AbstractException : AllIcons.Nodes.ExceptionClass);
          }

          if (PsiClassUtil.isRunnableClass(psiClass, false) && PsiMethodUtil.findMainMethod(psiClass) != null) {
            iconDescriptor.addLayerIcon(AllIcons.Nodes.RunnableMark);
          }

          if (TestFrameworks.getInstance().isTestClass(psiClass)) {
            iconDescriptor.addLayerIcon(AllIcons.Nodes.JunitTestMark);
          }
        }
      }

      processModifierList(element, iconDescriptor, flags);
    }
    else if(element instanceof PsiJavaFile) {
      if(processedFile(element, iconDescriptor)) {
        return;
      }

      final PsiClass[] classes = ((PsiJavaFile)element).getClasses();
      if(classes.length == 1) {
        IconDescriptorUpdaters.processExistingDescriptor(iconDescriptor, classes[0], flags);
      }
    }
    else if(element instanceof PsiMethod) {
      iconDescriptor.setMainIcon(((PsiMethod)element).hasModifierProperty(PsiModifier.ABSTRACT) ? AllIcons.Nodes.AbstractMethod : AllIcons.Nodes.Method);

      processModifierList(element, iconDescriptor, flags);
    }
    else if(element instanceof PsiField) {
      iconDescriptor.setMainIcon(AllIcons.Nodes.Field);
      processModifierList(element, iconDescriptor, flags);
    }
    else if(element instanceof PsiLocalVariable) {
      iconDescriptor.setMainIcon(AllIcons.Nodes.Variable);
      processModifierList(element, iconDescriptor, flags);
    }
    else if(element instanceof PsiParameter) {
      iconDescriptor.setMainIcon(AllIcons.Nodes.Parameter);
      processModifierList(element, iconDescriptor, flags);
    }
  }

  public static void processModifierList(PsiElement element, IconDescriptor iconDescriptor, int flags) {
    PsiModifierListOwner owner = (PsiModifierListOwner) element;

    if(owner.hasModifierProperty(PsiModifier.FINAL)) {
      iconDescriptor.addLayerIcon(AllIcons.Nodes.FinalMark);
    }

    if(owner.hasModifierProperty(PsiModifier.STATIC)) {
      iconDescriptor.addLayerIcon(AllIcons.Nodes.StaticMark);
    }

    if((flags & Iconable.ICON_FLAG_VISIBILITY) != 0) {
      if (owner.hasModifierProperty(PsiModifier.PUBLIC)) {
        iconDescriptor.setRightIcon(AllIcons.Nodes.C_public);
      }
      else if (owner.hasModifierProperty(PsiModifier.PRIVATE)) {
        iconDescriptor.setRightIcon(AllIcons.Nodes.C_private);
      }
      else if (owner.hasModifierProperty(PsiModifier.PROTECTED)) {
        iconDescriptor.setRightIcon(AllIcons.Nodes.C_protected);
      }
      else if (owner.hasModifierProperty(PsiModifier.PACKAGE_LOCAL)) {
        iconDescriptor.setRightIcon(AllIcons.Nodes.C_plocal);
      }
    }
  }

  private static boolean processedFile(PsiElement element, IconDescriptor iconDescriptor) {
    final PsiFile containingFile = element.getContainingFile();
    if(containingFile == null) {
      return false;
    }
    final FileType fileType = containingFile.getFileType();
    if (fileType != JavaFileType.INSTANCE && fileType != JavaClassFileType.INSTANCE) {
      return false;
    }

    final VirtualFile virtualFile = containingFile.getVirtualFile();
    if(virtualFile == null) {
      return false;
    }
    if (!JavaProjectRootsUtil.isJavaSourceFile(element.getProject(), virtualFile, true)) {
      iconDescriptor.setMainIcon(AllIcons.FileTypes.JavaOutsideSource);
      return true;
    }
    return false;
  }
}
