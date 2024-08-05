/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.codeInsight.navigation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.EditorEventMulticaster;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-08-05
 *
 * FIXME [VISTALL] maybe replace it by @TopicImpl?
 */
@ExtensionImpl
public class CtrlMouseHandlerProjectActivity implements PostStartupActivity, DumbAware {
    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        CtrlMouseHandler handler = project.getInstance(CtrlMouseHandler.class);

        EditorEventMulticaster eventMulticaster = EditorFactory.getInstance().getEventMulticaster();
        eventMulticaster.addEditorMouseListener(handler.getEditorMouseAdapter(), project);
        eventMulticaster.addEditorMouseMotionListener(handler.getEditorMouseMotionListener(), project);
        eventMulticaster.addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@Nonnull CaretEvent e) {
                handler.caretPositionChanged();
            }
        }, project);

        project.getMessageBus().connect().subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
                handler.disposeHighlighter();
                handler.cancelPreviousTooltip();
            }
        });
    }
}
