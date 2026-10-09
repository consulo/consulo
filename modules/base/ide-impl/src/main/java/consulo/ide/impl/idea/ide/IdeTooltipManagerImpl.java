// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide;

import consulo.application.Application;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.registry.Registry;
import consulo.colorScheme.EditorColorKey;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.hint.HintColorUtil;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.Html;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.hint.TooltipEvent;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awt.internal.IdeTooltip;
import consulo.ui.ex.awt.internal.IdeTooltipManager;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Objects;

@Singleton
@ServiceImpl
public final class IdeTooltipManagerImpl implements Disposable, IdeTooltipManager {
    public static final EditorColorKey TOOLTIP_COLOR_KEY = EditorColorKey.createColorKey("TOOLTIP", null);

    public static final Color GRAPHITE_COLOR = new Color(100, 100, 100, 230);

    private volatile Component myCurrentComponent;
    private volatile Component myQueuedComponent;

    private Balloon myCurrentTipUi;

    private Disposable myLastDisposable;

    private Runnable myHideRunnable;

    private boolean myShowDelay = true;

    private final Alarm myAlarm = new Alarm();

    private int myX;
    private int myY;

    private IdeTooltip myCurrentTooltip;
    private Runnable myShowRequest;
    private IdeTooltip myQueuedTooltip;

