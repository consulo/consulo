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
package consulo.it.codeInsight;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.it.HeadlessUIThread;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.document.util.TextRange;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.impl.internal.highlight.UpdateHighlightersUtilImpl;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.language.editor.impl.internal.intention.ShowIntentionActionsHandler;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Alt+Enter computes what it offers off the UI thread, under a read action, and hands the result back to the UI thread.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class ShowIntentionActionsHandlerTest {
    private static final int GROUP = 0x1A17;

    @Test
    public void quickFixAvailabilityIsCheckedOffTheUIThread(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.configureByText("some.sand", "class Item { \"body\" }\n");
        Project project = fixture.getProject();
        // in dumb mode the cached intentions drop every action which is not dumb aware - the recording fix too
        HeadlessProjects.awaitIdle(project);
        Document document = fixture.getDocument();
        RecordingFix fix = new RecordingFix();

        // a daemon pass started by the new editor would replace the highlighter of the fix before the intentions are
        // collected - on a loaded machine it does
        Disposable daemonDisabled = Disposable.newDisposable();
        HeadlessUIThread.run(() -> DaemonCodeAnalyzer.getInstance(project).disableUpdateByTimer(daemonDisabled));
        Editor editor = HeadlessUIThread.compute(() -> EditorFactory.getInstance().createEditor(document, project));
        try {
            HeadlessUIThread.run(() -> {
                editor.getCaretModel().moveToOffset(2);
                HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.WARNING)
                    .range(0, 5)
                    .descriptionAndTooltip(LocalizeValue.of("warning with a fix"))
                    .registerFix(fix)
                    .createUnconditionally();
                setHighlighters(project, document, List.of(info));
            });

            CachedIntentions cachedIntentions = HeadlessUIThread.compute(() -> ShowIntentionActionsHandler.calcCachedIntentions(project, editor, file));

            assertThat(cachedIntentions.getAllActions())
                .extracting(cached -> IntentionActionDelegate.unwrap(cached.getAction()))
                .contains(fix);
            assertThat(fix.myAvailabilityThread.get()).as("availability is checked at least once").isNotNull();
            assertThat(fix.myCheckedOnUIThread.get()).as("no availability check runs on the UI thread").isFalse();
            assertThat(fix.myCheckedWithoutReadAccess.get()).as("every availability check runs under a read action").isFalse();
        }
        finally {
            HeadlessUIThread.run(() -> {
                setHighlighters(project, document, List.of());
                EditorFactory.getInstance().releaseEditor(editor);
                Disposer.dispose(daemonDisabled);
            });
        }
    }

    private static void setHighlighters(Project project, Document document, List<HighlightInfo> infos) {
        MarkupModelEx markup = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
        UpdateHighlightersUtilImpl.setHighlightersInRange(
            project,
            document,
            new TextRange(0, document.getTextLength()),
            null,
            new ArrayList<>(infos),
            markup,
            GROUP
        );
    }

    private static final class RecordingFix implements IntentionAction {
        private final AtomicReference<Thread> myAvailabilityThread = new AtomicReference<>();
        private final AtomicBoolean myCheckedOnUIThread = new AtomicBoolean();
        private final AtomicBoolean myCheckedWithoutReadAccess = new AtomicBoolean();

        @Override
        public LocalizeValue getText() {
            return LocalizeValue.of("Recording fix");
        }

        @Override
        public boolean isAvailable(Project project, Editor editor, PsiFile file) {
            myAvailabilityThread.set(Thread.currentThread());
            if (HeadlessUIThread.isUIThread()) {
                myCheckedOnUIThread.set(true);
            }
            if (!Application.get().isReadAccessAllowed()) {
                myCheckedWithoutReadAccess.set(true);
            }
            return true;
        }

        @Override
        public void invoke(Project project, Editor editor, PsiFile file) {
        }
    }
}
