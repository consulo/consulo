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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.Presentation;
import consulo.ui.layout.HorizontalLayout;
import consulo.versionControlSystem.checkin.CommitProblem;
import consulo.versionControlSystem.checkin.CommitProblemWithDetails;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitStatus {
    private final Project myProject;
    private final HorizontalLayout myLayout;
    private final Label myLabel;
    private final Hyperlink myDetailsLink;

    private @Nullable CommitProblemWithDetails myProblemWithDetails;

    @RequiredUIAccess
    public UnifiedCommitStatus(Project project) {
        myProject = project;
        myLabel = Label.create(LocalizeValue.empty());
        myDetailsLink = Hyperlink.create(LocalizeValue.empty(), event -> {
            CommitProblemWithDetails problem = myProblemWithDetails;
            if (problem != null) {
                problem.showDetails(myProject);
            }
        });
        myDetailsLink.setVisible(false);

        myLayout = HorizontalLayout.create();
        myLayout.add(myLabel);
        myLayout.add(myDetailsLink);
        myLayout.setVisible(false);
    }

    public Component getComponent() {
        return myLayout;
    }

    @RequiredUIAccess
    public void showRunning() {
        myProblemWithDetails = null;
        myLabel.setForegroundColor(null);
        myLabel.setText(VcsLocalize.commitPanelChecksRunning());
        myDetailsLink.setVisible(false);
        myLayout.setVisible(true);
    }

    @RequiredUIAccess
    public void showProblem(LocalizeValue text, CommitProblem problem) {
        myLabel.setForegroundColor(TextAttribute.ERROR.getForegroundColor());
        myLabel.setText(text);

        if (problem instanceof CommitProblemWithDetails withDetails) {
            myProblemWithDetails = withDetails;
            LocalizeValue link = withDetails.getShowDetailsLink();
            myDetailsLink.setText(link.isEmpty() ? withDetails.getShowDetailsAction().map(Presentation.NO_MNEMONIC) : link);
            myDetailsLink.setVisible(true);
        }
        else {
            myProblemWithDetails = null;
            myDetailsLink.setVisible(false);
        }
        myLayout.setVisible(true);
    }

    @RequiredUIAccess
    public void hide() {
        myProblemWithDetails = null;
        myLayout.setVisible(false);
    }
}
