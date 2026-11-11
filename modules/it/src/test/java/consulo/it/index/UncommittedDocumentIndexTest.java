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
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.internal.TransferredWriteActionService;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.internal.psi.stub.IdIndex;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdIndexEntry;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An index query that runs while a loaded document is ahead of its PSI must answer from the document text, not from
 * the stale PSI tree: the in-memory index content is the one with the latest modification stamp.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class UncommittedDocumentIndexTest {
    @Test
    public void unsavedEditIsIndexedBeforeCommit(Application application, HeadlessProjects projects) throws Exception {
        Fixture fixture = open(projects, "consulo-it-uncommitted-edit");

        AtomicReference<QueryResult> result = new AtomicReference<>();
        WriteAction.run(() -> application.getInstance(TransferredWriteActionService.class).runOnEdtWithTransferredWriteActionAndWait(
            () -> {
                CommandProcessor.getInstance().runUndoTransparentAction(
                    () -> fixture.document().replaceString(0, fixture.document().getTextLength(), "class Bar {}"));
                result.set(query(fixture));
            }));

        assertResult(result.get());
    }

    @Test
    public void externalChangeOfLoadedDocumentIsIndexedBeforeCommit(Application application, HeadlessProjects projects) throws Exception {
        Fixture fixture = open(projects, "consulo-it-uncommitted-external");

        Files.writeString(Path.of(fixture.file().getPath()), "class Bar {}");

        AtomicReference<QueryResult> result = new AtomicReference<>();
        WriteAction.run(() -> application.getInstance(TransferredWriteActionService.class).runOnEdtWithTransferredWriteActionAndWait(
            () -> {
                fixture.file().refresh(false, false);
                result.set(query(fixture));
            }));

        assertResult(result.get());
    }

    private static void assertResult(QueryResult result) {
        assertThat(result.text()).as("the document must carry the new text").isEqualTo("class Bar {}");
        assertThat(result.uncommitted()).as("the query must run before the document is committed").isTrue();
        assertThat(result.psiCached()).as("the stale PSI must still be cached for the document").isTrue();
        assertThat(result.withNewWord()).as("the new word must be found in the uncommitted document").hasSize(1);
        assertThat(result.withOldWord()).as("the old word must be gone from the uncommitted document").isEmpty();
    }

    private static QueryResult query(Fixture fixture) {
        Project project = fixture.project();
        Document document = fixture.document();
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        Collection<VirtualFile> withNewWord = FileBasedIndex.getInstance().getContainingFiles(IdIndex.NAME, new IdIndexEntry("Bar", true), scope);
        Collection<VirtualFile> withOldWord = FileBasedIndex.getInstance().getContainingFiles(IdIndex.NAME, new IdIndexEntry("Foo", true), scope);
        return new QueryResult(
            document.getText(),
            psiDocumentManager.isUncommited(document),
            psiDocumentManager.getCachedPsiFile(document) != null,
            withNewWord,
            withOldWord
        );
    }

    private static Fixture open(HeadlessProjects projects, String prefix) throws Exception {
        Path directory = Files.createTempDirectory(prefix);
        Path src = Files.createDirectories(directory.resolve("src"));
        Path path = src.resolve("some.sand");
        Files.writeString(path, "class Foo {}");

        Project project = projects.open(directory);
        addContentRoot(project, directory);
        awaitIdle(project);

        VirtualFile file = findFile(path);
        waitFor("the initial content must be indexed", () -> ReadAction.compute(() -> {
            try {
                return FileBasedIndex.getInstance()
                    .getContainingFiles(IdIndex.NAME, new IdIndexEntry("Foo", true), GlobalSearchScope.allScope(project))
                    .contains(file);
            }
            catch (IndexNotReadyException e) {
                return false;
            }
        }));

        Document document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(file));
        assertThat(document).isNotNull();
        PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(file));
        assertThat(psiFile).isNotNull();
        assertThat(ReadAction.compute(() -> PsiDocumentManager.getInstance(project).isCommitted(document))).isTrue();
        return new Fixture(project, file, document, psiFile);
    }

    private record Fixture(Project project, VirtualFile file, Document document, PsiFile psiFile) {
    }

    private record QueryResult(
        String text,
        boolean uncommitted,
        boolean psiCached,
        Collection<VirtualFile> withNewWord,
        Collection<VirtualFile> withOldWord
    ) {
    }
}
