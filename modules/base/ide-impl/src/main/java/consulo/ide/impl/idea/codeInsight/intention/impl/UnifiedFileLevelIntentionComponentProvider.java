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
package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.markup.GutterMark;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.internal.EditorNotificationBuilderEx;
import consulo.fileEditor.internal.EditorNotificationBuilderFactory;
import consulo.language.editor.impl.internal.daemon.FileLevelHighlightComponentBuilder;
import consulo.language.editor.impl.internal.daemon.FileLevelHighlightComponentProvider;
import consulo.language.editor.intention.IntentionAction;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedFileLevelIntentionComponentProvider implements FileLevelHighlightComponentProvider {
    private final Project myProject;
    private final EditorNotificationBuilderFactory myEditorNotificationBuilderFactory;

    @Inject
    public UnifiedFileLevelIntentionComponentProvider(Project project, EditorNotificationBuilderFactory editorNotificationBuilderFactory) {
        myProject = project;
        myEditorNotificationBuilderFactory = editorNotificationBuilderFactory;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Disposable createComponent(FileEditor fileEditor, FileLevelHighlightComponentBuilder builder) {
        EditorNotificationBuilderEx notification = (EditorNotificationBuilderEx) myEditorNotificationBuilderFactory.newBuilder();
        notification.withText(builder.getDescription());
        notification.withType(builder.getNotificationType());
        GutterMark gutterMark = builder.getGutterMark();
        if (gutterMark != null) {
            notification.withIcon(gutterMark.getIcon());
        }

        for (IntentionAction action : builder.getIntentionActions()) {
            LocalizeValue text = action.getText();
            notification.withAction(text, event -> builder.invokeIntention(action, text));
        }

        if (builder.hasIntentions()) {
            notification.withGearAction(event -> builder.showIntentionOptions(event.getComponent(), event.getInputDetails()));
        }

        return FileEditorManager.getInstance(myProject).addTopComponent(fileEditor, notification);
    }
}
