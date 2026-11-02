package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.PowerSaveModeListener;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.internal.ProgressSuspender;
import consulo.application.internal.ProgressSuspenderListener;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.application.util.registry.Registry;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.ProgressBar;
import consulo.ui.ProgressBarStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.WrappedLayout;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiValuesMap;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The progress part of the status bar, built only from unified ui so every frontend gets the same one. One task shows
 * itself, several show a spinner and how many there are; a click on either opens the list of them.
 *
 * @author VISTALL
 * @since 2021-08-16
 */
public class UnifiedInfoAndProgressPanel implements Disposable {
    private static final String BACKGROUND_TASKS_GROUP = "BackgroundTasks";

    private final DockLayout myLayout;

    private final WrappedLayout myProgressSlot;

    private final HorizontalLayout myMultiProcess;
    private final Label myMultiProcessLabel;

    private final ProcessBalloon myBalloon = new ProcessBalloon();

    private final List<ProgressIndicatorEx> myOriginals = new ArrayList<>();
    private final List<TaskInfo> myInfos = new ArrayList<>();
    private final Map<InlineProgressIndicator, ProgressIndicatorEx> myInline2Original = new HashMap<>();
    private final MultiValuesMap<ProgressIndicatorEx, MyInlineProgressIndicator> myOriginal2Inlines =
        new MultiValuesMap<>();

    private final Supplier<@Nullable UIAccess> myOwnerUIAccess;

    private @Nullable Component mySlotContent;
    private boolean myShowingCount;
    private boolean myShouldCloseBalloonOnProcessFinish;
    private boolean myDisposed;

    @RequiredUIAccess
    public UnifiedInfoAndProgressPanel(Supplier<@Nullable UIAccess> ownerUIAccess) {
        myOwnerUIAccess = ownerUIAccess;
        myLayout = DockLayout.create();
        myLayout.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);

        ProgressBar spinner = ProgressBar.create();
        spinner.addStyle(ProgressBarStyle.SPINNER);
        spinner.addStyle(ProgressBarStyle.TRANSPARENT_BACKGROUND);
        spinner.setIndeterminate(true);

        myMultiProcessLabel = Label.create();
        myMultiProcessLabel.addStyle(LabelStyle.TRANSPARENT_BACKGROUND);

        myMultiProcess = HorizontalLayout.create();
        myMultiProcess.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        myMultiProcess.add(spinner);
        myMultiProcess.add(myMultiProcessLabel);
        installBalloonToggle(myMultiProcess);
        installBalloonToggle(myMultiProcessLabel);

        myProgressSlot = WrappedLayout.create();
        myLayout.right(myProgressSlot);

