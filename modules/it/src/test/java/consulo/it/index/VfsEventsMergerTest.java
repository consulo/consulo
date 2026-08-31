// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.it.index;

import consulo.component.ProcessCanceledException;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.index.impl.internal.VfsEventsMerger;
import consulo.language.index.impl.internal.VfsEventsMerger.VfsEventProcessor;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(HeadlessApplicationExtension.class)
public class VfsEventsMergerTest {
    /**
     * Ensures cancellation during preparation does not discard a change that must be processed later.
     */
    @Test
    public void changeRemainsQueuedWhenPreparationIsCancelled() {
        VfsEventsMerger merger = new VfsEventsMerger();
        merger.recordFileEvent(new TestVirtualFile("test.txt"), true);

        assertThrows(ProcessCanceledException.class, () -> merger.processChanges(new VfsEventProcessor() {
            @Override
            public void prepare(VfsEventsMerger.ChangeInfo changeInfo) {
                throw new ProcessCanceledException();
            }

            @Override
            public boolean process(VfsEventsMerger.ChangeInfo changeInfo) {
                return fail("The cancelled change must not be processed");
            }
        }));
        assertTrue(merger.hasChanges());

        boolean[] processed = new boolean[1];
        merger.processChanges(changeInfo -> {
            processed[0] = true;
            return true;
        });

        assertTrue(processed[0]);
        assertFalse(merger.hasChanges());
    }

    private static class TestVirtualFile extends LightVirtualFile implements VirtualFileWithId {
        TestVirtualFile(String name) {
            super(name);
        }

        @Override
        public int getId() {
            return 1;
        }
    }
}
