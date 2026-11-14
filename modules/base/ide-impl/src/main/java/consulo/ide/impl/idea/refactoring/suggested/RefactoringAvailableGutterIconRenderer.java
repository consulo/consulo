// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.awt.*;

public class RefactoringAvailableGutterIconRenderer extends GutterIconRenderer {
    private final LocalizeValue myTooltip;

    public RefactoringAvailableGutterIconRenderer(LocalizeValue tooltip) {
        myTooltip = tooltip;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.gutterSuggestedrefactoringbulb();
    }

    @Override
    public LocalizeValue getTooltipValue() {
        return myTooltip;
    }

    @Override
    public boolean isNavigateAction() {
        return true;
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.RIGHT;
    }

    @Override
    public AnAction getClickAction() {
        return new AnAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                Project project = e.getRequiredData(Project.KEY);
                Editor editor = e.getRequiredData(Editor.KEY);

                EditorGutterComponentEx gutterComponent = ((EditorEx) editor).getGutterComponentEx();
                Point anchor = gutterComponent.getCenterPoint(RefactoringAvailableGutterIconRenderer.this);

                PerformSuggestedRefactoring.performSuggestedRefactoring(
                    project,
                    editor,
                    gutterComponent.getComponent(),
                    anchor,
                    true,
                    ActionPlaces.EDITOR_GUTTER
                );
            }
        };
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other == this;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
