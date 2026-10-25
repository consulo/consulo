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
package consulo.codeEditor.impl.internal.inspection;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.markup.InspectionWidgetActionProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;

import java.util.ArrayList;
import java.util.List;

/**
 * The actions of the inspection widget of an editor: one per {@link InspectionWidgetActionProvider} that answers.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public final class InspectionWidgetActions {
    private InspectionWidgetActions() {
    }

    /**
     * Whether the editor shows an inspection widget at all - the editors of files and of diffs do.
     */
    public static boolean isShownIn(Editor editor) {
        EditorKind kind = editor.getEditorKind();
        return editor.getProject() != null && (kind == EditorKind.MAIN_EDITOR || kind == EditorKind.DIFF);
    }

    /**
     * @param parentDisposable the life of the editor; a created action that is {@link Disposable} goes with it
     */
    @RequiredUIAccess
    public static List<AnAction> create(Editor editor, Disposable parentDisposable) {
        List<AnAction> actions = new ArrayList<>();
        Application.get().getExtensionPoint(InspectionWidgetActionProvider.class).forEach(provider -> {
            AnAction action = provider.createAction(editor);
            if (action != null) {
                actions.add(action);
                if (action instanceof Disposable disposable) {
                    Disposer.register(parentDisposable, disposable);
                }
            }
        });
        return actions;
    }
}
