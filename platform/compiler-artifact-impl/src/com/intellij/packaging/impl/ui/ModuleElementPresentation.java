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
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.roots.ui.configuration.ContentFolderIconUtil;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.packaging.ui.TreeNodePresentation;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class ModuleElementPresentation extends TreeNodePresentation {
  private final ModulePointer myModulePointer;
  private final ArtifactEditorContext myContext;
  private final ContentFolderType myContentFolderType;

  public ModuleElementPresentation(@Nullable ModulePointer modulePointer, @NotNull ArtifactEditorContext context, final ContentFolderType contentFolderType) {
    myModulePointer = modulePointer;
    myContext = context;
    myContentFolderType = contentFolderType;
  }

  @Override
  public String getPresentableName() {
    return myModulePointer != null ? myModulePointer.getModuleName() : "<unknown>";
  }

  @Override
  public boolean canNavigateToSource() {
    return findModule() != null;
  }

  @Nullable
  private Module findModule() {
    return myModulePointer != null ? myModulePointer.getModule() : null;
  }

  @Override
  public void navigateToSource() {
    final Module module = findModule();
    if (module != null) {
      myContext.selectModule(module);
    }
  }

  @Override
  public void render(@NotNull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    final Module module = findModule();
    presentationData.setIcon(ContentFolderIconUtil.getRootIcon(myContentFolderType));

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
      moduleName = myModulePointer.getModuleName();
    }
    else {
      moduleName = "<unknown>";
    }

    String text;
    switch (myContentFolderType) {

      case SOURCE:
        text = CompilerBundle.message("node.text.0.compile.output", moduleName);
        break;
      case TEST:
        text = CompilerBundle.message("node.text.0.test.compile.output", moduleName);
        break;
      case RESOURCE:
        text = CompilerBundle.message("node.text.0.resource.compile.output", moduleName);
        break;
      default:
        throw new IllegalArgumentException();
    }
    presentationData.addText(text, module != null ? mainAttributes : SimpleTextAttributes.ERROR_ATTRIBUTES);
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.MODULE;
  }
}
