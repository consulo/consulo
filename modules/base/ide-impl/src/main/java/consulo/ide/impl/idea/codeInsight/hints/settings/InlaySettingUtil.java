/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.ide.impl.idea.codeInsight.hint.EditorFragmentComponent;
import consulo.language.Language;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.fileType.FileType;

import javax.swing.border.LineBorder;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-06-12
 */
public class InlaySettingUtil {
    private static final Key<Boolean> SETTINGS_EDITOR_MARKER = Key.create("inlay.settings.editor");

    public static boolean isInlaySettingsEditor(Editor editor) {
        return Boolean.TRUE.equals(editor.getUserData(SETTINGS_EDITOR_MARKER));
    }

    public static EditorTextField createEditor(Language language,
                                               Project project,
                                               Consumer<? super Editor> updateHints) {
        FileType fileType = language.getAssociatedFileType() != null
            ? language.getAssociatedFileType()
            : PlainTextFileType.INSTANCE;

        Document document = EditorFactory.getInstance().createDocument("");
        EditorTextField editorField = new EditorTextField(document,
            project,
            fileType,
            true,
            false) {
            @Override
            protected EditorEx createEditor() {
                EditorEx editor = super.createEditor();
                editor.putUserData(SETTINGS_EDITOR_MARKER, Boolean.TRUE);
                updateHints.accept(editor);
                return editor;
            }
        };
        editorField.setBorder(new LineBorder(JBColor.border()));
        editorField.addSettingsProvider(editor -> {
            editor.setVerticalScrollbarVisible(true);
            editor.setHorizontalScrollbarVisible(true);
            editor.getSettings().setAdditionalLinesCount(0);
            editor.getSettings().setAutoCodeFoldingEnabled(false);

            editor.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                public void documentChanged(DocumentEvent event) {
                    updateHints.accept(editor);
                }
            });

            editor.setBackgroundColor(EditorFragmentComponent.getBackgroundColor(editor, false));
            editor.setBorder(JBUI.Borders.empty());

            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, false);
            }
        });
        ReadAction.run(() -> editorField.setCaretPosition(0));
        return editorField;
    }
}
