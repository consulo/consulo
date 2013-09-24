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
package com.intellij.util.xml;

import com.intellij.ide.IconDescriptor;
import com.intellij.ide.IconDescriptorUpdater;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 1:16/19.07.13
 */
public class DomIconDescriptorUpdater implements IconDescriptorUpdater {
  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    if (element instanceof XmlFile) {
      DomFileDescription<?> description = DomManager.getDomManager(element.getProject()).getDomFileDescription((XmlFile)element);
      if(description != null) {
        final Icon fileIcon = description.getFileIcon(flags);
        if(fileIcon != null) {
          iconDescriptor.setMainIcon(fileIcon);
        }
      }
    }
  }
}
