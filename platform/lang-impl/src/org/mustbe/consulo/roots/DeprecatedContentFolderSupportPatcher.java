/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.roots;

import com.intellij.openapi.roots.ModifiableRootModel;
import org.consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.DeprecationInfo;

import java.util.Set;

/**
 * @author VISTALL
 * @since 31.03.14
 */
@Deprecated
@DeprecationInfo(value = "Exists until @ContentFoldersSupport not deleted", until = "1.0")
public class DeprecatedContentFolderSupportPatcher implements ContentFolderSupportPatcher {
  @Override
  public void patch(@NotNull ModifiableRootModel model, @NotNull Set<ContentFolderTypeProvider> set) {
    for (ModuleExtension moduleExtension : model.getExtensions()) {
      ContentFoldersSupport annotation = moduleExtension.getClass().getAnnotation(ContentFoldersSupport.class);
      if(annotation == null) {
        // if extension is mutable go get super class
        annotation = moduleExtension.getClass().getSuperclass().getAnnotation(ContentFoldersSupport.class);
      }
      if(annotation != null) {
        for (Class<? extends ContentFolderTypeProvider> o : annotation.value()) {
          ContentFolderTypeProvider folderTypeProvider = ContentFolderTypeProvider.EP_NAME.findExtension(o);
          if(folderTypeProvider != null) {
            set.add(folderTypeProvider);
          }
        }
      }
    }
  }
}
