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
package consulo.it.codeStyle;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.internal.TransferredWriteActionService;
import consulo.undoRedo.CommandProcessor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.it.internal.HeadlessAsyncFormattingService;
import consulo.it.internal.HeadlessFormattingServiceSuppressor;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.codeStyle.impl.internal.service.CoreFormattingService;
import consulo.language.codeStyle.impl.internal.service.FormattingServiceUtil;
import consulo.language.codeStyle.service.AsyncDocumentFormattingService;
import consulo.language.codeStyle.service.FormattingService;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The formatting service extension point: selection of the service for a file and the reformat flow through an
 * {@link consulo.language.codeStyle.service.AsyncDocumentFormattingService} (synchronous in the headless application).
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class FormattingServiceTest {
    private static final String IGNORE_HEADLESS = "consulo.async.formatting.ignoreHeadless";

    @BeforeEach
    public void setUp() {
        HeadlessAsyncFormattingService.setEnabled(true);
    }

    @AfterEach
    public void tearDown() {
        HeadlessAsyncFormattingService.setEnabled(false);
        HeadlessFormattingServiceSuppressor.setSuppressed(null);
    }

    @Test
    public void serviceIsSelectedForItsFiles(CodeInsightTestFixture fixture) throws Exception {
        PsiFile formatted = fixture.addFileToProject("src/headless-format-select.txt", "text\n");
        PsiFile other = fixture.addFileToProject("src/other.txt", "text\n");

        assertThat(findService(formatted, true)).isInstanceOf(HeadlessAsyncFormattingService.class);
        assertThat(findService(formatted, false)).isInstanceOf(HeadlessAsyncFormattingService.class);
        assertThat(findService(other, true)).isInstanceOf(CoreFormattingService.class);
    }

    @Test
    public void suppressedServiceIsSkipped(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/headless-format-suppressed.txt", "text\n");

        HeadlessFormattingServiceSuppressor.setSuppressed(HeadlessAsyncFormattingService.class);

        assertThat(findService(file, true)).isInstanceOf(CoreFormattingService.class);
    }

    @Test
    public void disabledServiceIsNotSelected(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/headless-format-disabled.txt", "text\n");

        HeadlessAsyncFormattingService.setEnabled(false);

        assertThat(findService(file, true)).isInstanceOf(CoreFormattingService.class);
    }

    @Test
    public void reformatWholeFile(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = addSyncFile(fixture, "headless-format-whole.txt", "first line\nsecond line\n");

        reformatText(fixture.getProject(), file, new TextRange(0, textOf(file).length()));

        assertThat(textOf(file)).isEqualTo("FIRST LINE\nSECOND LINE\n");
        List<HeadlessAsyncFormattingService.Request> requests = HeadlessAsyncFormattingService.getRequests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).ranges()).containsExactly(new TextRange(0, "first line\nsecond line\n".length()));
        assertThat(requests.get(0).quickFormat()).isFalse();
        assertThat(requests.get(0).whitespaceOnly()).isFalse();
    }

    @Test
    public void reformatRange(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = addSyncFile(fixture, "headless-format-range.txt", "first line\nsecond line\n");
        TextRange range = new TextRange("first line\n".length(), "first line\nsecond".length());

        reformatText(fixture.getProject(), file, range);

        assertThat(textOf(file)).isEqualTo("first line\nSECOND line\n");
        assertThat(HeadlessAsyncFormattingService.getRequests()).hasSize(1);
        assertThat(HeadlessAsyncFormattingService.getRequests().get(0).ranges()).containsExactly(range);
    }

    @Test
    public void reformatElement(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = addSyncFile(fixture, "headless-format-element.txt", "some text\n");

        runWriteCommand(fixture.getProject(), () -> CodeStyleManager.getInstance(fixture.getProject()).reformat(file));

        assertThat(textOf(file)).isEqualTo("SOME TEXT\n");
    }

    @Test
    public void ioFileOfALocalFileIsItsPath(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = addSyncFile(fixture, "headless-format-io.txt", "text\n");

        reformatText(fixture.getProject(), file, new TextRange(0, textOf(file).length()));

        Path ioFile = HeadlessAsyncFormattingService.getRequests().get(0).ioFile();
        assertThat(ioFile).isEqualTo(file.getVirtualFile().toNioPath());
    }

    @Test
    public void timeoutLeavesTheTextAndCancelsTheTask(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = addSyncFile(fixture, "headless-format-timeout.txt", "text\n");
        HeadlessAsyncFormattingService.setMode(HeadlessAsyncFormattingService.Mode.NO_RESPONSE, Duration.ofMillis(200));

        reformatText(fixture.getProject(), file, new TextRange(0, textOf(file).length()));

        assertThat(textOf(file)).isEqualTo("text\n");
        assertThat(HeadlessAsyncFormattingService.getRequests()).hasSize(1);
        assertThat(HeadlessAsyncFormattingService.getCancelCount()).isEqualTo(1);
    }

    @Test
    public void errorLeavesTheText(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = addSyncFile(fixture, "headless-format-error.txt", "text\n");
        HeadlessAsyncFormattingService.setMode(HeadlessAsyncFormattingService.Mode.ERROR, Duration.ofSeconds(30));

        reformatText(fixture.getProject(), file, new TextRange(0, textOf(file).length()));

        assertThat(textOf(file)).isEqualTo("text\n");
        assertThat(HeadlessAsyncFormattingService.getRequests()).hasSize(1);
        assertThat(HeadlessAsyncFormattingService.getCancelCount()).isZero();
    }

    @Test
    public void asyncFormattingRunsOffTheUIThreadAndUpdatesTheDocument(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/headless-format-async.txt", "async text\n");
        String ignoreHeadless = System.getProperty(IGNORE_HEADLESS);
        System.setProperty(IGNORE_HEADLESS, "true");
        try {
            reformatText(fixture.getProject(), file, new TextRange(0, textOf(file).length()));

            waitFor("the formatted text must be applied", () -> "ASYNC TEXT\n".equals(textOf(file)));
            assertThat(HeadlessAsyncFormattingService.getRequests()).hasSize(1);
            assertThat(HeadlessAsyncFormattingService.getRequests().get(0).onUIThread()).isFalse();
        }
        finally {
            if (ignoreHeadless == null) {
                System.clearProperty(IGNORE_HEADLESS);
            }
            else {
                System.setProperty(IGNORE_HEADLESS, ignoreHeadless);
            }
        }
    }

    @Test
    public void otherFilesAreFormattedByTheCoreService(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/other-file.txt", "text\n");

        reformatText(fixture.getProject(), file, new TextRange(0, textOf(file).length()));

        assertThat(textOf(file)).isEqualTo("text\n");
        assertThat(HeadlessAsyncFormattingService.getRequests()).isEmpty();
    }

    /**
     * A file whose document is formatted synchronously, so the result is there when the reformat returns.
     */
    private static PsiFile addSyncFile(CodeInsightTestFixture fixture, String name, String text) throws Exception {
        PsiFile file = fixture.addFileToProject("src/" + name, text);
        Document document = ReadAction.compute(() -> PsiDocumentManager.getInstance(file.getProject()).getDocument(file));
        document.putUserData(AsyncDocumentFormattingService.FORMAT_DOCUMENT_SYNCHRONOUSLY, true);
        return file;
    }

    private static FormattingService findService(PsiFile file, boolean isCompleteFile) {
        return ReadAction.compute(() -> FormattingServiceUtil.findService(file, true, isCompleteFile));
    }

    private static void reformatText(Project project, PsiFile file, TextRange range) {
        runWriteCommand(
            project,
            () -> CodeStyleManager.getInstance(project).reformatText(file, range.getStartOffset(), range.getEndOffset())
        );
    }

    /**
     * Write authority acquired off the UI thread, the command itself transferred onto the UI thread.
     */
    private static void runWriteCommand(Project project, Runnable command) {
        WriteAction.run(() -> project.getApplication().getInstance(TransferredWriteActionService.class)
            .runOnEdtWithTransferredWriteActionAndWait(
                () -> CommandProcessor.getInstance().newCommand().project(project).name(FormatterUtil.REFORMAT_COMMAND_NAME).run(command)
            ));
    }

    private static String textOf(PsiFile file) {
        return ReadAction.compute(() -> PsiDocumentManager.getInstance(file.getProject()).getDocument(file).getText());
    }
}
