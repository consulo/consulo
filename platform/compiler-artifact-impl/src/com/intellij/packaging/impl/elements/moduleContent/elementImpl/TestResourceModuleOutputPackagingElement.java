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
package com.intellij.packaging.impl.elements.moduleContent.elementImpl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElementBase;
import com.intellij.packaging.impl.elements.moduleContent.TestResourceModuleOutputElementType;
import org.consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 9:56/31.05.13
 */
public class TestResourceModuleOutputPackagingElement extends ModuleOutputPackagingElementBase {
  public TestResourceModuleOutputPackagingElement(@NotNull Project project) {
    super(TestResourceModuleOutputElementType.ELEMENT_TYPE, project);
  }

  public TestResourceModuleOutputPackagingElement(@NotNull Project project, @NotNull NamedPointer<Module> modulePointer) {
    super(TestResourceModuleOutputElementType.ELEMENT_TYPE, project, modulePointer);
  }

  @Override
  protected String getModuleOutputAntProperty(ArtifactAntGenerationContext generationContext) {
    return generationContext.getModuleTestOutputPath(myModulePointer.getName());
  }

  @Override
  protected ContentFolderType getContentFolderType() {
    return ContentFolderType.TEST_RESOURCE;
  }

}
