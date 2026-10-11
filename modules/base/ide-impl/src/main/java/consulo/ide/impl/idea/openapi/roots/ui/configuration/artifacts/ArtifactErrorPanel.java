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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.ui.ArtifactProblemQuickFix;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public class ArtifactErrorPanel {
    private final ArtifactEditorImpl myArtifactEditor;

    private @Nullable ArtifactProblem myProblem;

    private @Nullable DockLayout myPanel;
    private @Nullable Label myIconLabel;
    private @Nullable HtmlLabel myErrorLabel;
    private @Nullable Button myFixButton;

    public ArtifactErrorPanel(ArtifactEditorImpl artifactEditor) {
        myArtifactEditor = artifactEditor;
    }

    @RequiredUIAccess
    public Component createUIComponent() {
        Label iconLabel = Label.create();
        HtmlLabel errorLabel = HtmlLabel.create(LocalizeValue.empty());
        Button fixButton = Button.create(LocalizeValue.empty(), this::onFix);

        DockLayout panel = DockLayout.create();
        panel.left(iconLabel);
        panel.center(errorLabel);
        panel.right(fixButton);

        myPanel = panel;
        myIconLabel = iconLabel;
        myErrorLabel = errorLabel;
        myFixButton = fixButton;

        update();
        return panel;
    }

    @RequiredUIAccess
    public void showProblem(@Nullable ArtifactProblem problem) {
        myProblem = problem;
        update();
    }

    @RequiredUIAccess
    private void update() {
        DockLayout panel = myPanel;
        Label iconLabel = myIconLabel;
        HtmlLabel errorLabel = myErrorLabel;
        Button fixButton = myFixButton;
        if (panel == null || iconLabel == null || errorLabel == null || fixButton == null) {
            return;
        }

        ArtifactProblem problem = myProblem;
        if (problem == null) {
            panel.setVisible(false);
            return;
        }

        iconLabel.setImage(problem.error() ? PlatformIconGroup.generalError() : PlatformIconGroup.generalWarning());
        errorLabel.setText(LocalizeValue.of(problem.message()));

        List<ArtifactProblemQuickFix> quickFixes = problem.quickFixes();
        fixButton.setVisible(!quickFixes.isEmpty());
        if (quickFixes.size() == 1) {
            fixButton.setText(LocalizeValue.of(quickFixes.get(0).getActionName()));
        }
        else {
            fixButton.setText(ProjectLocalize.artifactProblemFixesButton());
        }
        panel.setVisible(true);
    }

    @RequiredUIAccess
    private void onFix(ClickEvent event) {
        ArtifactProblem problem = myProblem;
        if (problem == null || problem.quickFixes().isEmpty()) {
            return;
        }

        List<ArtifactProblemQuickFix> quickFixes = problem.quickFixes();
        if (quickFixes.size() == 1) {
            performFix(quickFixes.get(0));
            return;
        }

        JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<ArtifactProblemQuickFix>(null, quickFixes) {
            @Override
            public String getTextFor(ArtifactProblemQuickFix value) {
                return value.getActionName();
            }

            @Override
            @RequiredUIAccess
            public PopupStep onChosen(ArtifactProblemQuickFix selectedValue, boolean finalChoice) {
                performFix(selectedValue);
                return FINAL_CHOICE;
            }
        }).showBy(event.getComponent(), event.getInputDetails());
    }

    @RequiredUIAccess
    private void performFix(ArtifactProblemQuickFix quickFix) {
        quickFix.performFix(myArtifactEditor.getContext());
        myArtifactEditor.queueValidation();
    }
}
