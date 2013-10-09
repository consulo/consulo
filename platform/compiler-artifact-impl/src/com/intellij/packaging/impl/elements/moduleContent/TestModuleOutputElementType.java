/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.packaging.impl.elements.moduleContent;

import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElementBase;
import com.intellij.packaging.impl.elements.moduleContent.elementImpl.TestModuleOutputPackagingElement;
import org.consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class TestModuleOutputElementType extends ModuleOutputElementTypeBase<TestModuleOutputPackagingElement> {
  public static final TestModuleOutputElementType ELEMENT_TYPE = new TestModuleOutputElementType();

  public TestModuleOutputElementType() {
    super("module-test-output", CompilerBundle.message("element.type.name.module.test.output"));
  }

  @NotNull
  @Override
  public TestModuleOutputPackagingElement createEmpty(@NotNull Project project) {
    return new TestModuleOutputPackagingElement(project);
  }

  @Override
  public ModuleOutputPackagingElementBase createElement(@NotNull Project project, @NotNull NamedPointer<Module> pointer) {
    return new TestModuleOutputPackagingElement(project, pointer);
  }

  @NotNull
  @Override
  public ContentFolderType getContentFolderType() {
    return ContentFolderType.TEST;
  }
}
