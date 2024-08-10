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
package consulo.component.store.impl.internal.storage;

import consulo.component.persist.RoamingType;
import consulo.component.persist.StateSplitterEx;
import consulo.component.store.impl.internal.PathMacrosService;
import consulo.component.store.impl.internal.StreamProvider;
import consulo.component.store.impl.internal.TrackingPathMacroSubstitutor;
import consulo.component.store.impl.internal.storage.io.IoDirectoryBasedStorage;
import consulo.component.store.impl.internal.storage.io.IoFileBasedStorage;
import consulo.component.store.impl.internal.storage.nio.PathFileBasedStorage;
import consulo.component.store.impl.internal.storage.vfs.VfsDirectoryBasedStorage;
import consulo.component.store.impl.internal.storage.vfs.VfsFileBasedStorage;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
                                                   @Nullable StateStorageListener listener,
                                                   @Nullable StreamProvider streamProvider,
                                                   boolean useXmlProlog,
                                                   @Nonnull PathMacrosService pathMacrosService) {
            return new IoFileBasedStorage(filePath, fileSpec, roamingType, pathMacroManager, rootElementName, parentDisposable, listener, streamProvider, useXmlProlog, pathMacrosService);
        }

        @Nonnull
        @Override
        public StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                        @Nonnull String dir,
                                                        @Nonnull StateSplitterEx splitter,
                                                        @Nonnull Disposable parentDisposable,
                                                        @Nullable StateStorageListener listener,
                                                        @Nonnull PathMacrosService pathMacrosService) {
            return new IoDirectoryBasedStorage(pathMacroSubstitutor, dir, splitter, parentDisposable, listener, pathMacrosService);
        }
    },
    JAVA_NIO {
        @Nonnull
        @Override
        public StateStorage createFileBasedStorage(@Nonnull String filePath,
                                                   @Nonnull String fileSpec,
                                                   @Nullable RoamingType roamingType,
                                                   @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                                   @Nonnull String rootElementName,
                                                   @Nonnull Disposable parentDisposable,
                                                   @Nullable StateStorageListener listener,
                                                   @Nullable StreamProvider streamProvider,
                                                   boolean useXmlProlog,
                                                   @Nonnull PathMacrosService pathMacrosService) {
            return new PathFileBasedStorage(filePath, fileSpec, roamingType, pathMacroManager, rootElementName, parentDisposable, listener, streamProvider, useXmlProlog, pathMacrosService);
        }

        @Nonnull
        @Override
        public StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                        @Nonnull String dir,
                                                        @Nonnull StateSplitterEx splitter,
                                                        @Nonnull Disposable parentDisposable,
                                                        @Nullable StateStorageListener listener,
                                                        @Nonnull PathMacrosService pathMacrosService) {
            return new IoDirectoryBasedStorage(pathMacroSubstitutor, dir, splitter, parentDisposable, listener, pathMacrosService);
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
                                                   @Nullable StateStorageListener listener,
                                                   @Nullable StreamProvider streamProvider,
                                                   boolean useXmlProlog,
                                                   @Nonnull PathMacrosService pathMacrosService) {
            return new VfsFileBasedStorage(filePath, fileSpec, roamingType, pathMacroManager, rootElementName, parentDisposable, listener, streamProvider, useXmlProlog, pathMacrosService);
        }

        @Nonnull
        @Override
        public StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                        @Nonnull String dir,
                                                        @Nonnull StateSplitterEx splitter,
                                                        @Nonnull Disposable parentDisposable,
                                                        @Nullable StateStorageListener listener,
                                                        @Nonnull PathMacrosService pathMacrosService) {
            return new VfsDirectoryBasedStorage(pathMacroSubstitutor, dir, splitter, parentDisposable, listener, pathMacrosService);
        }
    };

    @Nonnull
    public abstract StateStorage createFileBasedStorage(@Nonnull String filePath,
                                                        @Nonnull String fileSpec,
                                                        @Nullable RoamingType roamingType,
                                                        @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                                        @Nonnull String rootElementName,
                                                        @Nonnull Disposable parentDisposable,
                                                        @Nullable StateStorageListener listener,
                                                        @Nullable StreamProvider streamProvider,
                                                        boolean useXmlProlog,
                                                        @Nonnull PathMacrosService pathMacrosService);

    @Nonnull
    public abstract StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                             @Nonnull String dir,
                                                             @Nonnull StateSplitterEx splitter,
                                                             @Nonnull Disposable parentDisposable,
                                                             @Nullable StateStorageListener listener,
                                                             @Nonnull PathMacrosService pathMacrosService);
}
