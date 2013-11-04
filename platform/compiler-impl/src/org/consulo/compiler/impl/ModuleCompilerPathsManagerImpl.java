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
package org.consulo.compiler.impl;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import org.consulo.compiler.ModuleCompilerPathsManager;
import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 18:28/20.10.13
 */
@Logger
@State(
  name = "ModuleCompilerPathsManager",
  storages = {@Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)})
public class ModuleCompilerPathsManagerImpl extends ModuleCompilerPathsManager implements PersistentStateComponent<Element>, Disposable {
  @NonNls
  private static final String MODULE_OUTPUT_TAG = "module";
  @NonNls
  private static final String EXCLUDE = "exclude";
  @NonNls
  private static final String NAME = "name";
  @NonNls
  private static final String URL = "url";
  @NonNls
  private static final String TYPE = "type";
  @NonNls
  private static final String OUTPUT_TAG = "output";

  private final Module myModule;

  private boolean myInheritOutput = true;
  private boolean myExcludeOutput = true;

  @NotNull
  private Map<String, VirtualFilePointer> myVirtualFilePointers =
    new LinkedHashMap<String, VirtualFilePointer>();

  public ModuleCompilerPathsManagerImpl(Module module) {
    myModule = module;
  }

  @Override
  public boolean isInheritedCompilerOutput() {
    return myInheritOutput;
  }

  @Override
  public void setInheritedCompilerOutput(boolean val) {
    myInheritOutput = val;
  }

  @Override
  public boolean isExcludeOutput() {
    return myExcludeOutput;
  }

  @Override
  public void setExcludeOutput(boolean val) {
    myExcludeOutput = val;
  }

  @Override
  public void setCompilerOutputUrl(@NotNull ContentFolderTypeProvider contentFolderType, @Nullable String compilerOutputUrl) {
    if (myInheritOutput) {
      throw new IllegalArgumentException();
    }

    myVirtualFilePointers.put(contentFolderType.getId(), VirtualFilePointerManager.getInstance().create(compilerOutputUrl, this, null));
  }

  @Override
  @Nullable
  public String getCompilerOutputUrl(@NotNull ContentFolderTypeProvider contentFolderType) {
    if (myInheritOutput) {
      CompilerConfiguration compilerManager = CompilerConfiguration.getInstance(myModule.getProject());

      final String backUrl =
        compilerManager.getCompilerOutputUrl() + "/" + contentFolderType.getId().toLowerCase() + "/" + myModule.getName();
      VirtualFile compilerOutput = compilerManager.getCompilerOutput();
      if (compilerOutput == null) {
        return backUrl;
      }
      VirtualFile outDir = compilerOutput.findFileByRelativePath(contentFolderType.getId().toLowerCase() + "/" + myModule.getName());
      return outDir != null ? outDir.getUrl() : backUrl;
    }
    else {
      VirtualFilePointer virtualFilePointer = myVirtualFilePointers.get(contentFolderType.getId());
      assert virtualFilePointer != null;
      return virtualFilePointer.getUrl();
    }
  }

  @Nullable
  @Override
  public VirtualFile getCompilerOutput(@NotNull ContentFolderTypeProvider contentFolderType) {
    if (myInheritOutput) {
      CompilerConfiguration compilerManager = CompilerConfiguration.getInstance(myModule.getProject());

      VirtualFile compilerOutput = compilerManager.getCompilerOutput();
      if (compilerOutput == null) {
        return null;
      }
      VirtualFile outDir = compilerOutput.findFileByRelativePath(contentFolderType.getId().toLowerCase() + "/" + myModule.getName());
      return outDir != null ? outDir : null;
    }
    else {
      VirtualFilePointer virtualFilePointer = myVirtualFilePointers.get(contentFolderType.getId());
      assert virtualFilePointer != null;
      return virtualFilePointer.getFile();
    }
  }

  @NotNull
  @Override
  public VirtualFilePointer getCompilerOutputPointer(@NotNull ContentFolderTypeProvider contentFolderType) {
    if (myInheritOutput) {
      throw new IllegalArgumentException("Then module is inherit output dir - output virtual file pointer not exists");
    }
    else {
      VirtualFilePointer virtualFilePointer = myVirtualFilePointers.get(contentFolderType.getId());
      assert virtualFilePointer != null;
      return virtualFilePointer;
    }
  }

  @Nullable
  @Override
  public Element getState() {
    if (myInheritOutput) {
      return null;
    }

    Element element = new Element("state");
    Element moduleElement = new Element(MODULE_OUTPUT_TAG);
    moduleElement.setAttribute(NAME, myModule.getName());
    moduleElement.setAttribute(EXCLUDE, String.valueOf(isExcludeOutput()));

    for (Map.Entry<String, VirtualFilePointer> tempEntry : myVirtualFilePointers.entrySet()) {
      final Element elementForOutput = createElementForOutput(tempEntry.getValue());
      elementForOutput.setAttribute(TYPE, tempEntry.getKey());
      moduleElement.addContent(elementForOutput);
    }

    element.addContent(moduleElement);

    return element;
  }

  private static Element createElementForOutput(VirtualFilePointer virtualFilePointer) {
    final Element pathElement = new Element(OUTPUT_TAG);
    pathElement.setAttribute(URL, virtualFilePointer.getUrl());
    return pathElement;
  }


  @Override
  public void loadState(Element element) {
    for (Element child : element.getChildren(MODULE_OUTPUT_TAG)) {
      final String name = child.getAttributeValue(NAME);
      if (name == null || !Comparing.equal(myModule.getName(), name)) {
        continue;
      }
      myInheritOutput = false;
      myExcludeOutput = Boolean.valueOf(child.getAttributeValue(EXCLUDE));
      for (Element child2 : child.getChildren()) {
        final String moduleUrl = child2.getAttributeValue(URL);
        final String type = child2.getAttributeValue(TYPE);

        myVirtualFilePointers.put(type, VirtualFilePointerManager.getInstance().create(moduleUrl, this, null));
      }
    }
  }

  @Override
  public void dispose() {
  }
}
