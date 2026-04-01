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
package consulo.virtualFileSystem.nativeFileType.impl.internal;

import com.jetbrains.util.filetype.DetectedFileInfo;
import com.jetbrains.util.filetype.FileProperties;
import consulo.annotation.component.ExtensionImpl;
import consulo.logging.Logger;
import consulo.util.io.ByteSequence;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeDetector;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * @author VISTALL
 * @since 2026-03-31
 */
@ExtensionImpl(id = "native")
public class NativeFileTypeDetector implements FileTypeDetector {
    private static final Logger LOG = Logger.getInstance(NativeFileTypeDetector.class);

    @Override
    public @Nullable FileType detect(VirtualFile file, ByteSequence firstBytes, @Nullable CharSequence firstCharsIfText) {
        try {
            ByteSequenceSeekableByteChannel channel = new ByteSequenceSeekableByteChannel(firstBytes);
            DetectedFileInfo detected = com.jetbrains.util.filetype.FileTypeDetector.detectFileType(channel);

            com.jetbrains.util.filetype.FileType key = detected.fileType();
            EnumSet<FileProperties> value = detected.fileProperties();

            switch (key) {
                case Pe:
                case Elf:
                case MachO:
                    if (value.contains(FileProperties.ExecutableType)) {
                        return NativeExecutableFileType.INSTANCE;
                    }
                    return NativeLibraryFileType.INSTANCE;
                case Msi:
                    return NativeExecutableFileType.INSTANCE;
            }
        }
        catch (Exception e) {
            LOG.warn("Failed to detect file type: " + file.getPath(), e);
        }

        return null;
    }

    @Override
    public int getDesiredContentPrefixLength() {
        return FileUtil.KILOBYTE * 20;
    }

    @Override
    public int getVersion() {
        return 14;
    }
}
