// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.ui.navigation;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.GenericListComponentUpdater;
import consulo.ui.ex.popup.JBPopup;
import consulo.usage.Usage;
import consulo.usage.UsageView;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public abstract class BackgroundUpdaterTaskBase<T> extends Task.Backgroundable {
    protected JBPopup myPopup;
    private GenericListComponentUpdater<T> myUpdater;
    private SimpleReference<? extends UsageView> myUsageView;
    private final Collection<T> myData;

    private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private final Object lock = new Object();

    private volatile boolean myCanceled;
    private volatile boolean myFinished;
    private volatile ProgressIndicator myIndicator;

    public BackgroundUpdaterTaskBase(@Nullable Project project, @Nonnull LocalizeValue title, @Nullable Comparator<T> comparator) {
        super(project, title);
        myData = comparator == null ? new ArrayList<>() : new TreeSet<>(comparator);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public BackgroundUpdaterTaskBase(@Nullable Project project, @Nonnull String title, @Nullable Comparator<T> comparator) {
        this(project, LocalizeValue.of(title), comparator);
    }

    @TestOnly
    public GenericListComponentUpdater<T> getUpdater() {
        return myUpdater;
    }

    public void init(
        @Nonnull JBPopup popup,
        @Nonnull GenericListComponentUpdater<T> updater,
        @Nonnull SimpleReference<? extends UsageView> usageView
    ) {
        myPopup = popup;
        myUpdater = updater;
        myUsageView = usageView;
    }

    public abstract String getCaption(int size);

    @Nullable
    protected abstract Usage createUsage(T element);

    protected void replaceModel(@Nonnull List<? extends T> data) {
        myUpdater.replaceModel(data);
    }

    protected void paintBusy(boolean paintBusy) {
        myUpdater.paintBusy(paintBusy);
    }

    private boolean setCanceled() {
        boolean canceled = myCanceled;
        myCanceled = true;
        return canceled;
    }

    public boolean isCanceled() {
        return myCanceled;
    }

    /**
     * @deprecated Use {@link #BackgroundUpdaterTaskBase(Project, String, Comparator)} and {@link #updateComponent(T)} instead
     */
    @Deprecated
    public boolean updateComponent(@Nonnull T element, @Nullable Comparator comparator) {
        if (tryAppendUsage(element)) {
            return true;
        }
        if (myCanceled) {
            return false;
        }

        if (myPopup.isDisposed()) {
            return false;
        }
        ModalityState modalityState = Application.get().getModalityStateForComponent(myPopup.getContent());

        synchronized (lock) {
            if (myData.contains(element)) {
                return true;
            }
            myData.add(element);
            if (comparator != null && myData instanceof List list) {
                Collections.sort(list, comparator);
            }
        }

        myAlarm.addRequest(
            () -> {
                myAlarm.cancelAllRequests();
                refreshModelImmediately();
            },
            200,
            modalityState
        );
        return true;
    }

    private boolean tryAppendUsage(@Nonnull T element) {
        UsageView view = myUsageView.get();
        if (view != null && !view.isDisposed()) {
            Usage usage = createUsage(element);
            if (usage == null) {
                return false;
            }
            Application.get().runReadAction(() -> view.appendUsage(usage));
            return true;
        }
        return false;
    }

    public boolean updateComponent(@Nonnull T element) {
        if (tryAppendUsage(element)) {
            return true;
        }

        if (myCanceled) {
            return false;
        }
        if (myPopup.isDisposed()) {
            return false;
        }

        synchronized (lock) {
            if (!myData.add(element)) {
                return true;
            }
        }

        myAlarm.addRequest(
            () -> {
                myAlarm.cancelAllRequests();
                refreshModelImmediately();
            },
            200,
            Application.get().getModalityStateForComponent(myPopup.getContent())
        );
        return true;
    }

    @RequiredUIAccess
    private void refreshModelImmediately() {
        UIAccess.assertIsUIThread();
        if (myCanceled) {
            return;
        }
        if (myPopup.isDisposed()) {
            return;
        }
        List<T> data;
        synchronized (lock) {
            data = new ArrayList<>(myData);
        }
        replaceModel(data);
        myPopup.setCaption(getCaption(getCurrentSize()));
        myPopup.pack(true, true);
    }

    public int getCurrentSize() {
        synchronized (lock) {
            return myData.size();
        }
    }

    @Override
    public void run(@Nonnull ProgressIndicator indicator) {
        paintBusy(true);
        myIndicator = indicator;
    }

    @Override
    @RequiredUIAccess
    public void onSuccess() {
        myFinished = true;
        refreshModelImmediately();
        paintBusy(false);
    }

    @Override
    @RequiredUIAccess
    public void onFinished() {
        myAlarm.cancelAllRequests();
        myFinished = true;
    }

    @Nullable
    protected T getTheOnlyOneElement() {
        synchronized (lock) {
            if (myData.size() == 1) {
                return myData.iterator().next();
            }
        }
        return null;
    }

    public boolean isFinished() {
        return myFinished;
    }

    public boolean cancelTask() {
        ProgressIndicator indicator = myIndicator;
        if (indicator != null) {
            indicator.cancel();
        }
        return setCanceled();
    }
}
