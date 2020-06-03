/*
 * Copyright 2013-2019 consulo.io
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
package consulo.components.impl.stores.storage;

import consulo.disposer.Disposable;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.StateSplitterEx;
import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import consulo.components.impl.stores.StreamProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-13
 */
public enum StateStorageFacade {
  JAVA_IO {
    @Nonnull
    @Override
    public StateStorage createFileBasedStorage(@Nonnull String filePath,
                                               @Nonnull String fileSpec,
                                               @Nullable RoamingType roamingType,
                                               @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                               @Nonnull String rootElementName,
                                               @Nonnull Disposable parentDisposable,
                                               @Nullable StateStorage.Listener listener,
                                               @Nullable StreamProvider streamProvider,
                                               boolean useXmlProlog) {
      return new IoFileBasedStorage(filePath, fileSpec, roamingType, pathMacroManager, rootElementName, parentDisposable, listener, streamProvider, useXmlProlog);
    }

    @Nonnull
    @Override
    public StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                    @Nonnull String dir,
                                                    @Nonnull StateSplitterEx splitter,
                                                    @Nonnull Disposable parentDisposable,
                                                    @Nullable StateStorage.Listener listener) {
      return new IoDirectoryBasedStorage(pathMacroSubstitutor, dir, splitter, parentDisposable, listener);
    }
  },
  CONSULO_VFS {
    @Nonnull
    @Override
    public StateStorage createFileBasedStorage(@Nonnull String filePath,
                                               @Nonnull String fileSpec,
                                               @Nullable RoamingType roamingType,
                                               @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                               @Nonnull String rootElementName,
                                               @Nonnull Disposable parentDisposable,
                                               @Nullable StateStorage.Listener listener,
                                               @Nullable StreamProvider streamProvider,
                                               boolean useXmlProlog) {
      return new VfsFileBasedStorage(filePath, fileSpec, roamingType, pathMacroManager, rootElementName, parentDisposable, listener, streamProvider, useXmlProlog);
    }

    @Nonnull
    @Override
    public StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                    @Nonnull String dir,
                                                    @Nonnull StateSplitterEx splitter,
                                                    @Nonnull Disposable parentDisposable,
                                                    @Nullable StateStorage.Listener listener) {
      return new VfsDirectoryBasedStorage(pathMacroSubstitutor, dir, splitter, parentDisposable, listener);
    }
  };

  @Nonnull
  public abstract StateStorage createFileBasedStorage(@Nonnull String filePath,
                                                      @Nonnull String fileSpec,
                                                      @Nullable RoamingType roamingType,
                                                      @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                                      @Nonnull String rootElementName,
                                                      @Nonnull Disposable parentDisposable,
                                                      @Nullable StateStorage.Listener listener,
                                                      @Nullable StreamProvider streamProvider,
                                                      boolean useXmlProlog);

  @Nonnull
  public abstract StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                           @Nonnull String dir,
                                                           @Nonnull StateSplitterEx splitter,
                                                           @Nonnull Disposable parentDisposable,
                                                           @Nullable StateStorage.Listener listener);}
