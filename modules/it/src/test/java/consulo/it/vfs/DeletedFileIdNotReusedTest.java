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
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.impl.internal.FSRecords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A new file never gets the id of a deleted one: ids are always allocated past the end of the records storage.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class DeletedFileIdNotReusedTest {
    @Test
    public void createdFileGetsFreshId() throws Exception {
        assertThat(FSRecords.REUSE_DELETED_FILE_IDS).as("deleted file ids must not be reused by default").isFalse();

        Path directory = Files.createTempDirectory("consulo-it-deleted-file-id");
        VirtualFile directoryFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(directoryFile).isNotNull();
        directoryFile.getChildren();

        Path deleted = directory.resolve("deleted.txt");
        Files.writeString(deleted, "deleted");
        directoryFile.refresh(false, true);
        VirtualFile deletedFile = directoryFile.findChild("deleted.txt");
        assertThat(deletedFile).isNotNull();
        int deletedId = ((VirtualFileWithId) deletedFile).getId();

        Files.delete(deleted);
        directoryFile.refresh(false, true);
        assertThat(directoryFile.findChild("deleted.txt")).isNull();
        assertThat(deletedFile.isValid()).isFalse();

        int maxIdBeforeCreation = FSRecords.getMaxId();

        Path created = directory.resolve("created.txt");
        Files.writeString(created, "created");
        directoryFile.refresh(false, true);
        VirtualFile createdFile = directoryFile.findChild("created.txt");
        assertThat(createdFile).isNotNull();
        int createdId = ((VirtualFileWithId) createdFile).getId();

        assertThat(createdId).as("the deleted file id must not be given to the new file").isNotEqualTo(deletedId);
        assertThat(createdId).as("the new file id must be allocated past the end of the records storage")
            .isGreaterThanOrEqualTo(maxIdBeforeCreation);
    }
}
