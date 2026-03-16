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
import consulo.component.store.internal.PathMacrosService;
import consulo.component.store.impl.internal.storage.io.IoDirectoryBasedStorage;
import consulo.component.store.impl.internal.storage.io.IoFileBasedStorage;
import consulo.component.store.impl.internal.storage.nio.PathDirectoryBasedStorage;
import consulo.component.store.impl.internal.storage.nio.PathFileBasedStorage;
import consulo.component.store.impl.internal.storage.vfs.VfsDirectoryBasedStorage;
import consulo.component.store.impl.internal.storage.vfs.VfsFileBasedStorage;
import consulo.component.store.internal.StateStorage;
import consulo.component.store.internal.StreamProvider;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.disposer.Disposable;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-13
 */
public enum StateStorageFacade {
    JAVA_IO {
        
        @Override
        public StateStorage createFileBasedStorage(String filePath,
                                                   String fileSpec,
                                                   @Nullable RoamingType roamingType,
                                                   @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                                   String rootElementName,
                                                   Disposable parentDisposable,
                                                   @Nullable StateStorageListener listener,
                                                   @Nullable StreamProvider streamProvider,
                                                   boolean useXmlProlog,
                                                   PathMacrosService pathMacrosService) {
            return new IoFileBasedStorage(filePath, fileSpec, roamingType, pathMacroManager, rootElementName, parentDisposable, listener, streamProvider, useXmlProlog, pathMacrosService);
        }

        
        @Override
        public StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                        String dir,
                                                        StateSplitterEx splitter,
                                                        Disposable parentDisposable,
                                                        @Nullable StateStorageListener listener,
                                                        PathMacrosService pathMacrosService) {
            return new IoDirectoryBasedStorage(pathMacroSubstitutor, dir, splitter, parentDisposable, listener, pathMacrosService);
        }
    },
    JAVA_NIO {
        
        @Override
        public StateStorage createFileBasedStorage(String filePath,
                                                   String fileSpec,
                                                   @Nullable RoamingType roamingType,
                                                   @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                                   String rootElementName,
                                                   Disposable parentDisposable,
                                                   @Nullable StateStorageListener listener,
                                                   @Nullable StreamProvider streamProvider,
                                                   boolean useXmlProlog,
                                                   PathMacrosService pathMacrosService) {
            return new PathFileBasedStorage(filePath, fileSpec, roamingType, pathMacroManager, rootElementName, parentDisposable, listener, streamProvider, useXmlProlog, pathMacrosService);
        }

        
        @Override
        public StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                        String dir,
                                                        StateSplitterEx splitter,
                                                        Disposable parentDisposable,
                                                        @Nullable StateStorageListener listener,
                                                        PathMacrosService pathMacrosService) {
            return new PathDirectoryBasedStorage(pathMacroSubstitutor, dir, splitter, parentDisposable, listener, pathMacrosService);
        }
    },
    CONSULO_VFS {
        
        @Override
        public StateStorage createFileBasedStorage(String filePath,
                                                   String fileSpec,
                                                   @Nullable RoamingType roamingType,
                                                   @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                                   String rootElementName,
                                                   Disposable parentDisposable,
                                                   @Nullable StateStorageListener listener,
                                                   @Nullable StreamProvider streamProvider,
                                                   boolean useXmlProlog,
                                                   PathMacrosService pathMacrosService) {
            return new VfsFileBasedStorage(filePath, fileSpec, roamingType, pathMacroManager, rootElementName, parentDisposable, listener, streamProvider, useXmlProlog, pathMacrosService);
        }

        
        @Override
        public StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                        String dir,
                                                        StateSplitterEx splitter,
                                                        Disposable parentDisposable,
                                                        @Nullable StateStorageListener listener,
                                                        PathMacrosService pathMacrosService) {
            return new VfsDirectoryBasedStorage(pathMacroSubstitutor, dir, splitter, parentDisposable, listener, pathMacrosService);
        }
    };

    
    public abstract StateStorage createFileBasedStorage(String filePath,
                                                        String fileSpec,
                                                        @Nullable RoamingType roamingType,
                                                        @Nullable TrackingPathMacroSubstitutor pathMacroManager,
                                                        String rootElementName,
                                                        Disposable parentDisposable,
                                                        @Nullable StateStorageListener listener,
                                                        @Nullable StreamProvider streamProvider,
                                                        boolean useXmlProlog,
                                                        PathMacrosService pathMacrosService);

    
    public abstract StateStorage createDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                                             String dir,
                                                             StateSplitterEx splitter,
                                                             Disposable parentDisposable,
                                                             @Nullable StateStorageListener listener,
                                                             PathMacrosService pathMacrosService);
}
