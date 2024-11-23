// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.progress.TaskInfo;
import consulo.disposer.Disposable;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.style.StyleManager;
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

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            cancelRequest();
        }

        @RequiredUIAccess
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
        myToolbar.updateActionsImmediately();
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

            myProcessName.setText(processInfo.getTitle());
            myComponent.add(myProcessName, BorderLayout.NORTH);
            myProcessName.setForeground(UIUtil.getPanelBackground().brighter().brighter());
            myProcessName.setBorder(JBUI.Borders.empty(2));

            final NonOpaquePanel content = new NonOpaquePanel(new BorderLayout());
            content.setBorder(JBUI.Borders.empty(2, 2, 2, myInfo.isCancellable() ? 2 : 4));
            myComponent.add(content, BorderLayout.CENTER);

            content.add(toolbar, BorderLayout.EAST);
            content.add(myText, BorderLayout.NORTH);

            // remove expanding
            NonOpaquePanel progressWrapper = new NonOpaquePanel(new MigLayout(new LC()
                .fillX()
                .alignY("center")
                .insets("")
                .noVisualPadding(),
                new AC().gap("0")));
            progressWrapper.add(myProgress, new CC().growX().alignY("center"));

            content.add(progressWrapper, BorderLayout.CENTER);
            content.add(myText2, BorderLayout.SOUTH);

            myComponent.setBorder(JBUI.Borders.empty(2));
        }
        UIUtil.uiTraverser(myComponent).forEach(o -> ((JComponent) o).setOpaque(false));
    }

    public List<AnAction> createEastButtons() {
        return List.of(new CancelAction(myInfo.getCancelTextValue(), myInfo.getCancelTooltipTextValue()));
    }

    protected void cancelRequest() {
        cancel();
    }

    public void updateProgress() {
        queueProgressUpdate();
    }

    public void updateAndRepaint() {
        if (isDisposed()) {
            return;
        }

        updateProgressNow();

        myComponent.repaint();
    }

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
                myText.setText("Stopping - " + myText.getText());
            }
            else {
                myProcessName.setText("Stopping - " + myInfo.getTitle());
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

        myToolbar.updateActionsImmediately();
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

    protected void queueProgressUpdate() {
        updateAndRepaint();
    }

    protected void queueRunningUpdate(@Nonnull Runnable update) {
        update.run();
    }

    @Override
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
        private final JComponent myProcessName;

        private MyComponent(final JComponent processName) {
            myProcessName = processName;
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (UIUtil.isCloseClick(e) && getBounds().contains(e.getX(), e.getY())) {
                        cancelRequest();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final GraphicsConfig c = GraphicsUtil.setupAAPainting(g);
            UISettingsUtil.setupAntialiasing(g);

            int arc = 8;
            Color bg = getBackground();
            final Rectangle bounds = myProcessName.getBounds();
            final Rectangle label = SwingUtilities.convertRectangle(myProcessName.getParent(), bounds, this);

            g.setColor(UIUtil.getPanelBackground());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            if (!StyleManager.get().getCurrentStyle().isDark()) {
                bg = ColorUtil.toAlpha(bg.darker().darker(), 230);
                g.setColor(bg);

                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                g.setColor(UIUtil.getPanelBackground());
                g.fillRoundRect(0, getHeight() / 2, getWidth() - 1, getHeight() / 2, arc, arc);
                g.fillRect(0, (int) label.getMaxY() + 1, getWidth() - 1, getHeight() / 2);
            }
            else {
                bg = bg.brighter();
                g.setColor(bg);
                g.drawLine(0, (int) label.getMaxY() + 1, getWidth() - 1, (int) label.getMaxY() + 1);
            }

            g.setColor(bg);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            c.restore();
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

