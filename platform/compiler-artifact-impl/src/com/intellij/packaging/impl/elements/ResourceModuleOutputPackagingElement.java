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
package com.intellij.packaging.impl.elements;

import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 9:56/31.05.13
 */
public class ResourceModuleOutputPackagingElement extends ModuleOutputPackagingElementBase {
  public ResourceModuleOutputPackagingElement(@NotNull Project project) {
    super(ResourceModuleOutputElementType.ELEMENT_TYPE, project);
  }

  public ResourceModuleOutputPackagingElement(@NotNull Project project, @NotNull ModulePointer modulePointer) {
    super(ResourceModuleOutputElementType.ELEMENT_TYPE, project, modulePointer);
  }

  @Override
  protected String getModuleOutputAntProperty(ArtifactAntGenerationContext generationContext) {
    return generationContext.getModuleOutputPath(myModulePointer.getModuleName());
  }

  @Override
  protected ContentFolderType getContentFolderType() {
    return ContentFolderType.RESOURCE;
  }

}
