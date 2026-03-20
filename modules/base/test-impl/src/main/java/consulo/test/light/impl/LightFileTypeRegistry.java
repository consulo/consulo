/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import consulo.component.ComponentManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.Maps;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightFileTypeRegistry extends FileTypeRegistry {
    private final Map<String, FileType> myExtensionsMap = Maps.newHashMap(FileUtil.PATH_HASHING_STRATEGY);
    private final List<FileType> myAllFileTypes = new ArrayList<>();

    @Inject
    public LightFileTypeRegistry() {
        myAllFileTypes.add(UnknownFileType.INSTANCE);
    }

    @Override
    public boolean isFileIgnored(String name) {
        return false;
    }

    @Override
    public boolean isFileIgnored(VirtualFile file) {
        return false;
    }

    @Override
    public boolean isFileOfType(VirtualFile file, FileType type) {
        return file.getFileType() == type;
    }

    
    @Override
    public FileType[] getRegisteredFileTypes() {
        return myAllFileTypes.toArray(new FileType[myAllFileTypes.size()]);
    }

    
    @Override
    public FileType getFileTypeByFile(VirtualFile file) {
        return getFileTypeByFileName(file.getName());
    }

    
    @Override
    public FileType getFileTypeByFileName(String fileName) {
        String extension = FileUtil.getExtension(fileName);
        return getFileTypeByExtension(extension);
    }

    
    @Override
    public FileType getFileTypeByFileName(CharSequence fileName) {
        String extension = FileUtil.getExtension(fileName.toString());
        return getFileTypeByExtension(extension);
    }

    
    @Override
    public FileType getFileTypeByExtension(String extension) {
        FileType result = myExtensionsMap.get(extension);
        return result == null ? UnknownFileType.INSTANCE : result;
    }

    public void registerFileType(FileType fileType, String extension) {
        myAllFileTypes.add(fileType);
        for (String ext : extension.split(";")) {
            myExtensionsMap.put(ext, fileType);
        }
    }

    @Override
    public @Nullable FileType findFileTypeByName(String fileTypeName) {
        for (FileType type : myAllFileTypes) {
            if (type.getId().equals(fileTypeName)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public @Nullable FileType getKnownFileTypeOrAssociate(VirtualFile file, ComponentManager project) {
        return null;
    }

    @RequiredUIAccess
    @Override
    public @Nullable FileType getKnownFileTypeOrAssociate(String fileName) {
        return null;
    }
}
