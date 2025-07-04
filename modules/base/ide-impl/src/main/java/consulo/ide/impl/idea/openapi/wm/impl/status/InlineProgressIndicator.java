// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.progress.TaskInfo;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ui.CaptionPanel;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class InlineProgressIndicator extends ProgressIndicatorBase implements Disposable {
    private final ActionToolbar myToolbar;

    private class CancelAction extends DumbAwareAction {
        public CancelAction(LocalizeValue text, LocalizeValue description) {
            super(text, description, PlatformIconGroup.actionsCancel());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            cancelRequest();
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(myInfo != null && myInfo.isCancellable());
        }
    }

    protected final JLabel myText = new JLabel();
    private final JLabel myText2 = new JLabel();

    protected JProgressBar myProgress;

    private JPanel myComponent;

    private final boolean myCompact;
    private TaskInfo myInfo;

    private final JLabel myProcessName = new JLabel();
    private boolean myDisposed;

    @RequiredUIAccess
    public InlineProgressIndicator(boolean compact, @Nonnull TaskInfo processInfo) {
        myCompact = compact;
        myInfo = processInfo;

        myProgress = new JProgressBar(SwingConstants.HORIZONTAL);
        myProgress.setStringPainted(false);

        List<AnAction> actions = createEastButtons();

        ActionGroup group = ActionGroup.newImmutableBuilder().addAll(actions).build();
        myToolbar = ActionManager.getInstance().createActionToolbar("InlineProgressBar", group, true);
        myToolbar.setTargetComponent(myComponent);
        myToolbar.updateActionsAsync();
        myToolbar.setMiniMode(true);

        JComponent toolbar = myToolbar.getComponent();

        if (myCompact) {
            myComponent = new JPanel(new HorizontalLayout(JBUI.scale(5), SwingConstants.CENTER));

            myComponent.add(myText);
            myComponent.add(myProgress);
            myComponent.add(toolbar);

            myComponent.setToolTipText(processInfo.getTitle() + ". " + IdeLocalize.progressTextClicktoviewprogresswindow());
        }
        else {
            myComponent = new MyComponent(myProcessName);
            myComponent.setLayout(new BorderLayout());
            myComponent.setBorder(new RoundedLineBorder(JBColor.border(), UIManager.getInt("Component.arc")));

            CaptionPanel captionPanel = new CaptionPanel();
            captionPanel.setActive(true);
            captionPanel.setBorder(JBUI.Borders.empty(2));

            captionPanel.add(myProcessName, BorderLayout.CENTER);
            myProcessName.setText(processInfo.getTitle());
            myComponent.add(captionPanel, BorderLayout.NORTH);

            NonOpaquePanel content = new NonOpaquePanel(new BorderLayout());
            content.setBorder(JBUI.Borders.empty(2));

            myComponent.add(content, BorderLayout.CENTER);

            content.add(toolbar, BorderLayout.EAST);
            content.add(myText, BorderLayout.NORTH);

            // remove expanding
            NonOpaquePanel progressWrapper = new NonOpaquePanel(new MigLayout(
                new LC()
                    .fillX()
                    .alignY("center")
                    .insets("")
                    .noVisualPadding(),
                new AC().gap("0")
            ));
            progressWrapper.add(myProgress, new CC().growX().alignY("center"));

            content.add(progressWrapper, BorderLayout.CENTER);
            content.add(myText2, BorderLayout.SOUTH);
        }
        UIUtil.uiTraverser(myComponent).forEach(o -> ((JComponent) o).setOpaque(false));
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

        myComponent.repaint();
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

        myText.setText(getText() != null ? getText() : "");
        myText2.setText(getText2() != null ? getText2() : "");

        if (myCompact && StringUtil.isEmpty(myText.getText())) {
            myText.setText(myInfo.getTitle());
        }

        if (isStopping()) {
            if (myCompact) {
                myText.setText(IdeLocalize.progressTextStopping(myText.getText()).get());
            }
            else {
                myProcessName.setText(IdeLocalize.progressTextStopping(myInfo.getTitle()).get());
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

    protected void queueRunningUpdate(@Nonnull Runnable update) {
        update.run();
    }

    @Override
    @RequiredUIAccess
    protected void onProgressChange() {
        updateProgress();
    }

    public JComponent getComponent() {
        return myComponent;
    }

    public boolean isCompact() {
        return myCompact;
    }

    public TaskInfo getInfo() {
        return myInfo;
    }

    private class MyComponent extends JPanel {
        private MyComponent(JComponent processName) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (UIUtil.isCloseClick(e) && getBounds().contains(e.getX(), e.getY())) {
                        cancelRequest();
                    }
                }
            });
        }
    }

    @Override
    public void dispose() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        myComponent.removeAll();

        myComponent = null;
        myProgress = null;
        myInfo = null;
    }

    private boolean isDisposed() {
        return myDisposed;
    }
}

