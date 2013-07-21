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
package com.intellij.xml.util;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IconDescriptor;
import com.intellij.ide.IconDescriptorUpdater;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 22:47/19.07.13
 */
public class XmlIconDescriptorUpdater implements IconDescriptorUpdater {
  @NonNls
  private static final String XSD_FILE_EXTENSION = "xsd";
  @NonNls
  private static final String WSDL_FILE_EXTENSION = "wsdl";

  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    if (element instanceof XmlFile) {
      final VirtualFile vf = ((XmlFile)element).getVirtualFile();
      if (vf != null) {
        final String extension = vf.getExtension();

        if (XSD_FILE_EXTENSION.equals(extension)) {
          iconDescriptor.setMainIcon(AllIcons.FileTypes.XsdFile);
        }
        if (WSDL_FILE_EXTENSION.equals(extension)) {
          iconDescriptor.setMainIcon(AllIcons.FileTypes.WsdlFile);
        }
      }
    }
  }
}