    @Inject
    public IdeTooltipManagerImpl(Application application) {
        application.getMessageBus().connect(application).subscribe(AnActionListener.class, new AnActionListener() {
            @Override
            public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                hideCurrent(null, action, event);
            }
        });
    }

    public IdeTooltip show(IdeTooltip tooltip, boolean now) {
        return show(tooltip, now, true);
    }

    public IdeTooltip show(IdeTooltip tooltip, boolean now, boolean animationEnabled) {
        myAlarm.cancelAllRequests();

        hideCurrent(null, tooltip, null, null);

        myQueuedComponent = tooltip.getComponent();
        myQueuedTooltip = tooltip;

        myShowRequest = () -> {
            if (myShowRequest == null) {
                return;
            }

            if (myQueuedComponent != tooltip.getComponent() || !tooltip.getComponent().isShowing()) {
                hideCurrent(null, tooltip, null, null, animationEnabled);
                return;
            }

            if (tooltip.beforeShow()) {
                show(tooltip, null, animationEnabled);
            }
            else {
                hideCurrent(null, tooltip, null, null, animationEnabled);
            }
        };

        if (now) {
            myShowRequest.run();
        }
        else {
            myAlarm.addRequest(myShowRequest, myShowDelay ? tooltip.getShowDelay() : tooltip.getInitialReshowDelay());
        }

        return tooltip;
    }

    private void show(IdeTooltip tooltip, @Nullable Runnable beforeShow, boolean animationEnabled) {
        boolean toCenterX;
        boolean toCenterY;

        boolean toCenter = tooltip.isToCenter();
        boolean small = false;
        if (!toCenter && tooltip.isToCenterIfSmall()) {
            Dimension size = tooltip.getComponent().getSize();
            toCenterX = size.width < 64;
            toCenterY = size.height < 64;
            toCenter = toCenterX || toCenterY;
            small = true;
        }
        else {
            toCenterX = true;
            toCenterY = true;
        }

        Point effectivePoint = tooltip.getPoint();
        if (toCenter) {
            Rectangle bounds = tooltip.getComponent().getBounds();
            effectivePoint.x = toCenterX ? bounds.width / 2 : effectivePoint.x;
            effectivePoint.y = toCenterY ? bounds.height / 2 : effectivePoint.y;
        }

        if (myCurrentComponent == tooltip.getComponent() && myCurrentTipUi != null && !myCurrentTipUi.isDisposed()) {
            myCurrentTipUi.show(new RelativePoint(tooltip.getComponent(), effectivePoint), tooltip.getPreferredPosition());
            return;
        }

        if (myCurrentComponent == tooltip.getComponent() && effectivePoint.equals(new Point(myX, myY))) {
            return;
        }

        Color bg = tooltip.getTextBackground() != null ? tooltip.getTextBackground() : getTextBackground(true);
        Color fg = tooltip.getTextForeground() != null ? tooltip.getTextForeground() : getTextForeground(true);
        Color borderColor = tooltip.getBorderColor() != null ? tooltip.getBorderColor() : getBorderColor(true);

        BalloonBuilder builder = JBPopupFactory.getInstance().createBalloonBuilder(tooltip.getTipComponent())
            .setFillColor(bg)
            .setBorderColor(borderColor).setBorderInsets(tooltip.getBorderInsets())
            .setAnimationCycle(animationEnabled ? Registry.intValue("ide.tooltip.animationCycle") : 0).setShowCallout(true)
            .setCalloutShift(small && tooltip.getCalloutShift() == 0 ? 2 : tooltip.getCalloutShift())
            .setPositionChangeXShift(tooltip.getPositionChangeX())
            .setPositionChangeYShift(tooltip.getPositionChangeY())
            .setHideOnKeyOutside(!tooltip.isExplicitClose())
            .setHideOnAction(!tooltip.isExplicitClose())
            .setRequestFocus(tooltip.isRequestFocus())
            .setPointerSize(tooltip.getPointerSize())
            .setLayer(tooltip.getLayer());

        if (tooltip.isPointerShiftedToStart()) {
            builder.setPointerShiftedToStart(true);
        }

        tooltip.getTipComponent().setForeground(fg);
        tooltip.getTipComponent().setBorder(tooltip.getComponentBorder());
        tooltip.getTipComponent().setFont(tooltip.getFont() != null ? tooltip.getFont() : getTextFont(true));

        if (beforeShow != null) {
            beforeShow.run();
        }

        myCurrentTipUi = builder.createBalloon();
        myCurrentTipUi.setAnimationEnabled(animationEnabled);
        tooltip.setUi((IdeTooltip.Ui) myCurrentTipUi);
        myCurrentComponent = tooltip.getComponent();
        myX = effectivePoint.x;
        myY = effectivePoint.y;
        myCurrentTooltip = tooltip;
        myShowRequest = null;
        myQueuedComponent = null;
        myQueuedTooltip = null;

        myLastDisposable = myCurrentTipUi;
        Disposer.register(myLastDisposable, () -> myLastDisposable = null);

        myCurrentTipUi.show(new RelativePoint(tooltip.getComponent(), effectivePoint), tooltip.getPreferredPosition());
        myAlarm.addRequest(
            () -> {
                if (myCurrentTooltip == tooltip && tooltip.canBeDismissedOnTimeout()) {
                    hideCurrent(null, null, null);
                }
            },
            tooltip.getDismissDelay()
        );
    }

    @Override
    @SuppressWarnings({"UnusedParameters"})
    public Color getTextForeground(boolean awtTooltip) {
        return UIUtil.getToolTipForeground();
    }

    @Override
    @SuppressWarnings({"UnusedParameters"})
    public Color getLinkForeground(boolean awtTooltip) {
        return TargetAWT.to(ComponentColors.LINK_FOREGROUND);
    }

    @Override
    @SuppressWarnings({"UnusedParameters"})
    public Color getTextBackground(boolean awtTooltip) {
        ColorValue color = EditorColorsUtil.getGlobalOrDefaultColor(TOOLTIP_COLOR_KEY);
        return color != null ? TargetAWT.to(color) : UIUtil.getToolTipBackground();
    }

    @Override
    @SuppressWarnings({"UnusedParameters"})
    public String getUlImg(boolean awtTooltip) {
        return StyleManager.get().getCurrentStyle().isDark() ? "/general/mdot-white.png" : "/general/mdot.png";
    }

    @Override
    @SuppressWarnings({"UnusedParameters"})
    @Deprecated(forRemoval = true)
    public Color getBorderColor(boolean awtTooltip) {
        return JBColor.border();
    }

    @Override
    @SuppressWarnings({"UnusedParameters"})
    public boolean isOwnBorderAllowed(boolean awtTooltip) {
        return !awtTooltip;
    }

    @Override
    @SuppressWarnings({"UnusedParameters"})
    public boolean isOpaqueAllowed(boolean awtTooltip) {
        return !awtTooltip;
    }

    @Override
    @SuppressWarnings({"UnusedParameters"})
    public Font getTextFont(boolean awtTooltip) {
        return UIManager.getFont("ToolTip.font");
    }

    public boolean hasCurrent() {
        return myCurrentTooltip != null;
    }

    public boolean hasScheduled() {
        return myShowRequest != null;
    }

    @Override
    public boolean hideCurrent(@Nullable MouseEvent me) {
        return hideCurrent(me, null, null, null);
    }

    private boolean hideCurrent(@Nullable MouseEvent me, @Nullable AnAction action, @Nullable AnActionEvent event) {
        return hideCurrent(me, null, action, event, myCurrentTipUi != null && myCurrentTipUi.isAnimationEnabled());
    }

    private boolean hideCurrent(
        @Nullable MouseEvent me,
        @Nullable IdeTooltip tooltipToShow,
        @Nullable AnAction action,
        @Nullable AnActionEvent event
    ) {
        return hideCurrent(me, tooltipToShow, action, event, myCurrentTipUi != null && myCurrentTipUi.isAnimationEnabled());
    }

    private boolean hideCurrent(
        @Nullable MouseEvent me,
        @Nullable IdeTooltip tooltipToShow,
        @Nullable AnAction action,
        @Nullable AnActionEvent event,
        boolean animationEnabled
    ) {
        if (myCurrentTooltip != null && me != null && myCurrentTooltip.isInside(new RelativePoint(me))) {
            if (me.getButton() == MouseEvent.NOBUTTON || myCurrentTipUi == null || myCurrentTipUi.isBlockClicks()) {
                return false;
            }
        }

        myShowRequest = null;
        myQueuedComponent = null;
        myQueuedTooltip = null;

        if (myCurrentTooltip == null) {
            return true;
        }

        if (myCurrentTipUi != null) {
            RelativePoint target = me != null ? new RelativePoint(me) : null;
            boolean isInsideOrMovingForward = target != null
                && (((IdeTooltip.Ui) myCurrentTipUi).isInside(target) || myCurrentTipUi.isMovingForward(target));
            boolean canAutoHide = myCurrentTooltip.canAutohideOn(new TooltipEvent(me, isInsideOrMovingForward, action, event));
            boolean implicitMouseMove = me != null
                && (me.getID() == MouseEvent.MOUSE_MOVED || me.getID() == MouseEvent.MOUSE_EXITED || me.getID() == MouseEvent.MOUSE_ENTERED);
            if (!canAutoHide
                || (isInsideOrMovingForward && implicitMouseMove)
                || (myCurrentTooltip.isExplicitClose() && implicitMouseMove)
                || (tooltipToShow != null && !tooltipToShow.isHint() && Objects.equals(myCurrentTooltip, tooltipToShow))) {
                if (myHideRunnable != null) {
                    myHideRunnable = null;
                }
                return false;
            }
        }

        myHideRunnable = () -> {
            if (myHideRunnable != null) {
                hideCurrentNow(animationEnabled);
                myHideRunnable = null;
            }
        };

        if (me != null && me.getButton() == MouseEvent.NOBUTTON) {
            myAlarm.addRequest(myHideRunnable, Registry.intValue("ide.tooltip.autoDismissDeadZone"));
        }
        else {
            myHideRunnable.run();
            myHideRunnable = null;
        }

        return true;
    }

    public void hideCurrentNow(boolean animationEnabled) {
        if (myCurrentTipUi != null) {
            myCurrentTipUi.setAnimationEnabled(animationEnabled);
            myCurrentTipUi.hide();
            myCurrentTooltip.onHidden();
            myShowDelay = false;
            myAlarm.addRequest(() -> myShowDelay = true, Registry.intValue("ide.tooltip.reshowDelay"));
        }

        myShowRequest = null;
        myCurrentTooltip = null;
        myCurrentTipUi = null;
        myCurrentComponent = null;
        myQueuedComponent = null;
        myQueuedTooltip = null;
        myX = -1;
        myY = -1;
    }

    @Override
    public void dispose() {
        hideCurrentNow(false);
        if (myLastDisposable != null) {
            Disposer.dispose(myLastDisposable);
        }
    }

    public static IdeTooltipManagerImpl getInstanceImpl() {
        return (IdeTooltipManagerImpl) IdeTooltipManager.getInstance();
    }

    @Override
    public void hide(@Nullable IdeTooltip tooltip) {
        if (myCurrentTooltip == tooltip || tooltip == null || tooltip == myQueuedTooltip) {
            hideCurrent(null, null, null);
        }
    }

    public void cancelAutoHide() {
        myHideRunnable = null;
    }

    @Override
    public JEditorPane initEditorPane(String text, HintHint hintHint, @Nullable JLayeredPane layeredPane) {
        return initPane(text, hintHint, layeredPane);
    }

    public static JEditorPane initPane(String text, HintHint hintHint, @Nullable JLayeredPane layeredPane) {
        return initPane(new Html(text), hintHint, layeredPane);
    }

    public static JEditorPane initPane(Html html, HintHint hintHint, @Nullable JLayeredPane layeredPane) {
        return initPane(html, hintHint, layeredPane, true);
    }

    public static JEditorPane initPane(
        Html html,
        final HintHint hintHint,
        final @Nullable JLayeredPane layeredPane,
        boolean limitWidthToScreen
    ) {
        String text = HintUtil.prepareHintText(html, hintHint);

        final boolean[] prefSizeWasComputed = {false};
        JEditorPane pane = limitWidthToScreen ? new JEditorPane() {
            private Dimension prefSize = null;

            @Override
            public Dimension getPreferredSize() {
                if (!prefSizeWasComputed[0] && hintHint.isAwtTooltip()) {
                    JLayeredPane lp = layeredPane;
                    if (lp == null) {
                        JRootPane rootPane = UIUtil.getRootPane(this);
                        if (rootPane != null && rootPane.getSize().width > 0) {
                            lp = rootPane.getLayeredPane();
                        }
                    }

                    Dimension size;
                    if (lp != null) {
                        GuiUtils.targetToDevice(this, lp);
                        size = lp.getSize();
                        prefSizeWasComputed[0] = true;
                    }
                    else {
                        size = ScreenUtil.getScreenRectangle(0, 0).getSize();
                    }
                    int fitWidth = (int) (size.width * 0.8);
                    Dimension prefSizeOriginal = super.getPreferredSize();
                    if (prefSizeOriginal.width > fitWidth) {
                        setSize(new Dimension(fitWidth, Integer.MAX_VALUE));
                        Dimension fixedWidthSize = super.getPreferredSize();
                        Dimension minSize = super.getMinimumSize();
                        prefSize = new Dimension(Math.max(fitWidth, minSize.width), fixedWidthSize.height);
                    }
                    else {
                        prefSize = new Dimension(prefSizeOriginal);
                    }
                }

                Dimension s = prefSize != null ? new Dimension(prefSize) : super.getPreferredSize();
                Border b = getBorder();
                if (b != null) {
                    JBInsets.addTo(s, b.getBorderInsets(this));
                }
                return s;
            }

            @Override
            public void setPreferredSize(Dimension preferredSize) {
                super.setPreferredSize(preferredSize);
                prefSize = preferredSize;
            }
        } : new JEditorPane();

        HTMLEditorKit kit = new JBHtmlEditorKit();
        pane.setEditorKit(kit);
        pane.setText(text);
        pane.setCaretPosition(0);
        pane.setEditable(false);

        if (hintHint.isOwnBorderAllowed()) {
            setBorder(pane);
            setColors(pane);
        }
        else {
            pane.setBorder(JBUI.Borders.emptyRight(4));
        }

        if (!hintHint.isAwtTooltip()) {
            prefSizeWasComputed[0] = true;
        }

        boolean opaque = hintHint.isOpaqueAllowed();
        pane.setOpaque(opaque);
        pane.setBackground(hintHint.getTextBackground());

        if (!limitWidthToScreen) {
            GuiUtils.targetToDevice(pane, layeredPane);
        }

        return pane;
    }

    public static void setColors(JComponent pane) {
        pane.setForeground(JBColor.foreground());
        pane.setBackground(TargetAWT.to(HintColorUtil.getInformationColor()));
        pane.setOpaque(true);
    }

    public static void setBorder(JComponent pane) {
        pane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black), JBUI.Borders.empty(0, 5)));
    }

    public boolean isQueuedToShow(IdeTooltip tooltip) {
        return Objects.equals(myQueuedTooltip, tooltip);
    }
}
