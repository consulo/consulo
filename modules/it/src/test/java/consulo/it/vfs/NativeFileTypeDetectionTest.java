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
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The real {@code NativeFileTypeDetector} on real headers: ELF and PE images reach it as bytes (never as text) and it
 * must tell an executable from a shared library the way the formats define it, not the way the file is named.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class NativeFileTypeDetectionTest {
    private static final String NATIVE_EXECUTABLE = "NATIVE_EXECUTABLE";
    private static final String NATIVE_LIBRARY = "NATIVE_LIBRARY";

    private static final int ET_EXEC = 2;
    private static final int ET_DYN = 3;

    @Test
    public void elfExecutableIsDetectedFromContent() throws Exception {
        assertDetectedAs(NATIVE_EXECUTABLE, "elf-exec", ".bin", elf64(ET_EXEC, false));
    }

    @Test
    public void elfSharedLibraryIsDetectedFromContent() throws Exception {
        assertDetectedAs(NATIVE_LIBRARY, "elf-so", ".bin", elf64(ET_DYN, false));
    }

    /**
     * A position independent executable is an {@code ET_DYN} image like a shared library; only its {@code PT_INTERP}
     * segment tells the two apart.
     */
    @Test
    public void positionIndependentExecutableIsNotALibrary() throws Exception {
        assertDetectedAs(NATIVE_EXECUTABLE, "elf-pie", ".bin", elf64(ET_DYN, true));
    }

    @Test
    public void portableExecutableIsDetectedFromContent() throws Exception {
        assertDetectedAs(NATIVE_EXECUTABLE, "pe-exe", ".bin", portableExecutable(false));
    }

    /**
     * The shape that started the archive detection recursion: a {@code .dll} whose type only its PE header can give.
     */
    @Test
    public void portableExecutableDllIsDetectedFromContent() throws Exception {
        assertDetectedAs(NATIVE_LIBRARY, "pe-dll", ".dll", portableExecutable(true));
    }

    @Test
    public void theExtensionPlaysNoPartInTheAnswer() throws Exception {
        FileTypeRegistry registry = FileTypeRegistry.getInstance();
        assertThat(registry.getFileTypeByFileName("anything.so"))
            .as("no file type claims .so by name, so only the content can answer")
            .isSameAs(UnknownFileType.INSTANCE);

        assertDetectedAs(NATIVE_EXECUTABLE, "misnamed", ".so", elf64(ET_EXEC, false));
    }

    @Test
    public void textContentIsNotMistakenForANativeImage() throws Exception {
        VirtualFile file = writeFile("plain", ".bin", "#!/bin/sh\necho hello\n".getBytes(StandardCharsets.UTF_8));

        assertThat(file.getFileType().getId())
            .as("the native detector must claim nothing it cannot parse")
            .isNotIn(NATIVE_EXECUTABLE, NATIVE_LIBRARY);
    }

    private static void assertDetectedAs(String expectedId, String prefix, String suffix, byte[] content)
        throws Exception {
        VirtualFile file = writeFile(prefix, suffix, content);

        assertThat(file.getFileType().getId()).isEqualTo(expectedId);
    }

    /**
     * A 64 bit ELF header, optionally carrying the single {@code PT_INTERP} program header that marks an image as
     * something the loader is expected to run.
     */
    private static byte[] elf64(int type, boolean withInterp) {
        int programHeaderOffset = 64;
        int programHeaderSize = 56;
        int programHeaderCount = withInterp ? 1 : 0;
        String interpreter = "/lib64/ld-linux-x86-64.so.2\0";
        int interpreterOffset = programHeaderOffset + programHeaderSize * programHeaderCount;

        ByteBuffer buffer = ByteBuffer.allocate(interpreterOffset + interpreter.length()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{0x7F, 'E', 'L', 'F', 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        buffer.putShort((short)type);
        buffer.putShort((short)0x3E);
        buffer.putInt(1);
        buffer.putLong(0x1000);
        buffer.putLong(programHeaderCount == 0 ? 0 : programHeaderOffset);
        buffer.putLong(0);
        buffer.putInt(0);
        buffer.putShort((short)64);
        buffer.putShort((short)programHeaderSize);
        buffer.putShort((short)programHeaderCount);
        buffer.putShort((short)64);
        buffer.putShort((short)0);
        buffer.putShort((short)0);

        if (withInterp) {
            buffer.position(programHeaderOffset);
            buffer.putInt(3);
            buffer.putInt(4);
            buffer.putLong(interpreterOffset);
            buffer.putLong(0);
            buffer.putLong(0);
            buffer.putLong(interpreter.length());
            buffer.putLong(interpreter.length());
            buffer.putLong(1);
            buffer.position(interpreterOffset);
            buffer.put(interpreter.getBytes(StandardCharsets.US_ASCII));
        }
        return buffer.array();
    }

    /**
     * A PE image behind the usual DOS stub, with only the COFF characteristics deciding executable versus library.
     */
    private static byte[] portableExecutable(boolean library) {
        int peOffset = 0x80;
        int optionalHeaderSize = 240;

        ByteBuffer buffer =
            ByteBuffer.allocate(peOffset + 4 + 20 + optionalHeaderSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'M', 'Z', (byte)0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF});
        buffer.position(0x3C);
        buffer.putInt(peOffset);
        buffer.position(peOffset);
        buffer.put(new byte[]{'P', 'E', 0, 0});
        buffer.putShort((short)0x8664);
        buffer.putShort((short)1);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putShort((short)optionalHeaderSize);
        buffer.putShort((short)(library ? 0x2002 : 0x0002));
        buffer.putShort((short)0x20B);
        return buffer.array();
    }

    private static VirtualFile writeFile(String prefix, String suffix, byte[] content) throws Exception {
        Path path = Files.createTempFile("consulo-it-native-" + prefix, suffix);
        Files.write(path, content);

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertThat(file).as("%s must be visible in the VFS", path).isNotNull();
        return file;
    }
}
