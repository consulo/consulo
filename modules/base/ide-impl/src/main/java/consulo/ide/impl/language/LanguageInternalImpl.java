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
package consulo.ide.impl.language;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.EditorHighlighter;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.internal.EditorHighlighterCache;
import consulo.language.internal.LanguageInternal;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.FileContent;
import consulo.language.scratch.ScratchFileCreationHelper;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-08-20
 */
@Singleton
@ServiceImpl
public class LanguageInternalImpl implements LanguageInternal {
    @Override
    public void rememberEditorHighlight(FileContent fileContent, Document document) {
        fileContent.putUserData(
            EditorHighlighter.KEY,
            EditorHighlighterCache.getEditorHighlighterForCachesBuilding(document)
        );
    }

    @Override
    public String reformatScratch(Project project, Language language, String text) {
        return CommandProcessor.getInstance().<String>newCommand()
            .project(project)
            .inWriteAction()
            .compute(() -> {
                PsiFile psi = ScratchFileCreationHelper.parseHeader(project, language, text);
                if (psi != null) {
                    CodeStyleManager.getInstance(project).reformat(psi);
                }
                return psi == null ? text : psi.getText();
            });
    }
}
