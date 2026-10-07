// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.popup.async;

import consulo.application.Application;
import consulo.desktop.awt.ui.popup.NextStepHandler;
import consulo.desktop.awt.ui.popup.WizardPopup;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.AsyncPopupStep;
import consulo.ui.ex.popup.PopupStep;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AsyncPopupImpl extends WizardPopup implements Runnable {
    private final Future<PopupStep> myFuture;
    private final Object myParentValue;
    private final NextStepHandler myCallBackParent;
    private final Alarm myAlarm;
    private JPanel myPanel;

    public AsyncPopupImpl(
        @Nullable Project project,
        @Nullable WizardPopup parent,
        AsyncPopupStep<Object> step,
        @Nullable Object parentValue
    ) {
        super(project, parent, step);

        if (!(parent instanceof NextStepHandler)) {
            throw new IllegalArgumentException("parent must be NextStepHandler");
        }

        myCallBackParent = (NextStepHandler) parent;
        myParentValue = parentValue;

        myFuture = Application.get().executeOnPooledThread(step);

        myAlarm = new Alarm(this);
        myAlarm.addRequest(this, 200);
        Disposer.register(
            this,
            () -> {
                if (!myFuture.isCancelled() && !myFuture.isDone()) {
                    myFuture.cancel(false);
                }
            }
        );
    }

    @Override
    public void run() {
        if (myFuture.isCancelled()) {
            return;
        }

        if (myFuture.isDone()) {
            goBack();
            try {
                myCallBackParent.handleNextStep(myFuture.get(), myParentValue);
            }
            catch (InterruptedException | ExecutionException ignored) {
            }
            return;
        }
        myAlarm.addRequest(this, 200);
    }

    @Override
    protected JComponent createContent() {
        if (myPanel != null) {
            return myPanel;
        }
        myPanel = new JPanel(new BorderLayout());
        //myPanel.add(new AsyncProcessIcon("Async Popup Step"), BorderLayout.WEST);
        JBLabel label = new JBLabel("Loading…");
        label.setForeground(UIUtil.getLabelDisabledForeground());
        myPanel.add(label, BorderLayout.CENTER);
        myPanel.setBorder(new EmptyBorder(UIUtil.getListCellPadding()));
        myPanel.setBackground(UIUtil.getListBackground());
        myPanel.registerKeyboardAction(e -> goBack(), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), JComponent.WHEN_FOCUSED);
        return myPanel;
    }

    @Override
    protected JComponent getPreferredFocusableComponent() {
        return createContent();
    }

    @Override
    protected InputMap getInputMap() {
        return null;
    }

    @Override
    protected ActionMap getActionMap() {
        return null;
    }

    @Override
    public void onChildSelectedFor(Object value) {
    }
}
