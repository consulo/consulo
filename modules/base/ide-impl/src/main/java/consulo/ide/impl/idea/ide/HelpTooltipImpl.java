// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide;

import consulo.application.Application;
import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.internal.HelpTooltip;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.BitUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Standard implementation of help context tooltip.
 *
 * <h2>Overview</h2>
 * <p>UI design requires to have tooltips that contain detailed information about UI actions and controls.
 * Embedded context help tooltip functionality is incorporated this class.</p>
 *
 * <p>A simple example of the tooltip usage is the following:<br/>
 * <code>new HelpTooltip().<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;setTitle("Title").<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;setShortcut("Shortcut").<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;setDescription("Description").<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;installOn(component);</code>
 * </p>
 *
 * <h2>Restrictions and field formats</h2>
 * <p>If you're creating a tooltip with a shortcut then title is mandatory otherwise title, description, link are optional.
 * You can optionally set the tooltip relative location using {@link HelpTooltipImpl#setLocation(Alignment)}.
 * The {@code Alignment} enum defines fixed relative locations according to the design document (see the link below).
 * More types of relative location will be added as needed but there won't be a way to choose the location on pixel basis.</p>
 *
 * <p>No HTML tagging is allowed in title or shortcut, they are supposed to be simple text strings.</p>
 * <p>Description is can be html formatted. You can use all possible html tagging in description just without enclosing
 * &lt;html&gt; and &lt;/html&gt; tags themselves. In description it's allowed to have &lt;p/&gt; or &lt;p&gt; tags between paragraphs.
 * Paragraphs will be rendered with the standard (10px) offset from the title, from one another and from the link.
 * To force the line break in a paragraph use &lt;br/&gt;. Standard font coloring and styling is also available.</p>
 *
 * <h2>Timeouts</h2>
 *
 * <p>Single line tooltips auto-close in 10 seconds, multiline in 30 seconds. You can optionally disable auto-closing by setting
 * {@link HelpTooltipImpl#setNeverHideOnTimeout(boolean)} to {@code true}. By default tooltips don't close after a timeout on help buttons
 * (those having a round icon with question mark). Before setting this option to true you should contact designers first.</p>
 *
 * <p>System wide tooltip timeouts are set through the registry:
 * <ul>
 * <li>&nbsp;ide.helptooltip.full.dismissDelay - multiline tooltip timeout (default 30 seconds)</li>
 * <li>&nbsp;ide.helptooltip.regular.dismissDelay - single line tooltip timeout (default 10 seconds)</li>
 * </ul></p>
 *
 * <h2>Avoiding multiple popups</h2>
 * <p>Some actions may open a popup menu. Current design is that the action's popup menu should take over the help tooltip.
 * This is partly implemented in {@code AbstractPopup} class to track such cases. But this doesn't always work.
 * If help tooltip shows up over the component's popup menu you should make sure you set the master popup for the help tooltip.
 * This will prevent help tooltip from showing when the popup menu is opened. The best way to do it is to take source component
 * from an {@code InputEvent} and pass the source component along with the popup menu reference to
 * {@link HelpTooltipImpl#setMasterPopup(Component, JBPopup)} static method.
 *
 * <p>If you're handling {@code DumbAware.actionPerformed(AnActionEvent e)}, it has {@code InputEvent} in {@code AnActionEvent}
 * which you can use to get the source.</p>
 *
 * <h2>ContextHelpLabel</h2>
 * <p>ContextHelpLabel is a convenient {@code JLabel} which contains a help icon and has a HelpTooltip installed on it.
 * You can create it using one of its static methods and pass title/description/link. This label can also be used in forms.
 * The UI designer will offer to create {@code private void createUIComponents()} method where you can create the label
 * with a static method.</p>
 */
public class HelpTooltipImpl implements HelpTooltip {
    private static final Color BACKGROUND_COLOR = JBColor.namedColor("ToolTip.background", new JBColor(0xf7f7f7, 0x474a4c));
    public static final Color SHORTCUT_COLOR = JBColor.namedColor("ToolTip.shortcutForeground", new JBColor(0x787878, 0x999999));
    private static final Color INFO_COLOR = JBColor.namedColor("ToolTip.infoForeground", UIUtil.getContextHelpForeground());
    private static final Color BORDER_COLOR = JBColor.namedColor("ToolTip.borderColor", new JBColor(0xadadad, 0x636569));

    private static final JBValue VGAP = new JBValue.UIInteger("HelpTooltip.verticalGap", 4);
    private static final JBValue MAX_WIDTH = new JBValue.UIInteger("HelpTooltip.maxWidth", 250);
    private static final JBValue X_OFFSET = new JBValue.UIInteger("HelpTooltip.xOffset", 0);
    private static final JBValue Y_OFFSET = new JBValue.UIInteger("HelpTooltip.yOffset", 0);
    private static final JBValue HEADER_FONT_SIZE_DELTA = new JBValue.UIInteger("HelpTooltip.fontSizeDelta", 0);
    private static final JBValue DESCRIPTION_FONT_SIZE_DELTA = new JBValue.UIInteger("HelpTooltip.descriptionSizeDelta", 0);
    private static final JBValue CURSOR_OFFSET = new JBValue.UIInteger("HelpTooltip.mouseCursorOffset", 20);

    private static final String PARAGRAPH_SPLITTER = "<p/?>";
    private static final String TOOLTIP_PROPERTY = "JComponent.helpTooltip";

    private int myInitialDelay = 500;
    private int myHideDelay = 150;
    private boolean myInitialShowScheduled;

    private LocalizeValue myTitle = LocalizeValue.empty();
    private @Nullable String myShortcut;
    private LocalizeValue myDescription = LocalizeValue.empty();
    private @Nullable LinkLabel<?> myLink;
    private boolean myNeverHide;
    private Alignment myAlignment = Alignment.CURSOR;

    private BooleanSupplier myMasterPopupOpenCondition;

    protected ComponentPopupBuilder myPopupBuilder;
    private Dimension myPopupSize;
    private JBPopup myPopup;
    private Future<?> myScheduleFuture = CompletableFuture.completedFuture(null);
    private boolean myIsOverPopup;
    private boolean myIsMultiline;
    private String myToolTipText;

    protected MouseAdapter myMouseListener;

    private HierarchyListener myHierarchyListener;

    /**
     * Location of the HelpTooltip relatively to the owner component.
     */
    public enum Alignment {
        RIGHT {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                Dimension size = owner.getSize();
                return new Point(size.width + JBUIScale.scale(5) - X_OFFSET.get(), JBUIScale.scale(1) + Y_OFFSET.get());
            }
        },

        BOTTOM {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                Dimension size = owner.getSize();
                return new Point(JBUIScale.scale(1) + X_OFFSET.get(), JBUIScale.scale(5) + size.height - Y_OFFSET.get());
            }
        },

        HELP_BUTTON {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                Insets i = ((JComponent) owner).getInsets();
                return new Point(X_OFFSET.get() - JBUIScale.scale(40), i.top + Y_OFFSET.get() - JBUIScale.scale(6) - popupSize.height);
            }
        },

        CURSOR {
            @Override
            public Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation) {
                Point location = mouseLocation.getLocation();
                location.y += CURSOR_OFFSET.get();

                SwingUtilities.convertPointToScreen(location, owner);
                Rectangle r = new Rectangle(location, popupSize);
                ScreenUtil.fitToScreen(r);
                location = r.getLocation();
                SwingUtilities.convertPointFromScreen(location, owner);
                r.setLocation(location);

                if (r.contains(mouseLocation)) {
                    location.y = -popupSize.height - JBUI.scale(1);
                }

                return location;
            }
        };

        public abstract Point getPointFor(Component owner, Dimension popupSize, Point mouseLocation);
    }

    /**
     * Sets tooltip title. If it's longer than 2 lines (fitting in 250 pixels each) then
     * the text is automatically stripped to the word boundary and dots are added to the end.
     *
     * @param title text for title.
     * @return {@code this}
     */
    @Override
    public HelpTooltipImpl setTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    /**
     * Sets text for the shortcut placeholder.
     *
     * @param shortcut text for shortcut.
     * @return {@code this}
     */
    @Override
    public HelpTooltipImpl setShortcut(@Nullable String shortcut) {
        myShortcut = shortcut;
        return this;
    }

    @Override
    public HelpTooltipImpl setShortcut(@Nullable Shortcut shortcut) {
        myShortcut = shortcut == null ? null : KeymapUtil.getShortcutText(shortcut);
        return this;
    }

    /**
     * Sets description text.
     *
     * @param description text for description.
     * @return {@code this}
     */
    @Override
    public HelpTooltipImpl setDescription(LocalizeValue description) {
        myDescription = description;
        return this;
    }

    /**
     * Enables link in the tooltip below description and sets action for it.
     *
     * @param linkText   text to show in the link.
     * @param linkAction action to execute when link is clicked.
     * @return {@code this}
     */
    @Override
    public HelpTooltipImpl setLink(String linkText, Runnable linkAction) {
        myLink = LinkLabel.create(
            linkText,
            () -> {
                hidePopup(true);
                linkAction.run();
            }
        );
        return this;
    }

    /**
     * Toggles whether to hide tooltip automatically on timeout. For default behaviour just don't call this method.
     *
     * @param neverHide {@code true} don't hide, {@code false} otherwise.
     * @return {@code this}
     */
    public HelpTooltipImpl setNeverHideOnTimeout(boolean neverHide) {
        myNeverHide = neverHide;
        return this;
    }

    /**
     * Sets location of the tooltip relatively to the owner component.
     *
     * @param alignment is relative location
     * @return {@code this}
     */
    public HelpTooltipImpl setLocation(Alignment alignment) {
        myAlignment = alignment;
        return this;
    }

    /**
     * Installs the tooltip after the configuration has been completed on the specified owner component.
     *
     * @param component is the owner component for the tooltip.
     */
    public void installOn(JComponent component) {
        myNeverHide = myNeverHide || UIUtil.isHelpButton(component);

        createMouseListeners();
        initPopupBuilder();

        component.putClientProperty(TOOLTIP_PROPERTY, this);
        installMouseListeners(component);
    }

    protected final void createMouseListeners() {
        myMouseListener = new MouseAdapter() {
            @Override
            @RequiredUIAccess
            public void mouseEntered(MouseEvent e) {
                if (myPopup != null && !myPopup.isDisposed()) {
                    myPopup.cancel();
                }
                myInitialShowScheduled = true;
                scheduleShow(e, myInitialDelay);
            }

            @Override
            @RequiredUIAccess
            public void mouseExited(MouseEvent e) {
                scheduleHide(myLink == null, myHideDelay);
            }

            @Override
            @RequiredUIAccess
            public void mouseMoved(MouseEvent e) {
                if (!myInitialShowScheduled) {
                    scheduleShow(e, Registry.intValue("ide.tooltip.reshowDelay"));
                }
            }
        };

        myHierarchyListener = e -> {
            if (BitUtil.isSet(e.getChangeFlags(), HierarchyEvent.PARENT_CHANGED)) {
                scheduleHide(myLink == null, myHideDelay);
            }
        };
    }

    private void initPopupBuilder() {
        JComponent tipPanel = createTipPanel();
        myPopupSize = tipPanel.getPreferredSize();
        myPopupBuilder = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(tipPanel, null)
            .setShowBorder(true)
            .setBorderColor(BORDER_COLOR)
            .setShowShadow(true);
    }

    protected void initPopupBuilder(HelpTooltipImpl instance) {
        instance.initPopupBuilder();
        myPopupSize = instance.myPopupSize;
        myPopupBuilder = instance.myPopupBuilder;
        myInitialShowScheduled = false;
    }

    private JPanel createTipPanel() {
        JPanel tipPanel = new JPanel();
        tipPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                myIsOverPopup = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (myLink == null || !myLink.getBounds().contains(e.getPoint())) {
                    myIsOverPopup = false;
                    hidePopup(false);
                }
            }
        });

        tipPanel.setLayout(new VerticalLayout(VGAP.get()));
        tipPanel.setBackground(BACKGROUND_COLOR);

        boolean hasTitle = myTitle.isNotEmpty();
        boolean hasDescription = myDescription.isNotEmpty() && !myTitle.equals(myDescription);

        if (hasTitle) {
            tipPanel.add(new Header(hasDescription), VerticalLayout.TOP);
        }

        if (hasDescription) {
            String[] pa = myDescription.get().split(PARAGRAPH_SPLITTER);
            myIsMultiline = pa.length > 1;
            Arrays.stream(pa)
                .filter(p -> !p.isEmpty())
                .forEach(p -> tipPanel.add(new Paragraph(p, hasTitle), VerticalLayout.TOP));
        }

        if (!hasTitle && StringUtil.isNotEmpty(myShortcut)) {
            JLabel shortcutLabel = new JLabel(myShortcut);
            shortcutLabel.setFont(deriveDescriptionFont(shortcutLabel.getFont(), false));
            shortcutLabel.setForeground(SHORTCUT_COLOR);

            tipPanel.add(shortcutLabel, VerticalLayout.TOP);
        }

        if (myLink != null) {
            myLink.setFont(deriveDescriptionFont(myLink.getFont(), hasTitle));
            tipPanel.add(myLink, VerticalLayout.TOP);
        }

        myIsMultiline = myIsMultiline || myDescription.isNotEmpty() && (myTitle.isNotEmpty() || myLink != null);
        tipPanel.setBorder(textBorder(myIsMultiline));

        return tipPanel;
    }

    private void installMouseListeners(JComponent owner) {
        owner.addMouseListener(myMouseListener);
        owner.addMouseMotionListener(myMouseListener);
        owner.addHierarchyListener(myHierarchyListener);
    }

    private void uninstallMouseListeners(JComponent owner) {
        owner.removeMouseListener(myMouseListener);
        owner.removeMouseMotionListener(myMouseListener);
        owner.removeHierarchyListener(myHierarchyListener);
    }

    /**
     * Hides and disposes the tooltip possibly installed on the mentioned component. Disposing means
     * unregistering all {@code HelpTooltip} specific listeners installed on the component.
     * If there is no tooltip installed on the component nothing happens.
     *
     * @param owner a possible {@code HelpTooltip} owner.
     */
    public static void dispose(Component owner) {
        if (owner instanceof JComponent component) {
            HelpTooltipImpl instance = (HelpTooltipImpl) component.getClientProperty(TOOLTIP_PROPERTY);
            if (instance != null) {
                instance.hidePopup(true);
                instance.uninstallMouseListeners(component);

                component.putClientProperty(TOOLTIP_PROPERTY, null);
                instance.myMasterPopupOpenCondition = null;
            }
        }
    }

    /**
     * Hides the tooltip possibly installed on the mentioned component without disposing.
     * Listeners are not removed.
     * If there is no tooltip installed on the component nothing happens.
     *
     * @param owner a possible {@code HelpTooltip} owner.
     */
    public static void hide(Component owner) {
        if (owner instanceof JComponent component) {
            HelpTooltipImpl instance = (HelpTooltipImpl) component.getClientProperty(TOOLTIP_PROPERTY);
            if (instance != null) {
                instance.hidePopup(true);
            }
        }
    }

    /**
     * Sets master popup for the current {@code HelpTooltip}. Master popup takes over the help tooltip,
     * so when the master popup is about to be shown help tooltip hides.
     *
     * @param owner  possible owner
     * @param master master popup
     */
    public static void setMasterPopup(Component owner, JBPopup master) {
        if (owner instanceof JComponent component) {
            HelpTooltipImpl instance = (HelpTooltipImpl) component.getClientProperty(TOOLTIP_PROPERTY);
            if (instance != null && instance.myPopup != master) {
                instance.myMasterPopupOpenCondition = () -> master == null || !master.isVisible();
            }
        }
    }

    /**
     * Sets master popup open condition supplier for the current {@code HelpTooltip}.
     * This method is more general than {@link HelpTooltipImpl#setMasterPopup(Component, JBPopup)} so that
     * it's possible to create master popup condition for any types of popups such as {@code JPopupMenu}
     *
     * @param owner     possible owner
     * @param condition a {@code BooleanSupplier} for open condition
     */
    public static void setMasterPopupOpenCondition(Component owner, @Nullable BooleanSupplier condition) {
        if (owner instanceof JComponent component) {
            HelpTooltipImpl instance = (HelpTooltipImpl) component.getClientProperty(TOOLTIP_PROPERTY);
            if (instance != null) {
                instance.myMasterPopupOpenCondition = condition;
            }
        }
    }

    @RequiredUIAccess
    private void scheduleShow(MouseEvent e, int delay) {
        myScheduleFuture.cancel(false);

        if (ScreenReader.isActive()) {
            return; // Disable HelpTooltip in screen reader mode.
        }

        schedule(
            UIAccess.current(),
            () -> {
                ComponentPopupBuilder popupBuilder = myPopupBuilder;
                if (popupBuilder == null) {
                    return;
                }
                myInitialShowScheduled = false;
                if (myMasterPopupOpenCondition == null || myMasterPopupOpenCondition.getAsBoolean()) {
                    Component owner = e.getComponent();
                    String text = owner instanceof JComponent component ? component.getToolTipText(e) : null;
                    if (myPopup != null && !myPopup.isDisposed()) {
                        if (StringUtil.isEmpty(text) && StringUtil.isEmpty(myToolTipText)) {
                            return; // do nothing if a tooltip become empty
                        }
                        if (StringUtil.equals(text, myToolTipText)) {
                            return; // do nothing if a tooltip is not changed
                        }
                        myPopup.cancel(); // cancel previous popup before showing a new one
                    }
                    myToolTipText = text;
                    myPopup = myPopupBuilder.createPopup();
                    myPopup.show(new RelativePoint(owner, myAlignment.getPointFor(owner, myPopupSize, e.getPoint())));
                    if (!myNeverHide) {
                        int dismissDelay =
                            Registry.intValue(myIsMultiline ? "ide.helptooltip.full.dismissDelay" : "ide.helptooltip.regular.dismissDelay");
                        scheduleHide(true, dismissDelay);
                    }
                }
            },
            delay
        );
    }

    @RequiredUIAccess
    private void scheduleHide(boolean force, int delay) {
        schedule(UIAccess.current(), () -> hidePopup(force), delay);
    }

    private void schedule(UIAccess uiAccess, Runnable runnable, int delay) {
        myScheduleFuture.cancel(false);

        Application application = Application.get();
        myScheduleFuture = uiAccess.getScheduler().schedule(runnable, application.getDefaultModalityState(), delay, TimeUnit.MILLISECONDS);
    }

    protected void hidePopup(boolean force) {
        myInitialShowScheduled = false;
        myScheduleFuture.cancel(false);
        if (myPopup != null && myPopup.isVisible() && (!myIsOverPopup || force)) {
            myPopup.cancel();
            myPopup = null;
        }
    }

    private static Border textBorder(boolean multiline) {
        Insets i = UIManager.getInsets(multiline ? "HelpTooltip.defaultTextBorderInsets" : "HelpTooltip.smallTextBorderInsets");
        if (i == null) {
            i = multiline ? JBUI.insets(8, 10, 10, 13) : JBUI.insets(6, 10, 7, 12);
        }
        return new JBEmptyBorder(i);
    }

    private static Font deriveHeaderFont(Font font) {
        return font.deriveFont((float) font.getSize() + HEADER_FONT_SIZE_DELTA.get());
    }

    private static Font deriveDescriptionFont(Font font, boolean hasTitle) {
        return hasTitle ? font.deriveFont((float) font.getSize() + DESCRIPTION_FONT_SIZE_DELTA.get()) : deriveHeaderFont(font);
    }

    private static class BoundWidthLabel extends JLabel {
        private static Collection<View> getRows(View root) {
            Collection<View> rows = new ArrayList<>();
            visit(root, rows);
            return rows;
        }

        private static void visit(View v, Collection<? super View> result) {
            String cName = v.getClass().getCanonicalName();
            if (cName != null && cName.contains("ParagraphView.Row")) {
                result.add(v);
            }

            for (int i = 0; i < v.getViewCount(); i++) {
                visit(v.getView(i), result);
            }
        }

        void setSizeForWidth(float width) {
            if (width > MAX_WIDTH.get()) {
                View v = (View) getClientProperty(BasicHTML.propertyKey);
                if (v != null) {
                    width = 0.0f;
                    for (View row : getRows(v)) {
                        float rWidth = row.getPreferredSpan(View.X_AXIS);
                        if (width < rWidth) {
                            width = rWidth;
                        }
                    }

                    v.setSize(width, v.getPreferredSpan(View.Y_AXIS));
                }
            }
        }
    }

    private class Header extends BoundWidthLabel {
        private Header(boolean obeyWidth) {
            setFont(deriveHeaderFont(getFont()));
            setForeground(UIUtil.getToolTipForeground());

            HtmlChunk htmlContent = new HtmlBuilder().appendRaw(myTitle).appendRaw(getShortcutAsHTML()).toFragment();
            HtmlChunk basicHtml = htmlContent.wrapWith(HtmlChunk.html());
            if (obeyWidth) {
                View v = BasicHTML.createHTMLView(this, basicHtml.toString());
                float width = v.getPreferredSpan(View.X_AXIS);
                myIsMultiline = myIsMultiline || width > MAX_WIDTH.get();
                HtmlChunk adaptedHtml = basicHtml;
                if (width > MAX_WIDTH.get()) {
                    adaptedHtml = htmlContent.wrapWith(HtmlChunk.div().attr("width", MAX_WIDTH.get())).wrapWith(HtmlChunk.html());
                }
                setText(adaptedHtml.toString());

                setSizeForWidth(width);
            }
            else {
                setText(basicHtml.toString());
            }
        }

        private String getShortcutAsHTML() {
            return StringUtil.isNotEmpty(myShortcut)
                ? String.format(" <font color=\"%s\">%s</font>", ColorUtil.toHtmlColor(SHORTCUT_COLOR), myShortcut)
                : "";
        }
    }

    private class Paragraph extends BoundWidthLabel {
        private Paragraph(String text, boolean hasTitle) {
            setForeground(hasTitle ? INFO_COLOR : UIUtil.getToolTipForeground());
            setFont(deriveDescriptionFont(getFont(), hasTitle));

            HtmlChunk htmlContent = new HtmlBuilder().appendRaw(text).toFragment();
            HtmlChunk basicHtml = htmlContent.wrapWith(HtmlChunk.html());
            View v = BasicHTML.createHTMLView(this, basicHtml.toString());
            float width = v.getPreferredSpan(View.X_AXIS);
            myIsMultiline = myIsMultiline || width > MAX_WIDTH.get();
            HtmlChunk adaptedHtml = basicHtml;
            if (width > MAX_WIDTH.get()) {
                adaptedHtml = htmlContent.wrapWith(HtmlChunk.div().attr("width", MAX_WIDTH.get())).wrapWith(HtmlChunk.html());
            }
            setText(adaptedHtml.toString());

            setSizeForWidth(width);
        }
    }
}
