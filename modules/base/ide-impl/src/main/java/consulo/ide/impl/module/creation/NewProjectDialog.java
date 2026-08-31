/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.module.creation;

import consulo.disposer.Disposer;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * @author VISTALL
 * @since 2014-06-04
 */
public class NewProjectDialog extends DialogWrapper {
    private UnifiedNewProjectPanel myProjectPanel;

    private Runnable myOkAction;
    private Runnable myCancelAction;

    @RequiredUIAccess
    public NewProjectDialog(@Nullable Project project, @Nullable VirtualFile moduleHome) {
        super(project, true);
        setResizable(false);

        myProjectPanel = new UnifiedNewProjectPanel(getDisposable(), moduleHome) {
            @Override
            @RequiredUIAccess
            protected @Nullable Component buildSouthPanel() {
                return null;
            }

            @Override
            @RequiredUIAccess
            public void setOKActionEnabled(boolean enabled) {
                NewProjectDialog.this.setOKActionEnabled(enabled);
            }

            @Override
            @RequiredUIAccess
            public void setOKActionText(LocalizeValue text) {
                NewProjectDialog.this.setOKButtonText(text);
            }

            @Override
            @RequiredUIAccess
            public void setCancelText(LocalizeValue text) {
                NewProjectDialog.this.setCancelButtonText(text);
            }

            @Override
            public void setOKAction(@Nullable Runnable action) {
                myOkAction = action;
            }

            @Override
            public void setCancelAction(@Nullable Runnable action) {
                myCancelAction = action;
            }
        };

        setTitle(moduleHome != null ? IdeLocalize.titleAddModule() : IdeLocalize.titleNewProject());

        setOKActionEnabled(false);
        init();
    }

    public UnifiedNewProjectPanel getProjectPanel() {
        return myProjectPanel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getCancelAction(), getOKAction()};
    }

    @Override
    public void doCancelAction(AWTEvent source) {
        if (source instanceof WindowEvent) {
            // if it's window event - close it via X
            super.doCancelAction();
            return;
        }
        super.doCancelAction(source);
    }

    @Override
    public void doCancelAction() {
        if (myCancelAction != null) {
            myCancelAction.run();
        }
        else {
            super.doCancelAction();
        }
    }

    @Override
    protected void doOKAction() {
        if (myOkAction != null) {
            myOkAction.run();
        }
        else {
            super.doOKAction();
        }
    }

    @Override
    protected void dispose() {
        myProjectPanel.finish();
        Disposer.dispose(myProjectPanel);

        super.dispose();
    }

    @Override
    @RequiredUIAccess
    protected void initRootPanel(JPanel root) {
        root.add((JComponent) TargetAWT.to(myProjectPanel.getLayout()), BorderLayout.CENTER);
    }

    @Override
    protected @Nullable String getDimensionServiceKey() {
        Dimension defaultWindowSize = TargetAWT.to(WelcomeFrameManager.getDefaultWindowSize());
        setSize(defaultWindowSize.width, defaultWindowSize.height);
        return "NewProjectDialog";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        throw new IllegalArgumentException();
    }
}
