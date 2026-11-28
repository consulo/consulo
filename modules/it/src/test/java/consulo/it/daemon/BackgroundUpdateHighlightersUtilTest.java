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
package consulo.it.daemon;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.Document;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.highlight.BackgroundUpdateHighlightersUtil;
import consulo.language.editor.impl.internal.highlight.HighlightingSessionImpl;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A highlighting pass applies its infos to the document markup straight from its background thread; a highlighter of
 * the same range and layer is reused by the next apply, the ones nobody reuses are disposed.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class BackgroundUpdateHighlightersUtilTest {
    private static final int GROUP = 0x8A11;

    @Test
    public void infosAreAppliedAndRecycledFromTheBackground(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.configureByText("some.sand", "class Item { \"body\" }\n");
        Project project = fixture.getProject();
        Document document = fixture.getDocument();

        apply(project, file, document, List.of(warning(0, 5, "first")));
        List<HighlightInfo> applied = infosOfGroup(project, document);
        assertThat(applied).hasSize(1);
        RangeHighlighter first = applied.get(0).getHighlighter();
        assertThat(first).isNotNull();
        assertThat(first.getStartOffset()).isEqualTo(0);
        assertThat(first.getEndOffset()).isEqualTo(5);

        apply(project, file, document, List.of(warning(0, 5, "second")));
        List<HighlightInfo> reapplied = infosOfGroup(project, document);
        assertThat(reapplied).hasSize(1);
        assertThat(reapplied.get(0).getDescription().get()).isEqualTo("second");
        assertThat(reapplied.get(0).getHighlighter()).as("the highlighter of the same range and layer is reused").isSameAs(first);

        apply(project, file, document, List.of(warning(6, 10, "third")));
        List<HighlightInfo> moved = infosOfGroup(project, document);
        assertThat(moved).hasSize(1);
        assertThat(moved.get(0).getStartOffset()).isEqualTo(6);
        assertThat(first.isValid()).as("the highlighter nobody reused is disposed").isFalse();

        apply(project, file, document, List.of());
        assertThat(infosOfGroup(project, document)).isEmpty();
    }

    private static HighlightInfo warning(int start, int end, String description) {
        return HighlightInfo.newHighlightInfo(HighlightInfoType.WARNING)
            .range(start, end)
            .descriptionAndTooltip(LocalizeValue.of(description))
            .createUnconditionally();
    }

    private static void apply(Project project, PsiFile file, Document document, List<HighlightInfo> infos) throws Exception {
        Application.get().executeOnPooledThread(() -> {
            DaemonProgressIndicator indicator = new DaemonProgressIndicator();
            ProgressManager.getInstance().runProcess(() -> ReadAction.run(() -> {
                HighlightingSessionImpl.getOrCreateHighlightingSession(file, indicator, null);
                BackgroundUpdateHighlightersUtil.setHighlightersToEditor(
                    project,
                    file,
                    document,
                    0,
                    document.getTextLength(),
                    infos,
                    GROUP
                );
            }), indicator);
        }).get();
    }

    private static List<HighlightInfo> infosOfGroup(Project project, Document document) {
        return ReadAction.compute(() -> {
            List<HighlightInfo> result = new ArrayList<>();
            DaemonCodeAnalyzer.processHighlights(document, project, null, 0, document.getTextLength(), info -> {
                if (((HighlightInfoImpl) info).getGroup() == GROUP) {
                    result.add(info);
                }
                return true;
            });
            // the markup must hold no highlighter of the group that is not reachable through an info
            long highlighters = 0;
            for (RangeHighlighter highlighter : DocumentMarkupModel.forDocument(document, project, true).getAllHighlighters()) {
                if (highlighter.getErrorStripeTooltip() instanceof HighlightInfoImpl info && info.getGroup() == GROUP) {
                    highlighters++;
                }
            }
            assertThat(highlighters).isEqualTo(result.size());
            return result;
        });
    }
}