        Application application = Application.get();
        MessageBusConnection connection = application.getMessageBus().connect(this);
        connection.subscribe(PowerSaveModeListener.class, this::queueQuery);
        connection.subscribe(ProgressSuspenderListener.class, new ProgressSuspenderListener() {
            @Override
            public void suspendableProgressAppeared(ProgressSuspender suspender) {
                queueQuery();
            }

            @Override
            public void suspendedStatusChanged(ProgressSuspender suspender) {
                queueQuery();
            }
        });
    }

    private void queueQuery() {
        UIAccess uiAccess = uiAccess();
        if (uiAccess != null) {
            uiAccess.give(() -> {
                if (!myDisposed) {
                    runQuery();
                }
            });
        }
    }

    @RequiredUIAccess
    private void installBalloonToggle(Component component) {
        component.addClickListener(event -> toggleBalloon());
        component.addContextMenuListener(event -> showBackgroundTasksMenu(component, event.getInputDetails()));
    }

    @RequiredUIAccess
    private void toggleBalloon() {
        if (myBalloon.isShowing()) {
            myBalloon.hide();
        }
        else if (!myBalloon.isJustClosed()) {
            openBalloon(true);
        }
    }

    @RequiredUIAccess
    private static void showBackgroundTasksMenu(Component target, @Nullable InputDetails inputDetails) {
        ActionManager actionManager = ActionManager.getInstance();
        if (!(actionManager.getAction(BACKGROUND_TASKS_GROUP) instanceof ActionGroup group)) {
            return;
        }

        ActionPopupMenu menu = actionManager.createActionPopupMenu(ActionPlaces.STATUS_BAR_PLACE, group);
        menu.setTargetComponent(target);
        menu.show(target, inputDetails == null ? 0 : inputDetails.getX(), inputDetails == null ? 0 : inputDetails.getY());
    }

    @RequiredUIAccess
    public void addProgress(ProgressIndicator original, TaskInfo info) {
        ProgressIndicatorEx originalEx = (ProgressIndicatorEx) original;

        boolean veryFirst = myOriginals.isEmpty();

        myOriginals.add(originalEx);
        myInfos.add(info);

        MyInlineProgressIndicator card = createInlineDelegate(info, originalEx, InlineProgressIndicator.Mode.CARD);
        MyInlineProgressIndicator statusBar = createInlineDelegate(info, originalEx, InlineProgressIndicator.Mode.STATUS_BAR);

        myBalloon.addIndicator(card);

        if (veryFirst && !myBalloon.isShowing()) {
            buildInInlineIndicator(statusBar);
        }
        else {
            buildInProcessCount();
            if (myInfos.size() > 1 && Registry.is("ide.windowSystem.autoShowProcessPopup")) {
                openBalloon(false);
            }
        }

        runQuery();

        // a task announces itself from the thread which starts it and is shown a moment later, by which time a
        // short one is already over. A delegate attached after the end is never told of it, so the bar would go
        // on showing a task nobody is running
        if (originalEx.isFinished(info)) {
            removeProgress(statusBar);
            removeProgress(card);
        }
    }

    @RequiredUIAccess
    private void removeProgress(MyInlineProgressIndicator progress) {
        if (!myInline2Original.containsKey(progress)) {
            return; // already disposed
        }

        boolean last = myOriginals.size() == 1;
        boolean beforeLast = myOriginals.size() == 2;

        myBalloon.removeIndicator(progress);

        ProgressIndicatorEx original = removeFromMaps(progress);
        if (myOriginals.contains(original)) {
            Disposer.dispose(progress);
            return;
        }

        if (last) {
            myShowingCount = false;
            mySlotContent = null;
            myProgressSlot.set((Component) null);

            if (myShouldCloseBalloonOnProcessFinish) {
                myBalloon.hide();
            }
        }
        else if (myBalloon.isShowing() || myOriginals.size() > 1) {
            buildInProcessCount();
        }
        else if (beforeLast) {
            buildInInlineIndicator(createInlineDelegate(myInfos.get(0), myOriginals.get(0), InlineProgressIndicator.Mode.STATUS_BAR));
        }

        runQuery();

        Disposer.dispose(progress);
    }

    private ProgressIndicatorEx removeFromMaps(MyInlineProgressIndicator progress) {
        ProgressIndicatorEx original = myInline2Original.get(progress);

        myInline2Original.remove(progress);

        myOriginal2Inlines.remove(original, progress);
        if (myOriginal2Inlines.get(original) == null) {
            int originalIndex = myOriginals.indexOf(original);
            myOriginals.remove(originalIndex);
            myInfos.remove(originalIndex);
        }

        return original;
    }

    /**
     * @param reveal opens the list without taking the focus - a banner has something to tell about what the user just
     *               tried
     */
    @RequiredUIAccess
    public void setBanners(List<Component> banners, boolean reveal) {
        myBalloon.setBanners(banners);

        if (reveal && !banners.isEmpty() && !myBalloon.isShowing()) {
            showBalloon(true, false);
        }
    }

    @RequiredUIAccess
    private void openBalloon(boolean explicit) {
        showBalloon(explicit, explicit);
    }

    @RequiredUIAccess
    private void showBalloon(boolean explicit, boolean focus) {
        if (myBalloon.isShowing()) {
            return;
        }

        myShouldCloseBalloonOnProcessFinish = !myOriginals.isEmpty();
        if (myShouldCloseBalloonOnProcessFinish) {
            buildInProcessCount();
            myMultiProcessLabel.setText(getMultiProgressText(true));
        }

        Component content = mySlotContent;
        myBalloon.show(content != null ? content : myProgressSlot, explicit, focus);
    }

    @RequiredUIAccess
    private void buildInProcessCount() {
        myMultiProcessLabel.setText(getMultiProgressText());

        myShowingCount = true;
        mySlotContent = myMultiProcessLabel;
        myProgressSlot.set(myMultiProcess);
    }

    private LocalizeValue getMultiProgressText() {
        return getMultiProgressText(myBalloon.isShowing());
    }

    private LocalizeValue getMultiProgressText(boolean balloonShowing) {
        ProgressIndicatorEx latest = ContainerUtil.getLastItem(myOriginals);
        String latestText = latest == null ? null : latest.getText().getNullIfEmpty();
        if (StringUtil.isEmptyOrSpaces(latestText) || balloonShowing) {
            return LocalizeValue.localizeTODO(
                myOriginals.size() + pluralizeProcess(myOriginals.size()) + " running…"
            );
        }

        int others = myOriginals.size() - 1;
        String trimmed = StringUtil.truncateWithEllipsis(latestText, 50);
        return LocalizeValue.localizeTODO(trimmed + " (" + others + " more" + pluralizeProcess(others) + ")");
    }

    private static String pluralizeProcess(int count) {
        return count == 1 ? " process" : " processes";
    }

    @RequiredUIAccess
    private void buildInInlineIndicator(MyInlineProgressIndicator inline) {
        inline.updateProgressNow();

        myShowingCount = false;
        mySlotContent = inline.myText;
        myProgressSlot.set(inline.getUIComponent());
    }

    @RequiredUIAccess
    private MyInlineProgressIndicator createInlineDelegate(
        TaskInfo info,
        ProgressIndicatorEx original,
        InlineProgressIndicator.Mode mode
    ) {
        Collection<MyInlineProgressIndicator> inlines = myOriginal2Inlines.get(original);
        if (inlines != null) {
            for (MyInlineProgressIndicator eachInline : inlines) {
                if (eachInline.getMode() == mode) {
                    return eachInline;
                }
            }
        }

        MyInlineProgressIndicator inline = new MyInlineProgressIndicator(mode, info, original);

        myInline2Original.put(inline, original);
        myOriginal2Inlines.put(original, inline);

        if (mode == InlineProgressIndicator.Mode.STATUS_BAR) {
            installBalloonToggle(inline.getUIComponent());
            installBalloonToggle(inline.myText);
        }

        return inline;
    }

    /**
     * Asks every running task where it is - a task which was already over when it was added never reports a
     * change of its own, and would sit in the bar until something else moved.
     */
    @RequiredUIAccess
    private void runQuery() {
        for (InlineProgressIndicator indicator : new ArrayList<>(myInline2Original.keySet())) {
            indicator.updateProgressNow();
        }
    }

    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
        List<Pair<TaskInfo, ProgressIndicator>> result = new ArrayList<>();
        for (int i = 0; i < myOriginals.size(); i++) {
            result.add(Pair.create(myInfos.get(i), myOriginals.get(i)));
        }
        return result;
    }

    @RequiredUIAccess
    public boolean isProcessWindowOpen() {
        return myBalloon.isShowing();
    }

    @RequiredUIAccess
    public void setProcessWindowOpen(boolean open) {
        if (open) {
            openBalloon(true);
        }
        else {
            myBalloon.hide();
        }
    }

    public Component getUIComponent() {
        return myLayout;
    }

    /**
     * A progress indicator reports from whatever thread the task runs on, so the access can not be taken from
     * the current one. The panel asks its own component first - the browser frontend has one ui per session,
     * and only the component knows which of them this panel belongs to - and its owner while it is not attached.
     */
    private @Nullable UIAccess uiAccess() {
        UIAccess uiAccess = myLayout.getUIAccess();
        if (uiAccess == null) {
            uiAccess = myOwnerUIAccess.get();
        }
        return uiAccess != null && uiAccess.isValid() ? uiAccess : null;
    }

    @Override
    public void dispose() {
        myDisposed = true;

        for (InlineProgressIndicator indicator : new ArrayList<>(myInline2Original.keySet())) {
            Disposer.dispose(indicator);
        }

        myInline2Original.clear();
        myOriginal2Inlines.clear();
        myOriginals.clear();
        myInfos.clear();
    }

    private class MyInlineProgressIndicator extends InlineProgressIndicator {
        private class SuspendAction extends LegacyDumbAwareAction {
            private SuspendAction() {
                super(LocalizeValue.empty(), LocalizeValue.empty(), PlatformIconGroup.generalInspectionspause());
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                ProgressSuspender suspender = getSuspender();
                if (suspender == null) {
                    return;
                }

                if (suspender.isSuspended()) {
                    suspender.resumeProcess();
                }
                else {
                    suspender.suspendProcess(null);
                }
            }

            @Override
            public void update(AnActionEvent e) {
                ProgressSuspender suspender = getSuspender();
                e.getPresentation().setEnabledAndVisible(suspender != null);
                if (suspender == null) {
                    return;
                }

                boolean suspended = suspender.isSuspended();
                e.getPresentation().setText(
                    suspended ? ApplicationLocalize.actionResumeText() : ApplicationLocalize.actionPauseText()
                );
                e.getPresentation().setIcon(
                    suspended ? PlatformIconGroup.actionsResume() : PlatformIconGroup.generalInspectionspause()
                );
            }
        }

        private @Nullable ProgressIndicatorEx myOriginal;

        @RequiredUIAccess
        MyInlineProgressIndicator(Mode mode, TaskInfo task, ProgressIndicatorEx original) {
            super(mode, task);
            myOriginal = original;
            original.addStateDelegate(this);
        }

        private @Nullable ProgressSuspender getSuspender() {
            ProgressIndicatorEx original = myOriginal;
            return original == null ? null : ProgressSuspender.getSuspender(original);
        }

        @Override
        public LocalizeValue getText() {
            LocalizeValue text = super.getText();
            ProgressSuspender suspender = getSuspender();
            return suspender != null && suspender.isSuspended() ? suspender.getSuspendedText() : text;
        }

        @Override
        public List<AnAction> createEastButtons() {
            List<AnAction> actions = new ArrayList<>(super.createEastButtons());
            actions.add(0, new SuspendAction());
            return actions;
        }

        @Override
        @RequiredUIAccess
        public void updateProgressNow() {
            if (getMode() == Mode.CARD) {
                myProgress.setVisible(!PowerSaveMode.isEnabled() || !isPaintingIndeterminate());
            }
            super.updateProgressNow();

            if (myOriginal == ContainerUtil.getLastItem(myOriginals) && myShowingCount) {
                myMultiProcessLabel.setText(getMultiProgressText());
            }
        }

        @Override
        public void stop() {
            super.stop();
            updateProgress();
        }

        @Override
        protected boolean isFinished() {
            TaskInfo info = getInfo();
            return info == null || !myInfos.contains(info);
        }

        @Override
        public void finish(TaskInfo task) {
            super.finish(task);

            queueRunningUpdate(() -> {
                if (!myDisposed) {
                    removeProgress(this);
                }
            });
        }

        @Override
        protected void queueProgressUpdate() {
            UIAccess uiAccess = uiAccess();
            if (uiAccess != null) {
                uiAccess.giveIfNeed(this::updateAndRepaint);
            }
        }

        @Override
        protected void queueRunningUpdate(@RequiredUIAccess Runnable update) {
            UIAccess uiAccess = uiAccess();
            if (uiAccess != null) {
                uiAccess.give(update);
            }
        }

        @Override
        public void dispose() {
            super.dispose();
            myOriginal = null;
        }

        @Override
        protected void cancelRequest() {
            ProgressIndicatorEx original = myOriginal;
            if (original != null) {
                original.cancel();
            }
        }
    }
}
