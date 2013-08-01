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
package org.jetbrains.plugins.groovy;

import com.intellij.ide.IconDescriptor;
import com.intellij.ide.IconDescriptorUpdater;
import com.intellij.ide.IconDescriptorUpdaters;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import icons.JetgroovyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.extensions.GroovyScriptTypeDetector;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

/**
 * @author VISTALL
 * @since 22:25/19.07.13
 */
public class GroovyIconDescriptorUpdater implements IconDescriptorUpdater {
  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    if (element instanceof GroovyFile) {
      GroovyFile file = (GroovyFile)element;
      final GrTypeDefinition[] typeDefinitions = file.getTypeDefinitions();
      if(typeDefinitions.length == 1) {
        IconDescriptorUpdaters.processExistingDescriptor(iconDescriptor, typeDefinitions[0], flags);
      }
      else {
        iconDescriptor.setMainIcon(GroovyScriptTypeDetector.getScriptType(file).getScriptIcon());
      }
    }
    else if(element instanceof GrTypeDefinition) {
      final GrTypeDefinition psiClass = (GrTypeDefinition)element;
      if(psiClass.isEnum()) {
        iconDescriptor.setMainIcon(JetgroovyIcons.Groovy.Enum);
      }
      else if(psiClass.isAnnotationType()) {
        iconDescriptor.setMainIcon(JetgroovyIcons.Groovy.AnnotationType);
      }
      else if(psiClass.isInterface()) {
        iconDescriptor.setMainIcon(JetgroovyIcons.Groovy.Interface);
      }
      else {
        final boolean abst = psiClass.hasModifierProperty(PsiModifier.ABSTRACT);
        iconDescriptor.setMainIcon(abst ? JetgroovyIcons.Groovy.AbstractClass : JetgroovyIcons.Groovy.Class);
      }

     // if(!DumbService.getInstance(element.getProject()).isDumb()) {
        /*if (GroovyRunnerUtil.isRunnable(psiClass)) {
          iconDescriptor.addLayerIcon(AllIcons.Nodes.RunnableMark);
        }*/
     // }
    }
  }
}
