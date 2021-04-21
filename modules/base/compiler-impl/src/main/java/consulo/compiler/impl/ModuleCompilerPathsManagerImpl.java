/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.LightFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import consulo.compiler.CompilerConfiguration;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.disposer.Disposable;
import consulo.roots.ContentFolderTypeProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 18:28/20.10.13
 */
@Singleton
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

  @Nonnull
  private final Map<String, VirtualFilePointer> myVirtualFilePointers = new LinkedHashMap<>();
  private final CompilerConfiguration myCompilerConfiguration;

  @Inject
  public ModuleCompilerPathsManagerImpl(Module module) {
    myModule = module;
    myCompilerConfiguration = CompilerConfiguration.getInstance(module.getProject());
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
  public void setCompilerOutputUrl(@Nonnull ContentFolderTypeProvider contentFolderType, @Nullable String compilerOutputUrl) {
    if (myInheritOutput) {
      throw new IllegalArgumentException();
    }
    if (compilerOutputUrl == null) {
      return;
    }

    myVirtualFilePointers.put(contentFolderType.getId(), VirtualFilePointerManager.getInstance().create(compilerOutputUrl, this, null));
  }

  @Override
  @Nullable
  public String getCompilerOutputUrl(@Nonnull ContentFolderTypeProvider contentFolderType) {
    if (!myInheritOutput) {
      VirtualFilePointer virtualFilePointer = myVirtualFilePointers.get(contentFolderType.getId());
      if (virtualFilePointer != null) {
        return virtualFilePointer.getUrl();
      }
    }

    String backUrl = myCompilerConfiguration.getCompilerOutputUrl() + "/" + getRelativePathForProvider(contentFolderType, myModule);

    VirtualFile compilerOutput = myCompilerConfiguration.getCompilerOutput();
    if (compilerOutput == null) {
      return backUrl;
    }
    VirtualFile outDir = compilerOutput.findFileByRelativePath(getRelativePathForProvider(contentFolderType, myModule));
    return outDir != null ? outDir.getUrl() : backUrl;
  }

  @Nullable
  @Override
  public VirtualFile getCompilerOutput(@Nonnull ContentFolderTypeProvider contentFolderType) {
    if (!myInheritOutput) {
      VirtualFilePointer virtualFilePointer = myVirtualFilePointers.get(contentFolderType.getId());
      if (virtualFilePointer != null) {
        return virtualFilePointer.getFile();
      }
    }

    VirtualFile compilerOutput = myCompilerConfiguration.getCompilerOutput();
    if (compilerOutput == null) {
      return null;
    }
    return compilerOutput.findFileByRelativePath(getRelativePathForProvider(contentFolderType, myModule));
  }

  @Nonnull
  @Override
  public VirtualFilePointer getCompilerOutputPointer(@Nonnull ContentFolderTypeProvider contentFolderType) {
    if (myInheritOutput) {
      throw new IllegalArgumentException("Then module is inherit output dir - output virtual file pointer not exists");
    }
    else {
      VirtualFilePointer virtualFilePointer = myVirtualFilePointers.get(contentFolderType.getId());
      if (virtualFilePointer != null) {
        return virtualFilePointer;
      }
      return new LightFilePointer(myCompilerConfiguration.getCompilerOutputUrl() + "/" + contentFolderType.getId().toLowerCase() + "/" + myModule.getName());
    }
  }

  @Nullable
  @Override
  public Element getState() {
    if (myInheritOutput) {
      return null;
    }

    Element moduleElement = new Element(MODULE_OUTPUT_TAG);
    moduleElement.setAttribute(NAME, myModule.getName());
    if (!isExcludeOutput()) {
      moduleElement.setAttribute(EXCLUDE, String.valueOf(isExcludeOutput()));
    }

    for (Map.Entry<String, VirtualFilePointer> tempEntry : myVirtualFilePointers.entrySet()) {
      final Element elementForOutput = createElementForOutput(tempEntry.getValue());
      elementForOutput.setAttribute(TYPE, tempEntry.getKey());
      moduleElement.addContent(elementForOutput);
    }

    return moduleElement;
  }

  private static Element createElementForOutput(VirtualFilePointer virtualFilePointer) {
    final Element pathElement = new Element(OUTPUT_TAG);
    pathElement.setAttribute(URL, virtualFilePointer.getUrl());
    return pathElement;
  }


  @Override
  public void loadState(Element element) {
    myInheritOutput = false;
    myExcludeOutput = Boolean.valueOf(element.getAttributeValue(EXCLUDE, "true"));
    for (Element child2 : element.getChildren()) {
      final String moduleUrl = child2.getAttributeValue(URL);
      final String type = child2.getAttributeValue(TYPE);

      myVirtualFilePointers.put(type, VirtualFilePointerManager.getInstance().create(moduleUrl, this, null));
    }
  }

  @Override
  public void dispose() {
  }
}
