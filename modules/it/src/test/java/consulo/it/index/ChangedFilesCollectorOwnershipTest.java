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

import consulo.application.Application;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.index.impl.internal.FileBasedIndexImpl;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.event.AsyncFileListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The collector VFS dispatches events into must be the very instance the file based index drains: a second instance
 * would record changes forever without them ever being indexed.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class ChangedFilesCollectorOwnershipTest {
    @Test
    public void vfsAndIndexShareOneCollector(Application application, HeadlessProjects projects) throws Exception {
        FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();

        assertThat(registeredCollectors(application))
            .as("exactly one collector must be registered with VFS and it must be the one the index drains")
            .containsExactly(fileBasedIndex.getChangedFilesCollector());

        Path directory = Files.createTempDirectory("consulo-it-collector-ownership");
        Path src = Files.createDirectories(directory.resolve("src"));
        Project project = projects.open(directory);
        addContentRoot(project, directory);
        awaitIdle(project);

        Files.writeString(src.resolve("created.sand"), "class Created {}");
        findFile(src).refresh(false, true);

        waitFor("a file created after the project opened must reach the index", () -> !findClasses(project, "Created").isEmpty());

        assertThat(registeredCollectors(application))
            .as("the collector must not be replaced while events flow")
            .containsExactly(fileBasedIndex.getChangedFilesCollector());
    }

    private static List<Object> registeredCollectors(Application application) {
        return application.getExtensionPoint(AsyncFileListener.class)
            .getExtensionList()
            .stream()
            .filter(listener -> "ChangedFilesCollector".equals(listener.getClass().getSimpleName()))
            .map(listener -> (Object) listener)
            .toList();
    }
}
