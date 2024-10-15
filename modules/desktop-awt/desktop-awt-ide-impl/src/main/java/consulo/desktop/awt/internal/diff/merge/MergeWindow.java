/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.merge;

import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.merge.MergeRequest;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awt.LocalizeAction;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;

public class MergeWindow {
    @Nullable
    private final Project myProject;
    @Nonnull
    private final MergeRequest myMergeRequest;

    private MyDialog myWrapper;

    public MergeWindow(@Nullable Project project, @Nonnull MergeRequest mergeRequest) {
        myProject = project;
        myMergeRequest = mergeRequest;
    }

    @RequiredUIAccess
    protected void init() {
        MergeRequestProcessor processor = new MergeRequestProcessor(myProject, myMergeRequest) {
            @Override
            @RequiredUIAccess
            public void closeDialog() {
                myWrapper.doCancelAction();
            }

            @Override
            protected void setWindowTitle(@Nonnull LocalizeValue title) {
                myWrapper.setTitle(title);
            }

            @Override
            protected void rebuildSouthPanel() {
                myWrapper.rebuildSouthPanel();
            }
        };

        myWrapper = new MyDialog(processor);
        myWrapper.init();
    }

    @RequiredUIAccess
    public void show() {
        init();
        myWrapper.show();
    }

    // TODO: use WindowWrapper
    private static class MyDialog extends DialogWrapper {
        @Nonnull
        private final MergeRequestProcessor myProcessor;
        @Nonnull
        private final Wrapper mySouthPanel = new Wrapper();

        public MyDialog(@Nonnull MergeRequestProcessor processor) {
            super(processor.getProject(), true);
            myProcessor = processor;
        }

        @Override
        public void init() {
            super.init();
            Disposer.register(getDisposable(), myProcessor);
            getWindow().addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    myProcessor.init();
                }
            });
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return new MyPanel(myProcessor.getComponent());
        }

        @Nullable
        @Override
        protected JComponent createSouthPanel() {
            rebuildSouthPanel();
            return mySouthPanel;
        }

        @Nullable
        @Override
        @RequiredUIAccess
        public JComponent getPreferredFocusedComponent() {
            return myProcessor.getPreferredFocusedComponent();
        }

        @Nullable
        @Override
        protected String getDimensionServiceKey() {
            return StringUtil.notNullize(myProcessor.getContextUserData(DiffUserDataKeys.DIALOG_GROUP_KEY), "MergeDialog");
        }

        @Nonnull
        @Override
        protected LocalizeAction[] createActions() {
            MergeRequestProcessor.BottomActions bottomActions = myProcessor.getBottomActions();
            List<LocalizeAction> actions = ContainerUtil.skipNulls(Arrays.asList(bottomActions.resolveAction, bottomActions.cancelAction));
            if (bottomActions.resolveAction != null) {
                bottomActions.resolveAction.putValue(DialogWrapper.DEFAULT_ACTION, true);
            }
            return actions.toArray(new LocalizeAction[actions.size()]);
        }

        @Nonnull
        @Override
        protected LocalizeAction[] createLeftSideActions() {
            MergeRequestProcessor.BottomActions bottomActions = myProcessor.getBottomActions();
            List<LocalizeAction> actions = ContainerUtil.skipNulls(Arrays.asList(bottomActions.applyLeft, bottomActions.applyRight));
            return actions.toArray(new LocalizeAction[actions.size()]);
        }

        @Nonnull
        @Override
        protected LocalizeAction getOKAction() {
            MergeRequestProcessor.BottomActions bottomActions = myProcessor.getBottomActions();
            if (bottomActions.resolveAction != null) {
                return bottomActions.resolveAction;
            }
            return super.getOKAction();
        }

        @Nonnull
        @Override
        protected LocalizeAction getCancelAction() {
            MergeRequestProcessor.BottomActions bottomActions = myProcessor.getBottomActions();
            if (bottomActions.cancelAction != null) {
                return bottomActions.cancelAction;
            }
            return super.getCancelAction();
        }

        @Nullable
        @Override
        protected String getHelpId() {
            return myProcessor.getHelpId();
        }

        @Override
        @RequiredUIAccess
        public void doCancelAction() {
            if (!myProcessor.checkCloseAction()) {
                return;
            }
            super.doCancelAction();
        }

        public void rebuildSouthPanel() {
            mySouthPanel.setContent(super.createSouthPanel());
        }
    }

    private static class MyPanel extends JPanel {
        public MyPanel(@Nonnull JComponent content) {
            super(new BorderLayout());
            add(content, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension windowSize = AWTDiffUtil.getDefaultDiffWindowSize();
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(windowSize.width, size.width), Math.max(windowSize.height, size.height));
        }
    }
}
