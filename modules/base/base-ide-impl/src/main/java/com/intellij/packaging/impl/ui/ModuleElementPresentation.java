/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.packaging.impl.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.packaging.ui.TreeNodePresentation;
import com.intellij.ui.SimpleTextAttributes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.util.pointers.NamedPointer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public class ModuleElementPresentation extends TreeNodePresentation {
  private final NamedPointer<Module> myModulePointer;
  private final ArtifactEditorContext myContext;
  private final ContentFolderTypeProvider myContentFolderType;

  public ModuleElementPresentation(@Nullable NamedPointer<Module> modulePointer,
                                   @Nonnull ArtifactEditorContext context,
                                   final ContentFolderTypeProvider contentFolderType) {
    myModulePointer = modulePointer;
    myContext = context;
    myContentFolderType = contentFolderType;
  }

  @Override
  public String getPresentableName() {
    return myModulePointer != null ? myModulePointer.getName() : "<unknown>";
  }

  @Override
  public boolean canNavigateToSource() {
    return findModule() != null;
  }

  @Nullable
  private Module findModule() {
    return myModulePointer != null ? myModulePointer.get() : null;
  }

  @Override
  public void navigateToSource() {
    final Module module = findModule();
    if (module != null) {
      myContext.selectModule(module);
    }
  }

  @Override
  public void render(@Nonnull PresentationData presentationData,
                     SimpleTextAttributes mainAttributes,
                     SimpleTextAttributes commentAttributes) {
    final Module module = findModule();
    presentationData.setIcon(myContentFolderType.getIcon());

    String moduleName;
    if (module != null) {
      moduleName = module.getName();
      final ModifiableModuleModel moduleModel = myContext.getModifiableModuleModel();
      if (moduleModel != null) {
        final String newName = moduleModel.getNewName(module);
        if (newName != null) {
          moduleName = newName;
        }
      }
    }
    else if (myModulePointer != null) {
      moduleName = myModulePointer.getName();
    }
    else {
      moduleName = "<unknown>";
    }

    presentationData
      .addText(CompilerBundle.message("node.text.0.1.compile.output", moduleName, StringUtil.toLowerCase(myContentFolderType.getName())),
               module != null ? mainAttributes : SimpleTextAttributes.ERROR_ATTRIBUTES);
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.MODULE;
  }
}
