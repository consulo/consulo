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

import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolderType;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 9:54/31.05.13
 */
public class ResourceModuleOutputElementType extends ModuleOutputElementTypeBase<ResourceModuleOutputPackagingElement> {
  public static final ResourceModuleOutputElementType ELEMENT_TYPE = new ResourceModuleOutputElementType();

  ResourceModuleOutputElementType() {
    super("module-resource-output", CompilerBundle.message("element.type.name.module.resource.output"));
  }

  @Override
  @NotNull
  public ResourceModuleOutputPackagingElement createEmpty(@NotNull Project project) {
    return new ResourceModuleOutputPackagingElement(project);
  }

  @Override
  protected ResourceModuleOutputPackagingElement createElement(@NotNull Project project, @NotNull ModulePointer pointer) {
    return new ResourceModuleOutputPackagingElement(project, pointer);
  }

  @NotNull
  @Override
  protected ContentFolderType getContentFolderType() {
    return ContentFolderType.RESOURCE;
  }
}
