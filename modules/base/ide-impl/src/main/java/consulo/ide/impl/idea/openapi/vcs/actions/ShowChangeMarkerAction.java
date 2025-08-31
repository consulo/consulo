/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.vcs.ex.LineStatusTracker;
import consulo.ide.impl.idea.openapi.vcs.ex.LineStatusTrackerDrawing;
import consulo.ide.impl.idea.openapi.vcs.ex.Range;
import consulo.ide.impl.idea.openapi.vcs.impl.LineStatusTrackerManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.versionControlSystem.action.AbstractVcsAction;
import consulo.versionControlSystem.action.VcsContext;
import jakarta.annotation.Nonnull;

/**
 * @author lesya
 */
public abstract class ShowChangeMarkerAction extends AbstractVcsAction {
    protected final ChangeMarkerContext myChangeMarkerContext;

    protected abstract Range extractRange(LineStatusTracker lineStatusTracker, int line, Editor editor);

    public ShowChangeMarkerAction(final Range range, final LineStatusTracker lineStatusTracker, final Editor editor) {
        myChangeMarkerContext = new ChangeMarkerContext() {
            @Override
            public Range getRange(VcsContext dataContext) {
                return range;
            }

            @Override
            public LineStatusTracker getLineStatusTracker(VcsContext dataContext) {
                return lineStatusTracker;
            }

            @Override
            public Editor getEditor(VcsContext dataContext) {
                return editor;
            }
        };
    }

    @Override
    protected boolean forceSyncUpdate(@Nonnull AnActionEvent e) {
        return true;
    }

    public ShowChangeMarkerAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nonnull Image icon) {
        super(text, description, icon);
        myChangeMarkerContext = new ChangeMarkerContext() {
            @Override
            public Range getRange(VcsContext context) {
                Editor editor = getEditor(context);
                if (editor == null) {
                    return null;
                }

                LineStatusTracker lineStatusTracker = getLineStatusTracker(context);
                if (lineStatusTracker == null) {
                    return null;
                }

                return extractRange(lineStatusTracker, editor.getCaretModel().getLogicalPosition().line, editor);
            }

            @Override
            public LineStatusTracker getLineStatusTracker(VcsContext dataContext) {
                Editor editor = getEditor(dataContext);
                if (editor == null) {
                    return null;
                }
                Project project = dataContext.getProject();
                if (project == null) {
                    return null;
                }
                return LineStatusTrackerManager.getInstance(project).getLineStatusTracker(editor.getDocument());
            }

            @Override
            public Editor getEditor(VcsContext dataContext) {
                return dataContext.getEditor();
            }
        };
    }

    private boolean isActive(VcsContext context) {
        return myChangeMarkerContext.getRange(context) != null;
    }

    @Override
    protected void update(@Nonnull VcsContext context, @Nonnull Presentation presentation) {
        boolean active = isActive(context);
        presentation.setEnabled(active);
        presentation.setVisible(context.getEditor() != null || ActionPlaces.isToolbarPlace(context.getPlace()));
    }

    @Override
    @RequiredUIAccess
    protected void actionPerformed(@Nonnull VcsContext context) {
        Editor editor = myChangeMarkerContext.getEditor(context);
        LineStatusTracker lineStatusTracker = myChangeMarkerContext.getLineStatusTracker(context);
        Range range = myChangeMarkerContext.getRange(context);

        LineStatusTrackerDrawing.moveToRange(range, editor, lineStatusTracker);
    }

    protected interface ChangeMarkerContext {
        Range getRange(VcsContext dataContext);

        LineStatusTracker getLineStatusTracker(VcsContext dataContext);

        Editor getEditor(VcsContext dataContext);
    }
}
