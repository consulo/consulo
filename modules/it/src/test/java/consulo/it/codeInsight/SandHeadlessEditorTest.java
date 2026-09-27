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

import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.it.internal.editor.HeadlessEditor;
import consulo.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seam the daemon analyses through: an editor obtained from {@link EditorFactory}, wrapped by
 * {@link TextEditorProvider} into a {@link TextEditor}, carrying a background highlighter. A null highlighter is what
 * silently stops highlighting, so it is asserted here rather than discovered as a timeout later.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SandHeadlessEditorTest {
    @Test
    public void editorFromTheFactoryCarriesABackgroundHighlighter(CodeInsightTestFixture fixture) throws Exception {
        fixture.configureByText("some.sand", "class Item { \"body\" }\n");

        Project project = fixture.getProject();
        Document document = fixture.getDocument();
        EditorFactory editorFactory = EditorFactory.getInstance();

        Editor editor = project.getUIAccess().giveAsync(() -> editorFactory.createEditor(document, project)).join();
        try {
            assertThat(editor).as("the headless factory answers its own implementation").isInstanceOf(HeadlessEditor.class);
            assertThat(editor.getDocument()).isSameAs(document);
            assertThat(editor.getColorsScheme()).as("every pass gets the editor's scheme").isNotNull();

            TextEditor textEditor = ReadAction.compute(() -> TextEditorProvider.getInstance().getTextEditor(editor));
            assertThat(textEditor).as("a plain editor wraps into a TextEditor").isNotNull();

            BackgroundEditorHighlighter highlighter = ReadAction.compute(textEditor::getBackgroundHighlighter);
            assertThat(highlighter).as("a null highlighter silently stops all analysis").isNotNull();
        }
        finally {
            project.getUIAccess().giveAsync(() -> {
                editorFactory.releaseEditor(editor);
                return null;
            }).join();
        }
    }
}
