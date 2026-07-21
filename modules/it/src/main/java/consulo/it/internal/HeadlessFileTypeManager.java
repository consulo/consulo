/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.language.file.FileTypeManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Mock {@code FileTypeManager} for the integration-test harness. The production impl
 * ({@code FileTypeManagerImpl}) lives in {@code ide-impl}; for now every file resolves to
 * {@link UnknownFileType} and no ignore patterns / associations are known. Bound only under the
 * {@link ComponentProfiles#INTEGRATION_TEST} profile.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessFileTypeManager extends FileTypeManager {
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
        return getFileTypeByFile(file) == type;
    }

    @Override
    public FileType[] getRegisteredFileTypes() {
        return new FileType[]{UnknownFileType.INSTANCE};
    }

    @Override
    public FileType getFileTypeByFile(VirtualFile file) {
        return UnknownFileType.INSTANCE;
    }

    @Override
    public FileType getFileTypeByFileName(String fileTypeId) {
        return UnknownFileType.INSTANCE;
    }

    @Override
    public FileType getFileTypeByExtension(String extension) {
        return UnknownFileType.INSTANCE;
    }

    @Override
    public @Nullable FileType findFileTypeByName(String fileTypeName) {
        return null;
    }

    @Override
    public @Nullable FileType getKnownFileTypeOrAssociate(String fileName) {
        return null;
    }

    @Override
    public @Nullable FileType getKnownFileTypeOrAssociate(VirtualFile file, Project project) {
        return file.getFileType();
    }

    @Override
    public String[] getAssociatedExtensions(FileType type) {
        return new String[0];
    }

    @Override
    public List<FileNameMatcher> getAssociations(FileType type) {
        return List.of();
    }

    @Override
    public void addFileTypeListener(FileTypeListener listener) {
    }

    @Override
    public void removeFileTypeListener(FileTypeListener listener) {
    }

    @Override
    public void associate(FileType type, FileNameMatcher matcher) {
    }

    @Override
    public void removeAssociation(FileType type, FileNameMatcher matcher) {
    }

    @Override
    public FileType getStdFileType(String fileTypeName) {
        return UnknownFileType.INSTANCE;
    }
}
