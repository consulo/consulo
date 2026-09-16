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
package consulo.it.vfs;

import consulo.application.Application;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessApplicationEncodingManager;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Content-based file type detection is reachable while the project root index is being built, so it must not ask the
 * encoding manager for a per-file encoding - that lookup goes through {@code ProjectLocator} and
 * {@code ProjectFileIndex} back into the root index. Content that will not be decoded never needs an encoding, so it is
 * neither requested nor stored on the file; for text content it still is, because detectors such as the Unity asset one
 * need the decoded text.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class BinaryContentSkipsEncodingLookupTest {
    private static final byte[] MANAGED_ASSEMBLY_HEADER = {
        'M', 'Z', (byte)0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, 0x00, 0x00
    };

    private static final byte[] NUL_BYTE_CONTENT = {
        'U', 'n', 'i', 't', 'y', 'F', 'S', 0x00, 0x00, 0x00, 0x00, 0x06, 0x35, 0x2E, 0x78, 0x00
    };

    @Test
    public void managedAssemblyHeaderNeverAsksForAPerFileEncoding(Application application) throws Exception {
        assertNoEncodingLookup(application, "consulo-it-assembly", ".dll", MANAGED_ASSEMBLY_HEADER);
    }

    @Test
    public void nulByteContentNeverAsksForAPerFileEncoding(Application application) throws Exception {
        assertNoEncodingLookup(application, "consulo-it-binary", ".bin", NUL_BYTE_CONTENT);
    }

    @Test
    public void textContentIsStillDecodedForDetectors(Application application) throws Exception {
        HeadlessApplicationEncodingManager encodingManager = encodingManager(application);
        byte[] content = "%YAML 1.1\n%TAG !u! tag:unity3d.com,2011:\n".getBytes(StandardCharsets.UTF_8);
        VirtualFile file = writeFile("consulo-it-text", ".prefab", content);

        encodingManager.resetEncodingLookupCount();
        CharSequence text = detect(file, content);

        assertThat(text).as("detectors keyed on text must still receive it").startsWith("%YAML");
        assertThat(encodingManager.getEncodingLookupCount(file)).as("per-file encoding lookup count").isPositive();
    }

    private static void assertNoEncodingLookup(Application application, String prefix, String suffix, byte[] content)
        throws Exception {
        HeadlessApplicationEncodingManager encodingManager = encodingManager(application);
        VirtualFile file = writeFile(prefix, suffix, content);

        encodingManager.resetEncodingLookupCount();
        CharSequence text = detect(file, content);

        assertThat(text).as("content that is not text must not be handed to detectors as text").isNull();
        assertThat(encodingManager.getEncodingLookupCount(file)).as("per-file encoding lookup count").isZero();
        assertThat(file.isCharsetSet()).as("no charset may be stored for content that was never decoded").isFalse();
    }

    private static HeadlessApplicationEncodingManager encodingManager(Application application) {
        return (HeadlessApplicationEncodingManager)application.getInstance(ApplicationEncodingManager.class);
    }

    private static VirtualFile writeFile(String prefix, String suffix, byte[] content) throws Exception {
        Path path = Files.createTempFile(prefix, suffix);
        Files.write(path, content);

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertThat(file).as("%s must be visible in the VFS", path).isNotNull();
        return file;
    }

    private static @Nullable CharSequence detect(VirtualFile file, byte[] content) {
        AtomicReference<CharSequence> detected = new AtomicReference<>();
        FileType fileType = LoadTextUtil.processTextFromBinaryPresentationOrNull(
            content,
            content.length,
            file,
            true,
            true,
            UnknownFileType.INSTANCE,
            text -> {
                detected.set(text);
                return UnknownFileType.INSTANCE;
            }
        );
        assertThat(fileType).isSameAs(UnknownFileType.INSTANCE);
        return detected.get();
    }
}
