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

import consulo.application.ReadAction;
import consulo.document.Document;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A completion renderer copies the file of the completion position, edits the copy's document and commits it - on a pooled thread,
 * inside a read action. The copy is non-physical, so its commit must not take the write lock: a reader can't.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class NonPhysicalCommitInReadActionTest {
    @Test
    public void copyIsCommittedInsideReadAction(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-copy-commit");
        Path source = directory.resolve("some.sand");
        Files.writeString(source, "class Item { \"text\" }\n");

        Project project = projects.open(directory);
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(source);
        assertThat(file).isNotNull();

        CompletableFuture<String> copyText = CompletableFuture.supplyAsync(() -> ReadAction.compute(() -> {
            PsiFile original = PsiManager.getInstance(project).findFile(file);
            assertThat(original).isNotNull();

            PsiFile copy = (PsiFile) original.copy();
            Document document = copy.getViewProvider().getDocument();
            assertThat(document).isNotNull();

            document.insertString(document.getTextLength(), "class Extra {}\n");
            PsiDocumentManager.getInstance(project).commitDocument(document);
            return copy.getText();
        }));

        assertThat(copyText.get(30, TimeUnit.SECONDS)).contains("class Extra");
    }
}
