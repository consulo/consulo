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

import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessContentFileTypeDetector;
import consulo.it.internal.HeadlessFileTypeManager;
import consulo.it.internal.HeadlessDetectedFileType;
import consulo.language.plain.PlainTextFileType;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The headless file type manager resolves a type from content through the same {@code FileTypeDetectionService} the
 * production one uses, so these run the real detection path: the detector is only reachable by content, because
 * {@link HeadlessDetectedFileType} is registered under no extension and no name matcher.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ContentBasedFileTypeDetectionTest {
    private static final byte[] MARKED_CONTENT =
        (HeadlessContentFileTypeDetector.MARKER + "\nsome payload\n").getBytes(StandardCharsets.UTF_8);

    private static final byte[] MANAGED_ASSEMBLY_HEADER = {
        'M', 'Z', (byte)0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, 0x00, 0x00
    };

    @Test
    public void contentDecidesTheTypeWhenTheNameSaysNothing() throws Exception {
        VirtualFile file = writeFile("marked", MARKED_CONTENT);

        assertThat(file.getFileType())
            .as("only the detector can produce this type, so seeing it proves detection ran")
            .isSameAs(HeadlessDetectedFileType.INSTANCE);
    }

    @Test
    public void textNoDetectorClaimsBecomesPlainText() throws Exception {
        VirtualFile file = writeFile("unclaimed", "nothing special in here\n".getBytes(StandardCharsets.UTF_8));

        assertThat(file.getFileType()).isSameAs(PlainTextFileType.INSTANCE);
        assertThat(HeadlessContentFileTypeDetector.sawTextFor(file.getName()))
            .as("readable text must reach the detectors")
            .isTrue();
    }

    @Test
    public void binaryContentIsUnknownAndReachesDetectorsWithoutText() throws Exception {
        VirtualFile file = writeFile("assembly", MANAGED_ASSEMBLY_HEADER);

        assertThat(file.getFileType()).isSameAs(UnknownFileType.INSTANCE);
        assertThat(HeadlessContentFileTypeDetector.sawTextFor(file.getName()))
            .as("content that is not text must be offered as bytes only")
            .isFalse();
    }

    @Test
    public void aDetectedTypeIsCachedAndNotDetectedTwice() throws Exception {
        VirtualFile file = writeFile("cached", MARKED_CONTENT);
        HeadlessContentFileTypeDetector.reset();

        assertThat(file.getFileType()).isSameAs(HeadlessDetectedFileType.INSTANCE);
        assertThat(HeadlessContentFileTypeDetector.detectCallCount(file.getName())).isEqualTo(1);

        assertThat(file.getFileType()).isSameAs(HeadlessDetectedFileType.INSTANCE);
        assertThat(HeadlessContentFileTypeDetector.detectCallCount(file.getName()))
            .as("the second lookup must come from the detection cache")
            .isEqualTo(1);
    }

    /**
     * The detection cache is persisted in a file attribute, so a result that has not changed must not be written back.
     * {@code isFileOfType} resolves the type the same way a plain lookup does, so asking about a type must not cost a
     * second write either - it runs on nearly every editor pass, and the writes go to the VFS records.
     */
    @Test
    public void anUnchangedDetectionIsWrittenToTheAttributeOnlyOnce() throws Exception {
        VirtualFile file = writeFile("write-once", MARKED_CONTENT);
        HeadlessFileTypeManager.resetFlagWriteCount();

        assertThat(file.getFileType()).isSameAs(HeadlessDetectedFileType.INSTANCE);
        assertThat(HeadlessFileTypeManager.flagWriteCount(file))
            .as("the first detection must persist its result once")
            .isEqualTo(1);

        assertThat(file.getFileType()).isSameAs(HeadlessDetectedFileType.INSTANCE);
        assertThat(FileTypeRegistry.getInstance().isFileOfType(file, HeadlessDetectedFileType.INSTANCE)).isTrue();
        assertThat(FileTypeRegistry.getInstance().isFileOfType(file, UnknownFileType.INSTANCE)).isFalse();

        assertThat(HeadlessFileTypeManager.flagWriteCount(file))
            .as("re-asking for a type that did not change must not write the attribute again")
            .isEqualTo(1);
    }

    @Test
    public void plainTextDetectionIsAlsoWrittenOnlyOnce() throws Exception {
        VirtualFile file = writeFile("write-once-text", "nothing special in here\n".getBytes(StandardCharsets.UTF_8));
        HeadlessFileTypeManager.resetFlagWriteCount();

        assertThat(file.getFileType()).isSameAs(PlainTextFileType.INSTANCE);
        assertThat(file.getFileType()).isSameAs(PlainTextFileType.INSTANCE);

        assertThat(HeadlessFileTypeManager.flagWriteCount(file))
            .as("a text result that did not change must be persisted once")
            .isEqualTo(1);
    }

    /**
     * The detection cache is an inline VFS record attribute, and {@code isFileOfType} runs on nearly every editor pass.
     * A plain text or binary verdict reached through it must not be persisted, or the IDE writes into the records file
     * for every file it merely looks at.
     */
    @Test
    public void askingAboutATypeNeverPersistsAPlainTextVerdict() throws Exception {
        VirtualFile file = writeFile("no-write-on-ask", "nothing special in here\n".getBytes(StandardCharsets.UTF_8));
        HeadlessFileTypeManager.resetFlagWriteCount();

        assertThat(FileTypeRegistry.getInstance().isFileOfType(file, HeadlessDetectedFileType.INSTANCE)).isFalse();

        assertThat(HeadlessFileTypeManager.flagWriteCount(file))
            .as("a type question whose answer is plain text must not touch the persistent cache")
            .isZero();
    }

    @Test
    public void isFileOfTypeAnswersFromContentToo() throws Exception {
        VirtualFile marked = writeFile("of-type-marked", MARKED_CONTENT);
        VirtualFile unclaimed = writeFile("of-type-plain", "no marker here\n".getBytes(StandardCharsets.UTF_8));

        FileTypeRegistry registry = FileTypeRegistry.getInstance();
        assertThat(registry.isFileOfType(marked, HeadlessDetectedFileType.INSTANCE)).isTrue();
        assertThat(registry.isFileOfType(unclaimed, HeadlessDetectedFileType.INSTANCE)).isFalse();
    }

    private static VirtualFile writeFile(String prefix, byte[] content) throws Exception {
        Path path = Files.createTempFile("consulo-it-detect-" + prefix, ".headlessdetect");
        Files.write(path, content);

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertThat(file).as("%s must be visible in the VFS", path).isNotNull();
        return file;
    }
}
