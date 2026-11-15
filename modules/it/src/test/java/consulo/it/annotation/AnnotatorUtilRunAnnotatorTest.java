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
package consulo.it.annotation;

import consulo.application.ReadAction;
import consulo.document.util.TextRange;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotatorUtil;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Annotations created for a file outside the highlighting passes, e.g. from the problems of an external tool.
 *
 * @author VISTALL
 * @since 2026-11-15
 */
@ExtendWith(HeadlessProjectExtension.class)
public class AnnotatorUtilRunAnnotatorTest {
    @Test
    public void annotationsOfTheAnnotatorAreReturned(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/annotated.sand", "class Item {}\n");

        List<Annotation> annotations = ReadAction.compute(() -> AnnotatorUtil.runAnnotator(
            (element, holder) -> {
                holder.newAnnotation(HighlightSeverity.ERROR, LocalizeValue.of("external error")).range(new TextRange(0, 5)).create();
                holder.newAnnotation(HighlightSeverity.WARNING, LocalizeValue.of("external warning")).range(new TextRange(6, 10)).create();
            },
            file,
            file
        ));

        assertThat(annotations).hasSize(2);
        assertThat(annotations.get(0).getSeverity()).isEqualTo(HighlightSeverity.ERROR);
        assertThat(annotations.get(0).getMessage().get()).isEqualTo("external error");
        assertThat(annotations.get(0).getStartOffset()).isEqualTo(0);
        assertThat(annotations.get(0).getEndOffset()).isEqualTo(5);
        assertThat(annotations.get(1).getSeverity()).isEqualTo(HighlightSeverity.WARNING);
    }
}
