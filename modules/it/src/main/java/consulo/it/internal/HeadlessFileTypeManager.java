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
import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.document.util.FileContentUtilCore;
import consulo.language.file.FileTypeManager;
import consulo.language.internal.FileTypeManagerEx;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.StubVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.virtualFileSystem.impl.internal.fileType.FileTypeDetectionService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock {@code FileTypeManager} for the integration-test harness. The production impl
 * ({@code FileTypeManagerImpl}) lives in {@code ide-impl}; file types registered through
 * {@link FileTypeFactory} extensions are resolved by extension and name matcher, and whatever the name cannot answer
 * goes through the real {@link FileTypeDetectionService}, so content-based detection behaves as it does in production.
 * No ignore patterns / user associations are known. Bound only under the
 * {@link ComponentProfiles#INTEGRATION_TEST} profile.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessFileTypeManager extends FileTypeManagerEx implements Disposable {
    private volatile @Nullable Registry myRegistry;

    private final FileTypeDetectionService myDetectionService;

    private static final Map<String, AtomicInteger> ourFlagWrites = new ConcurrentHashMap<>();

    /**
     * How often the detection cache was written to the persistent file attribute for {@code file}. Detection results
     * that did not change must not be written again, so a test can hold this to one write per file.
     */
    public static int flagWriteCount(VirtualFile file) {
        AtomicInteger count = ourFlagWrites.get(file.getPath());
        return count == null ? 0 : count.get();
    }

    public static void resetFlagWriteCount() {
        ourFlagWrites.clear();
    }

    @Inject
    public HeadlessFileTypeManager(Application application) {
        myDetectionService = new FileTypeDetectionService(application, 0, this) {
            @Override
            protected void writeFlagsToCache(VirtualFile file, int flags) {
                ourFlagWrites.computeIfAbsent(file.getPath(), path -> new AtomicInteger()).incrementAndGet();
                super.writeFlagsToCache(file, flags);
            }

            @Override
            protected FileType getDefaultTextFileType() {
                return PlainTextFileType.INSTANCE;
            }

            @Override
            protected @Nullable FileType getFileTypeByFileWithoutContent(VirtualFile file) {
                FileType fileType = getFileTypeByFileName(file.getName());
                return fileType == UnknownFileType.INSTANCE ? null : fileType;
            }

            @Override
            protected void onDetectedFileTypesChanged(List<VirtualFile> changed, List<VirtualFile> crashed) {
                if (!changed.isEmpty()) {
                    application.invokeLater(() -> FileContentUtilCore.reparseFiles(changed), application.getDisposed());
                }
                if (!crashed.isEmpty()) {
                    AppExecutorUtil.getAppScheduledExecutorService()
                        .schedule(() -> FileContentUtilCore.reparseFiles(crashed), 10, TimeUnit.SECONDS);
                }
            }
        };
    }

    @Override
    public void dispose() {
    }

    private static class Registry {
        final Map<String, FileType> byExtension = new HashMap<>();
        final List<Pair<FileNameMatcher, FileType>> matchers = new ArrayList<>();
    }

    private Registry registry() {
        Registry registry = myRegistry;
        if (registry == null) {
            synchronized (this) {
                registry = myRegistry;
                if (registry == null) {
                    Registry filled = new Registry();
                    Application.get().getExtensionPoint(FileTypeFactory.class).forEach(factory -> factory.createFileTypes(new FileTypeConsumer() {
                        @Override
                        public void consume(FileType fileType) {
                            consume(fileType, fileType.getDefaultExtension());
                        }

                        @Override
                        public void consume(FileType fileType, String extensions) {
                            for (String extension : extensions.split(";")) {
                                if (!extension.isEmpty()) {
                                    filled.byExtension.put(extension.toLowerCase(Locale.ROOT), fileType);
                                }
                            }
                        }

                        @Override
                        public void consume(FileType fileType, FileNameMatcher... nameMatchers) {
                            for (FileNameMatcher matcher : nameMatchers) {
                                filled.matchers.add(Pair.create(matcher, fileType));
                            }
                        }
                    }));
                    myRegistry = registry = filled;
                }
            }
        }
        return registry;
    }

    @Override
    public String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1);
    }

    @Override
    public void fireFileTypesChanged() {
    }

    @Override
    public void fireBeforeFileTypesChanged() {
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
        if (FileTypeDetectionService.mightBeReplacedByDetectedFileType(type) || type.equals(UnknownFileType.INSTANCE)) {
            return file.getFileType().equals(type);
        }

        FileType fileTypeByFileName = getFileTypeByFileName(file.getName());
        if (fileTypeByFileName == type) {
            return true;
        }
        if (fileTypeByFileName != UnknownFileType.INSTANCE) {
            return false;
        }
        if (file instanceof StubVirtualFile) {
            return false;
        }

        return myDetectionService.isDetectedFromContentAs(file, type);
    }

    @Override
    public FileType[] getRegisteredFileTypes() {
        Registry registry = registry();
        List<FileType> types = new ArrayList<>(registry.byExtension.values());
        for (Pair<FileNameMatcher, FileType> matcher : registry.matchers) {
            types.add(matcher.getSecond());
        }
        types.add(UnknownFileType.INSTANCE);
        return types.toArray(FileType[]::new);
    }

    @Override
    public FileType getFileTypeByFile(VirtualFile file) {
        return getFileTypeByFile(file, null);
    }

    @Override
    public FileType getFileTypeByFile(VirtualFile file, byte @Nullable [] content) {
        FileType fileType = getFileTypeByFileName(file.getName());
        if (file instanceof StubVirtualFile) {
            return fileType;
        }
        if (fileType == UnknownFileType.INSTANCE) {
            return myDetectionService.getOrDetectFromContent(file, content);
        }
        if (FileTypeDetectionService.mightBeReplacedByDetectedFileType(fileType)) {
            FileType detectedFromContent = myDetectionService.getOrDetectFromContent(file, content);
            if (detectedFromContent != UnknownFileType.INSTANCE && detectedFromContent != PlainTextFileType.INSTANCE) {
                return detectedFromContent;
            }
        }
        return fileType;
    }

    @Override
    public FileType getFileTypeByFileName(String fileName) {
        Registry registry = registry();
        for (Pair<FileNameMatcher, FileType> matcher : registry.matchers) {
            if (matcher.getFirst().accept(fileName)) {
                return matcher.getSecond();
            }
        }
        return getFileTypeByExtension(getExtension(fileName));
    }

    @Override
    public FileType getFileTypeByExtension(String extension) {
        FileType fileType = registry().byExtension.get(extension.toLowerCase(Locale.ROOT));
        return fileType == null ? UnknownFileType.INSTANCE : fileType;
    }

    @Override
    public @Nullable FileType findFileTypeByName(String fileTypeName) {
        for (FileType fileType : getRegisteredFileTypes()) {
            if (fileType.getId().equals(fileTypeName)) {
                return fileType;
            }
        }
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
