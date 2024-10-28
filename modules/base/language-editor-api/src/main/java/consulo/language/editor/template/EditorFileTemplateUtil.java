/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.template;

import consulo.codeEditor.Editor;
import consulo.language.editor.util.EditorHelper;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-03-27
 */
public class EditorFileTemplateUtil {
    @RequiredUIAccess
    public static void startLiveTemplate(@Nonnull PsiFile file) {
        startLiveTemplate(file, Collections.emptyMap());
    }

    @RequiredUIAccess
    public static void startLiveTemplate(@Nonnull PsiFile file, @Nonnull Map<String, String> defaultValues) {
        Editor editor = EditorHelper.openInEditor(file);
        if (editor == null) {
            return;
        }

        Project project = file.getProject();
        TemplateManager templateManager = TemplateManager.getInstance(project);

        Template template = templateManager.createTemplate("", "", file.getText());
        template.setInline(true);
        int count = template.getSegmentsCount();
        if (count == 0) {
            return;
        }

        Set<String> variables = new HashSet<>();
        for (int i = 0; i < count; i++) {
            variables.add(template.getSegmentName(i));
        }
        variables.removeAll(Template.INTERNAL_VARS_SET);
        for (String variable : variables) {
            String defaultValue = defaultValues.getOrDefault(variable, variable);
            template.addVariable(variable, null, '"' + defaultValue + '"', true);
        }

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .inWriteAction()
            .run(() -> editor.getDocument().setText(template.getTemplateText()));

        editor.getCaretModel().moveToOffset(0);  // ensures caret at the start of the template
        templateManager.startTemplate(editor, template);
    }
}
