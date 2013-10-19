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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import org.consulo.compiler.CompilerPathsManager;
import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14:20/19.10.13
 */
@Logger
@State(
  name = "DefaultCompilerPathsManager",
  storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
public class DefaultCompilerPathsManagerImpl extends CompilerPathsManager implements PersistentStateComponent<Element> {
  @NonNls
  private static final String DEFAULT_PATH = "out";

  @NonNls
  private static final String URL = "url";

  private String myDefaultUrl = DEFAULT_PATH;

  public DefaultCompilerPathsManagerImpl() {
  }

  @Nullable
  @Override
  public VirtualFile getCompilerOutput() {
    throw new IllegalArgumentException();
  }

  @Nullable
  @Override
  public String getCompilerOutputUrl() {
    return myDefaultUrl;
  }

  @Override
  public VirtualFilePointer getCompilerOutputPointer() {
    throw new IllegalArgumentException();
  }

  @Override
  public void setCompilerOutputUrl(@Nullable String compilerOutputUrl) {
    myDefaultUrl = compilerOutputUrl == null ? DEFAULT_PATH : compilerOutputUrl;
  }

  @Override
  public boolean isInheritedCompilerOutput(@NotNull Module module) {
    throw new IllegalArgumentException();
  }

  @Override
  public void setInheritedCompilerOutput(@NotNull Module module, boolean val) {
    throw new IllegalArgumentException();
  }

  @Override
  public boolean isExcludeOutput(@NotNull Module module) {
    throw new IllegalArgumentException();
  }

  @Override
  public void setExcludeOutput(@NotNull Module module, boolean val) {
    throw new IllegalArgumentException();
  }

  @Override
  public void setCompilerOutputUrl(@NotNull Module module,
                                   @NotNull ContentFolderType contentFolderType,
                                   @Nullable String compilerOutputUrl) {
    throw new IllegalArgumentException();
  }

  @Override
  public String getCompilerOutputUrl(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    throw new IllegalArgumentException();
  }

  @Nullable
  @Override
  public VirtualFile getCompilerOutput(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    throw new IllegalArgumentException();
  }

  @NotNull
  @Override
  public VirtualFilePointer getCompilerOutputPointer(@NotNull Module module, @NotNull ContentFolderType contentFolderType) {
    throw new IllegalArgumentException();
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("state");
    element.setAttribute(URL, myDefaultUrl);

    return element;
  }

  @Override
  public void loadState(Element element) {
    String url = element.getAttributeValue(URL);
    if (url != null) {

      setCompilerOutputUrl(url);
    }
  }
}
