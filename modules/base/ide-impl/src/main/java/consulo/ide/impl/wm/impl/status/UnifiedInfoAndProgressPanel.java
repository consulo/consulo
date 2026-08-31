package consulo.ide.impl.wm.impl.status;

import consulo.application.Application;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.internal.ProgressSuspender;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.wm.impl.status.InlineProgressIndicator;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.layout.DockLayout;
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

/**
 * The progress half of the status bar, built only from unified ui so every frontend gets the same one.
 *
 * @author VISTALL
 * @since 16/08/2021
 */
public class UnifiedInfoAndProgressPanel implements Disposable {
    private final DockLayout myLayout;

    private final Label myStatusLabel;

    /**
     * What the right hand side of the panel is showing at the moment - either the one running task, or the link
     * standing for several of them.
     */
    private final WrappedLayout myProgressSlot;

    private final Hyperlink myMultiProcessLink;

    private final UnifiedProcessPopup myPopup = new UnifiedProcessPopup();

    private final List<ProgressIndicatorEx> myOriginals = new ArrayList<>();
    private final List<TaskInfo> myInfos = new ArrayList<>();
    private final Map<InlineProgressIndicator, ProgressIndicatorEx> myInline2Original = new HashMap<>();
    private final MultiValuesMap<ProgressIndicatorEx, MyInlineProgressIndicator> myOriginal2Inlines =
        new MultiValuesMap<>();

    private boolean myShouldClosePopupAndOnProcessFinish;
    private boolean myDisposed;

    @RequiredUIAccess
    public UnifiedInfoAndProgressPanel() {
        myLayout = DockLayout.create();

        myStatusLabel = Label.create();
        myLayout.left(myStatusLabel);

        myMultiProcessLink = Hyperlink.create(LocalizeValue.empty(), event -> openProcessPopup());

        myProgressSlot = WrappedLayout.create();
        myLayout.right(myProgressSlot);
    }

    @RequiredUIAccess
    public void setStatusText(String text) {
        myStatusLabel.setText(LocalizeValue.of(text));
    }

    @RequiredUIAccess
    public void addProgress(ProgressIndicator original, TaskInfo info) {
        ProgressIndicatorEx originalEx = (ProgressIndicatorEx) original;

        boolean veryFirst = myOriginals.isEmpty();

        myOriginals.add(originalEx);
        myInfos.add(info);

        MyInlineProgressIndicator expanded = createInlineDelegate(info, originalEx, false);
        MyInlineProgressIndicator compact = createInlineDelegate(info, originalEx, true);

        myPopup.addIndicator(expanded);

        if (veryFirst && !myPopup.isShowing()) {
            buildInInlineIndicator(compact);
        }
        else {
            buildInProcessCount();
        }

        runQuery();
    }

    @RequiredUIAccess
    private void removeProgress(MyInlineProgressIndicator progress) {
        if (!myInline2Original.containsKey(progress)) {
            return; // already disposed
        }

        boolean last = myOriginals.size() == 1;
        boolean beforeLast = myOriginals.size() == 2;

        myPopup.removeIndicator(progress);

        ProgressIndicatorEx original = removeFromMaps(progress);
        if (myOriginals.contains(original)) {
            Disposer.dispose(progress);
            return;
        }

        if (last) {
            myProgressSlot.set((Component) null);

            if (myShouldClosePopupAndOnProcessFinish) {
                myPopup.hide();
            }
        }
        else if (myPopup.isShowing() || myOriginals.size() > 1) {
            buildInProcessCount();
        }
        else if (beforeLast) {
            buildInInlineIndicator(createInlineDelegate(myInfos.get(0), myOriginals.get(0), true));
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

    @RequiredUIAccess
    private void openProcessPopup() {
        if (myPopup.isShowing()) {
            return;
        }

        myPopup.show(myMultiProcessLink);

        if (!myOriginals.isEmpty()) {
            myShouldClosePopupAndOnProcessFinish = true;
            buildInProcessCount();
        }
        else {
            myShouldClosePopupAndOnProcessFinish = false;
        }
    }

    @RequiredUIAccess
    private void buildInProcessCount() {
        myMultiProcessLink.setText(getMultiProgressLinkText());

        myProgressSlot.set(myMultiProcessLink);
    }

    private LocalizeValue getMultiProgressLinkText() {
        ProgressIndicatorEx latest = ContainerUtil.getLastItem(myOriginals);
        String latestText = latest == null ? null : latest.getText().getNullIfEmpty();
        if (StringUtil.isEmptyOrSpaces(latestText) || myPopup.isShowing()) {
            return LocalizeValue.localizeTODO(
                myOriginals.size() + pluralizeProcess(myOriginals.size()) + " running…"
            );
        }

        int others = myOriginals.size() - 1;
        String trimmed = latestText.length() > 55 ? latestText.substring(0, 50) + "…" : latestText;
        return LocalizeValue.localizeTODO(trimmed + " (" + others + " more" + pluralizeProcess(others) + ")");
    }

    private static String pluralizeProcess(int count) {
        return count == 1 ? " process" : " processes";
    }

    @RequiredUIAccess
    private void buildInInlineIndicator(MyInlineProgressIndicator inline) {
        inline.updateProgressNow();

        myProgressSlot.set(inline.getUIComponent());
    }

    @RequiredUIAccess
    private MyInlineProgressIndicator createInlineDelegate(
        TaskInfo info,
        ProgressIndicatorEx original,
        boolean compact
    ) {
        Collection<MyInlineProgressIndicator> inlines = myOriginal2Inlines.get(original);
        if (inlines != null) {
            for (MyInlineProgressIndicator eachInline : inlines) {
                if (eachInline.isCompact() == compact) {
                    return eachInline;
                }
            }
        }

        MyInlineProgressIndicator inline = new MyInlineProgressIndicator(compact, info, original);

        myInline2Original.put(inline, original);
        myOriginal2Inlines.put(original, inline);

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
        return myPopup.isShowing();
    }

    @RequiredUIAccess
    public void setProcessWindowOpen(boolean open) {
        if (open) {
            openProcessPopup();
        }
        else {
            myPopup.hide();
        }
    }

    public Component getUIComponent() {
        return myLayout;
    }

    /**
     * A progress indicator reports from whatever thread the task runs on, so the access can not be taken from
     * the current one. The panel asks its own component first - the browser frontend has one ui per session,
     * and only the component knows which of them this panel belongs to.
     */
    private UIAccess uiAccess() {
        UIAccess uiAccess = myLayout.getUIAccess();
        return uiAccess != null ? uiAccess : Application.get().getLastUIAccess();
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
        MyInlineProgressIndicator(boolean compact, TaskInfo task, ProgressIndicatorEx original) {
            super(compact, task);
            myOriginal = original;
            original.addStateDelegate(this);
        }

        private @Nullable ProgressSuspender getSuspender() {
            ProgressIndicatorEx original = myOriginal;
            return original == null ? null : ProgressSuspender.getSuspender(original);
        }

        @Override
        public List<AnAction> createEastButtons() {
            List<AnAction> actions = new ArrayList<>(super.createEastButtons());
            actions.add(0, new SuspendAction());
            return actions;
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
            uiAccess().giveIfNeed(this::updateAndRepaint);
        }

        @Override
        protected void queueRunningUpdate(@RequiredUIAccess Runnable update) {
            uiAccess().give(update);
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
