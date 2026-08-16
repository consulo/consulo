// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.progress.TaskInfo;
import consulo.disposer.Disposable;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.ProgressBar;
import consulo.ui.ProgressBarStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.style.ComponentColors;

import javax.swing.JComponent;
import java.util.List;

public class InlineProgressIndicator extends ProgressIndicatorBase implements Disposable {
    private final ActionToolbar myToolbar;

    private class CancelAction extends LegacyDumbAwareAction {
        public CancelAction(LocalizeValue text, LocalizeValue description) {
            super(text, description, PlatformIconGroup.actionsClose());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            cancelRequest();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(myInfo != null && myInfo.isCancellable());
        }
    }

    protected final Label myText = Label.create();
    private final Label myText2 = Label.create();

    protected ProgressBar myProgress;

    private Component myComponent;

    private final boolean myCompact;
    private TaskInfo myInfo;

    private final Label myProcessName = Label.create();
    private boolean myDisposed;

    @RequiredUIAccess
    public InlineProgressIndicator(boolean compact, TaskInfo processInfo) {
        myCompact = compact;
        myInfo = processInfo;

        List<AnAction> actions = createEastButtons();

        ActionGroup group = ActionGroup.newImmutableBuilder().addAll(actions).build();

        ActionToolbar.Style style = myCompact ? ActionToolbar.Style.INPLACE : ActionToolbar.Style.HORIZONTAL;

        myToolbar = ActionToolbarFactory.getInstance().createActionToolbar("InlineProgressBar", group, style);
        myToolbar.updateActionsAsync();

        Component toolbar = myToolbar.getUIComponent();

        myText.addStyle(LabelStyle.TRANSPARENT_BACKGROUND);
        myText2.addStyle(LabelStyle.TRANSPARENT_BACKGROUND);
        myProcessName.addStyle(LabelStyle.TRANSPARENT_BACKGROUND);

        myProgress = ProgressBar.create();
        myProgress.addStyle(ProgressBarStyle.TRANSPARENT_BACKGROUND);

        if (myCompact) {
            myProgress.addStyle(ProgressBarStyle.SPINNER);

            HorizontalLayout layout = HorizontalLayout.create(5);
            layout.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);

            layout.add(myProgress);
            layout.add(myText);
            layout.add(toolbar);

            layout.setToolTipText(LocalizeValue.join(
                LocalizeValue.of(processInfo.getTitle() + ". "),
                IdeLocalize.progressTextClicktoviewprogresswindow()
            ));

            myComponent = layout;
        }
        else {
            myProcessName.setText(LocalizeValue.of(processInfo.getTitle()));

            DockLayout content = DockLayout.create();
            content.top(myText);
            content.center(myProgress);
            content.bottom(myText2);
            content.right(toolbar);
            content.addBorders(BorderStyle.EMPTY, null, 2);

            DockLayout root = DockLayout.create();
            root.top(myProcessName);
            root.center(content);
            root.addBorders(BorderStyle.LINE, ComponentColors.BORDER, 1);

            myComponent = root;
        }
    }

    public List<AnAction> createEastButtons() {
        return List.of(new CancelAction(myInfo.getCancelTextValue(), myInfo.getCancelTooltipTextValue()));
    }

    protected void cancelRequest() {
        cancel();
    }

    @RequiredUIAccess
    public void updateProgress() {
        queueProgressUpdate();
    }

    @RequiredUIAccess
    public void updateAndRepaint() {
        if (isDisposed()) {
            return;
        }

        updateProgressNow();
    }

    @RequiredUIAccess
    public void updateProgressNow() {
        if (isPaintingIndeterminate()) {
            myProgress.setIndeterminate(true);
        }
        else {
            myProgress.setIndeterminate(false);
            myProgress.setMinimum(0);
            myProgress.setMaximum(100);
        }
        if (getFraction() > 0) {
            myProgress.setValue((int) (getFraction() * 99 + 1));
        }

        LocalizeValue text = getText();
        myText.setText(text);
        myText2.setText(getText2());

        if (myCompact && text == LocalizeValue.empty()) {
            myText.setText(LocalizeValue.of(myInfo.getTitle()));
        }

        if (isStopping()) {
            if (myCompact) {
                myText.setText(IdeLocalize.progressTextStopping(text));
            }
            else {
                myProcessName.setText(IdeLocalize.progressTextStopping(LocalizeValue.of(myInfo.getTitle())));
                myText.setEnabled(false);
                myText2.setEnabled(false);
            }
            myProgress.setEnabled(false);
        }
        else {
            myText.setEnabled(true);
            myText2.setEnabled(true);
            myProgress.setEnabled(true);
        }

        myToolbar.updateActionsAsync();
    }

    protected boolean isPaintingIndeterminate() {
        return isIndeterminate() || getFraction() == 0;
    }

    private boolean isStopping() {
        return wasStarted() && (isCanceled() || !isRunning()) && !isFinished();
    }

    protected boolean isFinished() {
        return false;
    }

    @RequiredUIAccess
    protected void queueProgressUpdate() {
        updateAndRepaint();
    }

    protected void queueRunningUpdate(Runnable update) {
        update.run();
    }

    @Override
    @RequiredUIAccess
    protected void onProgressChange() {
        updateProgress();
    }

    public Component getUIComponent() {
        return myComponent;
    }

    public JComponent getComponent() {
        return (JComponent) TargetAWT.to(myComponent);
    }

    public boolean isCompact() {
        return myCompact;
    }

    public TaskInfo getInfo() {
        return myInfo;
    }

    @Override
    public void dispose() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        myComponent = null;
        myProgress = null;
        myInfo = null;
    }

    private boolean isDisposed() {
        return myDisposed;
    }
}
