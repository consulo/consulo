// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.progress;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.progress.ProgressDialog;
import consulo.application.impl.internal.progress.ProgressWindow;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.component.ComponentManager;
import consulo.desktop.awt.ui.GlassPaneDialogWrapperPeer;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ui.PopupBorder;
import consulo.ide.impl.idea.ui.TitlePanel;
import consulo.ide.impl.idea.ui.WindowMoveListener;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.DialogWrapperDialog;
import consulo.ui.ex.awt.internal.DialogWrapperPeer;
import consulo.ui.ex.awt.internal.DialogWrapperPeerFactory;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class DesktopAWTProgressDialogImpl implements ProgressDialog {
    private final ProgressWindow myProgressWindow;
    private long myLastTimeDrawn = -1;
    private volatile boolean myShouldShowBackground;
    private final Alarm myUpdateAlarm = new Alarm(this);
    boolean myWasShown;

    final Runnable myRepaintRunnable = new Runnable() {
        @Override
        public void run() {
            String text = myProgressWindow.getText();
            double fraction = myProgressWindow.getFraction();
            String text2 = myProgressWindow.getText2();

            if (myProgressBar.isShowing()) {
                final int perc = (int) (fraction * 100);
                myProgressBar.setIndeterminate(myProgressWindow.isIndeterminate());
                myProgressBar.setValue(perc);
            }

            myTextLabel.setText(fitTextToLabel(text, myTextLabel));
            myText2Label.setText(fitTextToLabel(text2, myText2Label));

            myTitlePanel.setText(myProgressWindow.getTitle() != null && !myProgressWindow.getTitle().isEmpty() ? myProgressWindow.getTitle() : " ");

            myLastTimeDrawn = System.currentTimeMillis();
            synchronized (DesktopAWTProgressDialogImpl.this) {
                myRepaintedFlag = true;
            }
        }
    };

    @Nonnull
    private static String fitTextToLabel(@Nullable String fullText, @Nonnull JLabel label) {
        if (fullText == null || fullText.isEmpty()) {
            return " ";
        }
        while (label.getFontMetrics(label.getFont()).stringWidth(fullText) > label.getWidth()) {
            int sep = fullText.indexOf(File.separatorChar, 4);
            if (sep < 0) {
                return fullText;
            }
            fullText = "..." + fullText.substring(sep);
        }
        return fullText;
    }

    private final Runnable myUpdateRequest = () -> update();
    JPanel myPanel;

    private JLabel myTextLabel;
    private JBLabel myText2Label;

    private JButton myCancelButton;
    private JButton myBackgroundButton;

    private JProgressBar myProgressBar;
    private boolean myRepaintedFlag = true; // guarded by this
    private TitlePanel myTitlePanel;
    private JPanel myInnerPanel;
    DialogWrapper myPopup;
    private final consulo.ui.Window myParentWindow;

    public DesktopAWTProgressDialogImpl(ProgressWindow progressWindow,
                                        boolean shouldShowBackground,
                                        Project project,
                                        LocalizeValue cancelText) {
        myProgressWindow = progressWindow;
        consulo.ui.Window parentWindow = WindowManager.getInstance().suggestParentWindow(project);
        if (parentWindow == null) {
            parentWindow = WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow();
        }
        myParentWindow = parentWindow;

        initDialog(shouldShowBackground, cancelText);
    }

    public DesktopAWTProgressDialogImpl(ProgressWindow progressWindow,
                                        boolean shouldShowBackground,
                                        Component parent,
                                        LocalizeValue cancelText) {
        myProgressWindow = progressWindow;
        myParentWindow = TargetAWT.from(UIUtil.getWindow(parent));
        initDialog(shouldShowBackground, cancelText);
    }

    private void initDialog(boolean shouldShowBackground, LocalizeValue cancelText) {
        myInnerPanel.setPreferredSize(new Dimension(Platform.current().os().isMac() ? 350 : JBUI.scale(450), -1));

        myCancelButton.addActionListener(__ -> doCancelAction());

        myCancelButton.registerKeyboardAction(__ -> {
            if (myCancelButton.isEnabled()) {
                doCancelAction();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        myShouldShowBackground = shouldShowBackground;
        if (cancelText != LocalizeValue.of()) {
            myProgressWindow.setCancelButtonText(cancelText);
        }
        myProgressBar.setMaximum(100);
        createCenterPanel();

        myTitlePanel.setActive(true);
        WindowMoveListener moveListener = new WindowMoveListener(myTitlePanel) {
            @Override
            protected Component getView(Component component) {
                return SwingUtilities.getAncestorOfClass(DialogWrapperDialog.class, component);
            }
        };
        myTitlePanel.addMouseListener(moveListener);
        myTitlePanel.addMouseMotionListener(moveListener);
    }

    @Override
    public void dispose() {
        UIUtil.dispose(myTitlePanel);
        UIUtil.dispose(myBackgroundButton);
        UIUtil.dispose(myCancelButton);
    }

    JPanel getPanel() {
        return myPanel;
    }

    @Override
    public void startBlocking(@Nonnull CompletableFuture<?> stopCondition, @Nonnull Predicate<AWTEvent> isCancellationEvent) {
        IdeEventQueue.getInstance().pumpEventsForHierarchy(myPanel, stopCondition, event -> {
            if (isCancellationEvent.test(event)) {
                cancel();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean isPopupWasShown() {
        return myPopup != null && myPopup.isShowing();
    }

    @Override
    public void changeCancelButtonText(LocalizeValue text) {
        myCancelButton.setText(text.get());
    }

    private void doCancelAction() {
        if (myProgressWindow.myShouldShowCancel) {
            myProgressWindow.cancel();
        }
    }

    @Override
    public void copyPopupStateToWindow() {
        final DialogWrapper popup = myPopup;
        if (popup != null) {
            if (popup.isShowing()) {
                myWasShown = true;
            }
        }
    }

    private void setCancelButtonEnabledInEDT() {
        myCancelButton.setEnabled(true);
    }

    private void setCancelButtonDisabledInEDT() {
        myCancelButton.setEnabled(false);
    }

    @Override
    public void enableCancelButtonIfNeeded(boolean enable) {
        if (!myProgressWindow.myShouldShowCancel || myDisableCancelAlarm.isDisposed()) {
            return;
        }

        myDisableCancelAlarm.cancelAllRequests();
        myDisableCancelAlarm.addRequest(enable ? this::setCancelButtonEnabledInEDT : this::setCancelButtonDisabledInEDT, 500);
    }

    private final Alarm myDisableCancelAlarm = new Alarm(this);

    private void createCenterPanel() {
        // Cancel button (if any)

        if (myProgressWindow.myCancelText != LocalizeValue.of()) {
            myCancelButton.setText(myProgressWindow.myCancelText.get());
        }
        myCancelButton.setVisible(myProgressWindow.myShouldShowCancel);

        myBackgroundButton.setVisible(myShouldShowBackground);
        myBackgroundButton.addActionListener(__ -> {
            if (myShouldShowBackground) {
                myProgressWindow.background();
            }
        });
    }

    static final int UPDATE_INTERVAL = 50; //msec. 20 frames per second.

    @Override
    public synchronized void update() {
        if (myRepaintedFlag) {
            if (System.currentTimeMillis() > myLastTimeDrawn + UPDATE_INTERVAL) {
                myRepaintedFlag = false;
                SwingUtilities.invokeLater(myRepaintRunnable);
            }
            else {
                // later to avoid concurrent dispose/addRequest
                if (!myUpdateAlarm.isDisposed() && myUpdateAlarm.getActiveRequestCount() == 0) {
                    SwingUtilities.invokeLater(() -> {
                        if (!myUpdateAlarm.isDisposed() && myUpdateAlarm.getActiveRequestCount() == 0) {
                            myUpdateAlarm.addRequest(myUpdateRequest, 500, myProgressWindow.getModalityState());
                        }
                    });
                }
            }
        }
    }

    @Override
    public synchronized void background() {
        if (myShouldShowBackground) {
            myBackgroundButton.setEnabled(false);
        }

        hide();
    }

    @Override
    public void hide() {
        ApplicationManager.getApplication().invokeLater(this::hideImmediately, IdeaModalityState.any());
    }

    @Override
    public void hideImmediately() {
        if (myPopup != null) {
            myPopup.close(DialogWrapper.CANCEL_EXIT_CODE);
            myPopup = null;
        }
    }

    @Override
    public void show() {
        myWasShown = true;
        if (myParentWindow == null) {
            return;
        }
        if (myPopup != null) {
            myPopup.close(DialogWrapper.CANCEL_EXIT_CODE);
        }

        Window awtWindow = TargetAWT.to(myParentWindow);

        myPopup = awtWindow.isShowing()
            ? new MyDialogWrapper(awtWindow, myProgressWindow.myShouldShowCancel)
            : new MyDialogWrapper(myProgressWindow.myProject, myProgressWindow.myShouldShowCancel);
        myPopup.setUndecorated(true);

        if (myPopup.getPeer().isRealDialog()) {
            myPopup.getPeer().setAutoRequestFocus(false);
            if (isWriteActionProgress()) {
                myPopup.setModal(false); // display the dialog and continue with EDT execution, don't block it forever
            }
        }
        myPopup.pack();

        SwingUtilities.invokeLater(() -> {
            if (myPopup != null) {
                getFocusManager().requestFocusInProject(myCancelButton, myProgressWindow.myProject).doWhenDone(myRepaintRunnable);
            }
        });

        Disposer.register(myPopup.getDisposable(), () -> myProgressWindow.exitModality());

        myPopup.show();
    }

    public IdeFocusManager getFocusManager() {
        return ProjectIdeFocusManager.getInstance(myProgressWindow.getProject());
    }

    @Override
    public void runRepaintRunnable() {
        myRepaintRunnable.run();
    }

    private boolean isWriteActionProgress() {
        return myProgressWindow instanceof PotemkinProgress;
    }

    private class MyDialogWrapper extends DialogWrapper {
        private final boolean myIsCancellable;

        public MyDialogWrapper(Project project, final boolean cancellable) {
            super(project, false);
            init();
            myIsCancellable = cancellable;
        }

        public MyDialogWrapper(Component parent, final boolean cancellable) {
            super(parent, false);
            init();
            myIsCancellable = cancellable;
        }

        @Override
        public void doCancelAction() {
            if (myIsCancellable) {
                super.doCancelAction();
            }
        }

        @Nonnull
        @Override
        protected DialogWrapperPeer createPeer(@Nonnull final Component parent, final boolean canBeParent) {
            if (useLightPopup()) {
                try {
                    return new GlassPaneDialogWrapperPeer(this, parent, canBeParent);
                }
                catch (GlassPaneDialogWrapperPeer.GlasspanePeerUnavailableException e) {
                    return super.createPeer(parent, canBeParent);
                }
            }
            else {
                return super.createPeer(parent, canBeParent);
            }
        }

        @Nonnull
        @Override
        protected DialogWrapperPeer createPeer(final ComponentManager project, final boolean canBeParent, final boolean applicationModalIfPossible) {
            if (useLightPopup()) {
                try {
                    return new GlassPaneDialogWrapperPeer(this, canBeParent);
                }
                catch (GlassPaneDialogWrapperPeer.GlasspanePeerUnavailableException e) {
                    return DialogWrapperPeerFactory.getInstance().createPeer(this, WindowManager.getInstance().suggestParentWindow(myProgressWindow.myProject), canBeParent, applicationModalIfPossible);
                }
            }
            else {
                return DialogWrapperPeerFactory.getInstance().createPeer(this, WindowManager.getInstance().suggestParentWindow(myProgressWindow.myProject), canBeParent, applicationModalIfPossible);
            }
        }

        private boolean useLightPopup() {
            return System.getProperty("vintage.progress") == null && !isWriteActionProgress();
        }

        @Nonnull
        @Override
        protected DialogWrapperPeer createPeer(final ComponentManager project, final boolean canBeParent) {
            if (System.getProperty("vintage.progress") == null) {
                try {
                    return new GlassPaneDialogWrapperPeer(this, (Project) project, canBeParent);
                }
                catch (GlassPaneDialogWrapperPeer.GlasspanePeerUnavailableException e) {
                    return super.createPeer(project, canBeParent);
                }
            }
            else {
                return super.createPeer(project, canBeParent);
            }
        }

        @Override
        protected void init() {
            super.init();
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.NONE);
            myPanel.setBorder(PopupBorder.Factory.create(true, true));
        }

        @Override
        protected boolean isProgressDialog() {
            return true;
        }

        @Override
        protected JComponent createCenterPanel() {
            return myPanel;
        }

        @Override
        @Nullable
        protected JComponent createSouthPanel() {
            return null;
        }

        @Override
        @Nullable
        protected Border createContentPaneBorder() {
            return null;
        }
    }
}
