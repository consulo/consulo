// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.ui.impl.internal.wm.statusBar;

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
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;

import java.util.List;

public class InlineProgressIndicator extends ProgressIndicatorBase implements Disposable {
    /**
     * Where the indicator is shown, which decides how much of the task it tells.
     */
    public enum Mode {
        /**
         * A spinner, the text and a cancel button in one line - the status bar, where there is no room for a bar
         */
        STATUS_BAR,
        /**
         * The title with the buttons, a bar and one line of detail - a row of the list of running tasks
         */
        CARD
    }

    private final ActionToolbar myToolbar;

    private class CancelAction extends DumbAwareAction implements AnActionWithSyncUpdate {
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
            TaskInfo info = myInfo;
            e.getPresentation().setEnabledAndVisible(info != null && info.isCancellable());
        }
    }

    protected final Label myText = Label.create();

    protected ProgressBar myProgress;

    private Component myComponent;

    private final Mode myMode;
    private volatile TaskInfo myInfo;

    private final Label myProcessName = Label.create();
    private boolean myDisposed;

    @RequiredUIAccess
    public InlineProgressIndicator(Mode mode, TaskInfo processInfo) {
        myMode = mode;
        myInfo = processInfo;

        List<AnAction> actions = createEastButtons();

        ActionGroup group = ActionGroup.newImmutableBuilder().addAll(actions).build();

        myToolbar = ActionToolbarFactory.getInstance().createActionToolbar("InlineProgressBar", group, ActionToolbar.Style.INPLACE);
        myToolbar.setTargetUIComponent(null);
        myToolbar.updateActionsAsync();

        Component toolbar = myToolbar.getUIComponent();

        myText.addStyle(LabelStyle.TRANSPARENT_BACKGROUND);
        myProcessName.addStyle(LabelStyle.TRANSPARENT_BACKGROUND);

        myProgress = ProgressBar.create();
        myProgress.addStyle(ProgressBarStyle.TRANSPARENT_BACKGROUND);

        if (myMode == Mode.STATUS_BAR) {
            myProgress.addStyle(ProgressBarStyle.SPINNER);
            myProgress.setIndeterminate(true);

            HorizontalLayout layout = HorizontalLayout.create();
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

            myText.addStyle(LabelStyle.FONT_SMALL);
            myText.setForegroundColor(ComponentColors.INFO_FOREGROUND);

            DockLayout header = DockLayout.create();
            header.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
            header.center(myProcessName);
            header.right(toolbar);

            DockLayout root = DockLayout.create();
            root.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
            root.top(header);
            root.center(myProgress);
            root.bottom(myText);
            root.paddingBuilder().verticalSet(Space.SMALL).leftSet(Space.MEDIUM).rightSet(Space.SMALL).apply();

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
        if (myMode == Mode.CARD) {
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
        }

        LocalizeValue text = getText();
        boolean stopping = isStopping();
        if (myMode == Mode.STATUS_BAR) {
            LocalizeValue shown = text.isEmpty() ? LocalizeValue.of(myInfo.getTitle()) : text;
            myText.setText(stopping ? IdeLocalize.progressTextStopping(shown) : shown);
        }
        else {
            myText.setText(detailText(text, getText2()));

            LocalizeValue title = LocalizeValue.of(myInfo.getTitle());
            myProcessName.setText(stopping ? IdeLocalize.progressTextStopping(title) : title);
        }

        myText.setEnabled(!stopping);
        myProgress.setEnabled(!stopping);

        myToolbar.updateActionsAsync();
    }

    private static LocalizeValue detailText(LocalizeValue text, LocalizeValue text2) {
        String first = text.get();
        String second = text2.get();
        if (StringUtil.isEmptyOrSpaces(second)) {
            return text;
        }
        if (StringUtil.isEmptyOrSpaces(first)) {
            return text2;
        }
        return LocalizeValue.of(first + " \u00B7 " + second);
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

    public Mode getMode() {
        return myMode;
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
