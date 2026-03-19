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
package consulo.module.content.internal;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.layer.ModuleRootModel;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2014-12-08
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ModuleRootsProcessor {
    public static final ExtensionPointName<ModuleRootsProcessor> EP_NAME = ExtensionPointName.create(ModuleRootsProcessor.class);

    public static @Nullable ModuleRootsProcessor findRootsProcessor(ModuleRootModel moduleRootModel) {
        for (ModuleRootsProcessor moduleRootsProcessor : EP_NAME.getExtensionList()) {
            if (moduleRootsProcessor.canHandle(moduleRootModel)) {
                return moduleRootsProcessor;
            }
        }
        return null;
    }

    public abstract boolean canHandle(ModuleRootModel moduleRootModel);

    public abstract boolean containsFile(ObjectIntMap<VirtualFile> roots, VirtualFile virtualFile);

    public void processFiles(
        ModuleRootModel moduleRootModel,
        Predicate<ContentFolderTypeProvider> predicate,
        Predicate<VirtualFile> processor
    ) {
        VirtualFile[] files = getFiles(moduleRootModel, predicate);
        ContainerUtil.process(files, processor);
    }

    public void processFileUrls(
        ModuleRootModel moduleRootModel,
        Predicate<ContentFolderTypeProvider> predicate,
        Predicate<String> processor
    ) {
        String[] files = getUrls(moduleRootModel, predicate);
        ContainerUtil.process(files, processor);
    }

    
    @Deprecated
    @DeprecationInfo(value = "Override #processFiles()")
    public VirtualFile[] getFiles(ModuleRootModel moduleRootModel, Predicate<ContentFolderTypeProvider> predicate) {
        return VirtualFile.EMPTY_ARRAY;
    }

    
    @Deprecated
    @DeprecationInfo(value = "Override #processFileUrls()")
    public String[] getUrls(ModuleRootModel moduleRootModel, Predicate<ContentFolderTypeProvider> predicate) {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }
}
