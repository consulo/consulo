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
package consulo.test.junit.impl.light;

import consulo.test.junit.impl.extension.ConsuloProjectLoader;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ExtendWith(ConsuloProjectLoader.class)
public class LocalFileSystemTest {
    @Test
    public void testFindFileByPath(@TempDir Path tempDir) throws Exception {
        Path ioFile = Files.writeString(tempDir.resolve("test.txt"), "some text");

        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

        VirtualFile file = localFileSystem.findFileByPath(FileUtil.toSystemIndependentName(ioFile.toString()));

        Assertions.assertNotNull(file);
        Assertions.assertSame(localFileSystem, file.getFileSystem());
        Assertions.assertTrue(file.isInLocalFileSystem());
        Assertions.assertFalse(file.isDirectory());
        Assertions.assertEquals(ioFile, file.toNioPath());
        Assertions.assertEquals("some text", new String(file.contentsToByteArray(), StandardCharsets.UTF_8));

        VirtualFile directory = localFileSystem.findFileByPath(FileUtil.toSystemIndependentName(tempDir.toString()));

        Assertions.assertNotNull(directory);
        Assertions.assertTrue(directory.isDirectory());
        Assertions.assertEquals(directory, file.getParent());
        Assertions.assertEquals(file, directory.findChild("test.txt"));

        Assertions.assertNull(localFileSystem.findFileByPath(FileUtil.toSystemIndependentName(tempDir.resolve("missing.txt").toString())));
    }

    @Test
    public void testFindFileByNioPath(VirtualFileManager virtualFileManager, @TempDir Path tempDir) {
        VirtualFile directory = virtualFileManager.findFileByNioPath(tempDir);

        Assertions.assertNotNull(directory);
        Assertions.assertSame(LocalFileSystem.get(virtualFileManager), directory.getFileSystem());
        Assertions.assertEquals(directory, LocalFileSystem.getInstance().findFileByNioFile(tempDir));
    }
}
