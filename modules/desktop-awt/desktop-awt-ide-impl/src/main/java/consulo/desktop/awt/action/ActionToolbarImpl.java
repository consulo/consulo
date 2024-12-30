// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.action;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.desktop.awt.action.toolbar.ActionToggleToolbarButtonImpl;
import consulo.desktop.awt.action.toolbar.ActionToolbarButtonImpl;
import consulo.desktop.awt.ui.animation.AlphaAnimated;
import consulo.desktop.awt.ui.animation.AlphaAnimationContext;
import consulo.desktop.awt.ui.plaf2.flat.InplaceComponent;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.actionSystem.RightAlignedToolbarAction;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionUpdater;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Size;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awt.paint.LinePainter2D;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.internal.ActionToolbarEx;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ActionToolbarImpl extends JToolBar implements ActionToolbarEx, QuickActionProvider, AlphaAnimated {
    private static final Logger LOG = Logger.getInstance(ActionToolbarImpl.class);

    private static final String RIGHT_ALIGN_KEY = "RIGHT_ALIGN";
    private static final String SECONDARY_SHORTCUT = "SecondaryActions.shortcut";

    private static final String SUPPRESS_ACTION_COMPONENT_WARNING = "ActionToolbarImpl.suppressCustomComponentWarning";
    private static final String SUPPRESS_TARGET_COMPONENT_WARNING = "ActionToolbarImpl.suppressTargetComponentWarning";

    /**
     * This array contains Rectangles which define bounds of the corresponding
     * components in the toolbar. This list can be consider as a cache of the
     * Rectangle objects that are used in calculation of preferred sizes and
     * components layout.
     */
    private final List<Rectangle> myComponentBounds = new ArrayList<>();
    @Nonnull
    private final Style myStyle;

    private Size myMinimumButtonSize = Size.ZERO;

    /**
     * @see ActionToolbar#getLayoutPolicy()
     */
    private int myLayoutPolicy;
    private final ActionGroup myActionGroup;
    private Boolean myContentAreaFilled;
    @Nonnull
    private final String myPlace;
    List<? extends AnAction> myVisibleActions;
    private final PresentationFactory myPresentationFactory = new BasePresentationFactory();

    private final ToolbarUpdater myUpdater;

    /**
     * @see ActionToolbar#adjustTheSameSize(boolean)
     */
    private boolean myAdjustTheSameSize;

    private final DataManager myDataManager;
    protected final ActionManagerEx myActionManager;

    private Rectangle myAutoPopupRec;

    private boolean myForceMinimumSize;
    private boolean myForceShowFirstComponent;
    private boolean mySkipWindowAdjustments;

    private final Throwable myCreationTrace = new Throwable("toolbar creation trace");

    private int myFirstOutsideIndex = -1;
    private JBPopup myPopup;

    private JComponent myTargetComponent;
    private boolean myShowSeparatorTitles;

    private final AlphaAnimationContext myAlphaContext = new AlphaAnimationContext(this);

    public ActionToolbarImpl(@Nonnull String place, @Nonnull final ActionGroup actionGroup, @Nonnull Style style) {
        this(place, actionGroup, style, false);
    }

    public ActionToolbarImpl(@Nonnull String place,
                             @Nonnull ActionGroup actionGroup,
                             @Nonnull Style style,
                             boolean updateActionsNow) {
        super(null);
        myStyle = style;
        myAlphaContext.getAnimator().setVisibleImmediately(true);
        myActionManager = ActionManagerEx.getInstanceEx();
        myPlace = place;
        myActionGroup = actionGroup;
        myVisibleActions = new ArrayList<>();
        myDataManager = DataManager.getInstance();
        myUpdater = new ToolbarUpdater(KeymapManagerEx.getInstanceEx(), this) {
            @Override
            protected void updateActionsImpl(boolean transparentOnly, boolean forced) {
                if (!ApplicationManager.getApplication().isDisposedOrDisposeInProgress()) {
                    ActionToolbarImpl.this.updateActionsImpl(transparentOnly, forced);
                }
            }
        };

        setOrientation(style.isHorizontal() ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL);

        myUpdater.updateActions(updateActionsNow, false);

        // If the panel doesn't handle mouse event then it will be passed to its parent.
        // It means that if the panel is in sliding mode then the focus goes to the editor
        // and panel will be automatically hidden.
        enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
        setMiniMode(false);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        for (Component component : getComponents()) {
            tweakActionComponentUI(component);
        }
    }

    @Nonnull
    public String getPlace() {
        return myPlace;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ActionToolbarsHolder.add(this);

        // should update action right on the showing, otherwise toolbar may not be displayed at all,
        // since by default all updates are postponed until frame gets focused.
        updateActionsImmediately();
    }

    private boolean isInsideNavBar() {
        return ActionPlaces.NAVIGATION_BAR_TOOLBAR.equals(myPlace);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        ActionToolbarsHolder.remove(this);

        if (myPopup != null) {
            myPopup.cancel();
            myPopup = null;
        }

        CancellablePromise<List<AnAction>> lastUpdate = myLastUpdate;
        if (lastUpdate != null) {
            lastUpdate.cancel();
        }
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public int getLayoutPolicy() {
        return myLayoutPolicy;
    }

    @Override
    public void setLayoutPolicy(int layoutPolicy) {
        if (layoutPolicy != NOWRAP_LAYOUT_POLICY && layoutPolicy != WRAP_LAYOUT_POLICY && layoutPolicy != AUTO_LAYOUT_POLICY) {
            throw new IllegalArgumentException("wrong layoutPolicy: " + layoutPolicy);
        }
        myLayoutPolicy = layoutPolicy;
    }

    @Nonnull
    public ActionGroup getActionGroup() {
        return myActionGroup;
    }

    @Nonnull
    @Override
    public AlphaAnimationContext getAlphaContext() {
        return myAlphaContext;
    }

    @Override
    public void paint(Graphics g) {
        myAlphaContext.paint(g, () -> super.paint(g));
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (myLayoutPolicy == AUTO_LAYOUT_POLICY && myAutoPopupRec != null) {
            Image link = PlatformIconGroup.ideLink();
            
            if (getOrientation() == SwingConstants.HORIZONTAL) {
                final int dy = myAutoPopupRec.height / 2 - link.getHeight() / 2;
                TargetAWT.to(link)
                    .paintIcon(this, g, (int) myAutoPopupRec.getMaxX() - link.getWidth() - 1, myAutoPopupRec.y + dy);
            }
            else {
                final int dx = myAutoPopupRec.width / 2 - link.getWidth() / 2;
                TargetAWT.to(link)
                    .paintIcon(this, g, myAutoPopupRec.x + dx, (int) myAutoPopupRec.getMaxY() - link.getWidth() - 1);
            }
        }
    }

    private void fillToolBar(@Nonnull final List<? extends AnAction> actions) {
        boolean isLastElementSeparator = false;
        final List<AnAction> rightAligned = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            AnAction action = actions.get(i);
            if (action instanceof RightAlignedToolbarAction) {
                rightAligned.add(action);
                continue;
            }

            if (action instanceof AnSeparator separator) {
                if (isLastElementSeparator) {
                    continue;
                }
                if (i > 0 && i < actions.size() - 1) {
                    add(
                        SEPARATOR_CONSTRAINT,
                        new MySeparator(myShowSeparatorTitles ? separator.getTextValue() : LocalizeValue.empty())
                    );
                    isLastElementSeparator = true;
                    continue;
                }
            }
            else if (action instanceof CustomComponentAction) {
                add(CUSTOM_COMPONENT_CONSTRAINT, getCustomComponent(action));
            }
            else {
                add(ACTION_BUTTON_CONSTRAINT, createToolbarButton(action).getComponent());
            }
            isLastElementSeparator = false;
        }

        for (AnAction action : rightAligned) {
            JComponent button = action instanceof CustomComponentAction ? getCustomComponent(action) : createToolbarButton(action).getComponent();
            if (!isInsideNavBar()) {
                button.putClientProperty(RIGHT_ALIGN_KEY, Boolean.TRUE);
            }
            add(button);
        }
    }

    @Nonnull
    private JComponent getCustomComponent(@Nonnull AnAction action) {
        Presentation presentation = myPresentationFactory.getPresentation(action);
        JComponent customComponent = presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY);
        if (customComponent == null) {
            customComponent = ((CustomComponentAction) action).createCustomComponent(presentation, myPlace);
            presentation.putClientProperty(CustomComponentAction.COMPONENT_KEY, customComponent);
            UIUtil.putClientProperty(customComponent, CustomComponentAction.ACTION_KEY, action);
        }
        tweakActionComponentUI(customComponent);

        //AbstractButton clickable = UIUtil.findComponentOfType(customComponent, AbstractButton.class);
        //if (clickable != null) {
        //  class ToolbarClicksCollectorListener extends MouseAdapter {
        //    @Override
        //    public void mouseClicked(MouseEvent e) {
        //      ToolbarClicksCollector.record(action, myPlace, e, getDataContext());
        //    }
        //  }
        //  if (Arrays.stream(clickable.getMouseListeners()).noneMatch(ml -> ml instanceof ToolbarClicksCollectorListener)) {
        //    clickable.addMouseListener(new ToolbarClicksCollectorListener());
        //  }
        //}
        return customComponent;
    }

    private void tweakActionComponentUI(@Nonnull Component actionComponent) {
        if (ActionPlaces.EDITOR_TOOLBAR.equals(myPlace)) {
            // tweak font & color for editor toolbar to match editor tabs style
            actionComponent.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
            actionComponent.setForeground(ColorUtil.dimmer(JBColor.BLACK));
        }

        if (myStyle == Style.INPLACE && actionComponent instanceof JComponent jComponent) {
            InplaceComponent.prepareLeadingOrTrailingComponent(jComponent);
        }
    }

    public boolean useDefaultLayout() {
        return myStyle == Style.INPLACE || myStyle == Style.BUTTON;
    }

    @Nonnull
    protected ActionButton createToolbarButton(@Nonnull AnAction action,
                                               @Nonnull String place,
                                               @Nonnull Presentation presentation,
                                               @Nonnull Size minimumSize) {
        ActionButton actionButton;
        if (action instanceof Toggleable toggleable) {
            actionButton = new ActionToggleToolbarButtonImpl(action, presentation, place, minimumSize) {
                @Override
                protected DataContext getDataContext() {
                    return getToolbarDataContext();
                }
            };
        }
        else {
            actionButton = new ActionToolbarButtonImpl(action, presentation, place, minimumSize) {
                @Override
                protected DataContext getDataContext() {
                    return getToolbarDataContext();
                }
            };
        }

        if (myContentAreaFilled != null) {
            actionButton.setContentAreaFilled(myContentAreaFilled);
        }

        tweakActionComponentUI(actionButton.getComponent());

        return actionButton;
    }

    @Nonnull
    private ActionButton createToolbarButton(@Nonnull AnAction action) {
        return createToolbarButton(action,
            myPlace,
            myPresentationFactory.getPresentation(action),
            myMinimumButtonSize);
    }

    @Override
    public void doLayout() {
        if (useDefaultLayout()) {
            super.doLayout();
            return;
        }

        if (!isValid()) {
            calculateBounds(getSize(), myComponentBounds);
        }
        final int componentCount = getComponentCount();
        LOG.assertTrue(componentCount <= myComponentBounds.size());
        for (int i = componentCount - 1; i >= 0; i--) {
            final Component component = getComponent(i);
            component.setBounds(myComponentBounds.get(i));
        }
    }

    @Override
    public void validate() {
        if (useDefaultLayout()) {
            super.validate();
            return;
        }

        if (!isValid()) {
            calculateBounds(getSize(), myComponentBounds);
            super.validate();
        }
    }

    private Dimension getChildPreferredSize(int index) {
        Component component = getComponent(index);
        return component.isVisible() ? component.getPreferredSize() : new Dimension();
    }

    /**
     * @return maximum button width
     */
    private int getMaxButtonWidth() {
        int width = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            final Dimension dimension = getChildPreferredSize(i);
            width = Math.max(width, dimension.width);
        }
        return width;
    }

    /**
     * @return maximum button height
     */
    @Override
    public int getMaxButtonHeight() {
        int height = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            final Dimension dimension = getChildPreferredSize(i);
            height = Math.max(height, dimension.height);
        }
        return height;
    }

    private void calculateBoundsNowrapImpl(@Nonnull List<? extends Rectangle> bounds) {
        final int componentCount = getComponentCount();
        LOG.assertTrue(componentCount <= bounds.size());

        final int width = getWidth();
        final int height = getHeight();

        final Insets insets = getInsets();

        if (myAdjustTheSameSize) {
            final int maxWidth = getMaxButtonWidth();
            final int maxHeight = getMaxButtonHeight();

            int offset = 0;
            if (getOrientation() == SwingConstants.HORIZONTAL) {
                for (int i = 0; i < componentCount; i++) {
                    final Rectangle r = bounds.get(i);
                    r.setBounds(insets.left + offset, insets.top + (height - maxHeight) / 2, maxWidth, maxHeight);
                    offset += maxWidth;
                }
            }
            else {
                for (int i = 0; i < componentCount; i++) {
                    final Rectangle r = bounds.get(i);
                    r.setBounds(insets.left + (width - maxWidth) / 2, insets.top + offset, maxWidth, maxHeight);
                    offset += maxHeight;
                }
            }
        }
        else {
            if (getOrientation() == SwingConstants.HORIZONTAL) {
                final int maxHeight = getMaxButtonHeight();
                int offset = 0;
                for (int i = 0; i < componentCount; i++) {
                    final Dimension d = getChildPreferredSize(i);
                    final Rectangle r = bounds.get(i);
                    r.setBounds(insets.left + offset, insets.top + (maxHeight - d.height) / 2, d.width, d.height);
                    offset += d.width;
                }
            }
            else {
                final int maxWidth = getMaxButtonWidth();
                int offset = 0;
                for (int i = 0; i < componentCount; i++) {
                    final Dimension d = getChildPreferredSize(i);
                    final Rectangle r = bounds.get(i);
                    r.setBounds(insets.left + (maxWidth - d.width) / 2, insets.top + offset, d.width, d.height);
                    offset += d.height;
                }
            }
        }
    }

    private void calculateBoundsAutoImp(@Nonnull Dimension sizeToFit, @Nonnull List<? extends Rectangle> bounds) {
        final int componentCount = getComponentCount();
        LOG.assertTrue(componentCount <= bounds.size());

        final boolean actualLayout = bounds == myComponentBounds;

        if (actualLayout) {
            myAutoPopupRec = null;
        }

        int autoButtonSize = AllIcons.Ide.Link.getWidth();
        boolean full = false;

        final Insets insets = getInsets();
        int widthToFit = sizeToFit.width - insets.left - insets.right;
        int heightToFit = sizeToFit.height - insets.top - insets.bottom;

        if (getOrientation() == SwingConstants.HORIZONTAL) {
            int eachX = 0;
            int maxHeight = heightToFit;
            for (int i = 0; i < componentCount; i++) {
                final Component eachComp = getComponent(i);
                final boolean isLast = i == componentCount - 1;

                final Rectangle eachBound = new Rectangle(getChildPreferredSize(i));
                maxHeight = Math.max(eachBound.height, maxHeight);

                if (!full) {
                    boolean inside = isLast ? eachX + eachBound.width <= widthToFit : eachX + eachBound.width + autoButtonSize <= widthToFit;

                    if (inside) {
                        eachBound.x = insets.left + eachX;
                        eachX += eachBound.width;
                        eachBound.y = insets.top;
                    }
                    else {
                        full = true;
                    }
                }

                if (full) {
                    if (myAutoPopupRec == null) {
                        myAutoPopupRec = new Rectangle(insets.left + eachX, insets.top, widthToFit - eachX, heightToFit);
                        myFirstOutsideIndex = i;
                    }
                    eachBound.x = Integer.MAX_VALUE;
                    eachBound.y = Integer.MAX_VALUE;
                }

                bounds.get(i).setBounds(eachBound);
            }

            for (final Rectangle r : bounds) {
                if (r.height < maxHeight) {
                    r.y += (maxHeight - r.height) / 2;
                }
            }

        }
        else {
            int eachY = 0;
            for (int i = 0; i < componentCount; i++) {
                final Rectangle eachBound = new Rectangle(getChildPreferredSize(i));
                if (!full) {
                    boolean outside;
                    if (i < componentCount - 1) {
                        outside = eachY + eachBound.height + autoButtonSize < heightToFit;
                    }
                    else {
                        outside = eachY + eachBound.height < heightToFit;
                    }
                    if (outside) {
                        eachBound.x = insets.left;
                        eachBound.y = insets.top + eachY;
                        eachY += eachBound.height;
                    }
                    else {
                        full = true;
                    }
                }

                if (full) {
                    if (myAutoPopupRec == null) {
                        myAutoPopupRec = new Rectangle(insets.left, insets.top + eachY, widthToFit, heightToFit - eachY);
                        myFirstOutsideIndex = i;
                    }
                    eachBound.x = Integer.MAX_VALUE;
                    eachBound.y = Integer.MAX_VALUE;
                }

                bounds.get(i).setBounds(eachBound);
            }
        }

    }

    private void calculateBoundsWrapImpl(@Nonnull Dimension sizeToFit, @Nonnull List<? extends Rectangle> bounds) {
        // We have to graceful handle case when toolbar was not laid out yet.
        // In this case we calculate bounds as it is a NOWRAP toolbar.
        if (getWidth() == 0 || getHeight() == 0) {
            try {
                setLayoutPolicy(NOWRAP_LAYOUT_POLICY);
                calculateBoundsNowrapImpl(bounds);
            }
            finally {
                setLayoutPolicy(WRAP_LAYOUT_POLICY);
            }
            return;
        }

        Dimension minSize = TargetAWT.to(myMinimumButtonSize);

        final int componentCount = getComponentCount();
        LOG.assertTrue(componentCount <= bounds.size());

        final Insets insets = getInsets();
        int widthToFit = sizeToFit.width - insets.left - insets.right;
        int heightToFit = sizeToFit.height - insets.top - insets.bottom;

        int orientation = getOrientation();
        if (myAdjustTheSameSize) {
            final int maxWidth = getMaxButtonWidth();
            final int maxHeight = getMaxButtonHeight();
            int xOffset = 0;
            int yOffset = 0;
            if (orientation == SwingConstants.HORIZONTAL) {

                // Lay components out
                int maxRowWidth = getMaxRowWidth(widthToFit, maxWidth);
                for (int i = 0; i < componentCount; i++) {
                    if (xOffset + maxWidth > maxRowWidth) { // place component at new row
                        xOffset = 0;
                        yOffset += maxHeight;
                    }

                    final Rectangle each = bounds.get(i);
                    each.setBounds(insets.left + xOffset, insets.top + yOffset, maxWidth, maxHeight);

                    xOffset += maxWidth;
                }
            }
            else {

                // Lay components out
                // Calculate max size of a row. It's not possible to make more then 3 column toolbar
                final int maxRowHeight = Math.max(heightToFit, componentCount * minSize.height / 3);
                for (int i = 0; i < componentCount; i++) {
                    if (yOffset + maxHeight > maxRowHeight) { // place component at new row
                        yOffset = 0;
                        xOffset += maxWidth;
                    }

                    final Rectangle each = bounds.get(i);
                    each.setBounds(insets.left + xOffset, insets.top + yOffset, maxWidth, maxHeight);

                    yOffset += maxHeight;
                }
            }
        }
        else {
            if (orientation == SwingConstants.HORIZONTAL) {
                // Calculate row height
                int rowHeight = 0;
                final Dimension[] dims = new Dimension[componentCount]; // we will use this dimensions later
                for (int i = 0; i < componentCount; i++) {
                    dims[i] = getChildPreferredSize(i);
                    final int height = dims[i].height;
                    rowHeight = Math.max(rowHeight, height);
                }

                // Lay components out
                int xOffset = 0;
                int yOffset = 0;
                // Calculate max size of a row. It's not possible to make more then 3 row toolbar
                int maxRowWidth = getMaxRowWidth(widthToFit, minSize.width);

                for (int i = 0; i < componentCount; i++) {
                    final Dimension d = dims[i];
                    if (xOffset + d.width > maxRowWidth) { // place component at new row
                        xOffset = 0;
                        yOffset += rowHeight;
                    }

                    final Rectangle each = bounds.get(i);
                    each.setBounds(insets.left + xOffset, insets.top + yOffset + (rowHeight - d.height) / 2, d.width, d.height);

                    xOffset += d.width;
                }
            }
            else {
                // Calculate row width
                int rowWidth = 0;
                final Dimension[] dims = new Dimension[componentCount]; // we will use this dimensions later
                for (int i = 0; i < componentCount; i++) {
                    dims[i] = getChildPreferredSize(i);
                    final int width = dims[i].width;
                    rowWidth = Math.max(rowWidth, width);
                }

                // Lay components out
                int xOffset = 0;
                int yOffset = 0;
                // Calculate max size of a row. It's not possible to make more then 3 column toolbar
                final int maxRowHeight = Math.max(heightToFit, componentCount * minSize.height / 3);
                for (int i = 0; i < componentCount; i++) {
                    final Dimension d = dims[i];
                    if (yOffset + d.height > maxRowHeight) { // place component at new row
                        yOffset = 0;
                        xOffset += rowWidth;
                    }

                    final Rectangle each = bounds.get(i);
                    each.setBounds(insets.left + xOffset + (rowWidth - d.width) / 2, insets.top + yOffset, d.width, d.height);

                    yOffset += d.height;
                }
            }
        }
    }

    private int getMaxRowWidth(int widthToFit, int maxWidth) {
        int componentCount = getComponentCount();
        // Calculate max size of a row. It's not possible to make more than 3 row toolbar
        int maxRowWidth = Math.max(widthToFit, componentCount * maxWidth / 3);
        for (int i = 0; i < componentCount; i++) {
            final Component component = getComponent(i);
            if (component instanceof JComponent jComponent && jComponent.getClientProperty(RIGHT_ALIGN_KEY) == Boolean.TRUE) {
                maxRowWidth -= getChildPreferredSize(i).width;
            }
        }
        return maxRowWidth;
    }

    /**
     * Calculates bounds of all the components in the toolbar
     */
    private void calculateBounds(@Nonnull Dimension size2Fit, @Nonnull List<Rectangle> bounds) {
        bounds.clear();
        for (int i = 0; i < getComponentCount(); i++) {
            bounds.add(new Rectangle());
        }

        if (myLayoutPolicy == NOWRAP_LAYOUT_POLICY) {
            calculateBoundsNowrapImpl(bounds);
        }
        else if (myLayoutPolicy == WRAP_LAYOUT_POLICY) {
            calculateBoundsWrapImpl(size2Fit, bounds);
        }
        else if (myLayoutPolicy == AUTO_LAYOUT_POLICY) {
            calculateBoundsAutoImp(size2Fit, bounds);
        }
        else {
            throw new IllegalStateException("unknown layoutPolicy: " + myLayoutPolicy);
        }


        if (getComponentCount() > 0 && size2Fit.width < Integer.MAX_VALUE) {
            int maxHeight = 0;
            for (int i = 0; i < bounds.size() - 2; i++) {
                maxHeight = Math.max(maxHeight, bounds.get(i).height);
            }

            int rightOffset = 0;
            Insets insets = getInsets();
            for (int i = getComponentCount() - 1, j = 1; i > 0; i--, j++) {
                final Component component = getComponent(i);
                if (component instanceof JComponent jComponent && jComponent.getClientProperty(RIGHT_ALIGN_KEY) == Boolean.TRUE) {
                    rightOffset += bounds.get(i).width;
                    Rectangle r = bounds.get(bounds.size() - j);
                    r.x = size2Fit.width - rightOffset;
                    r.y = insets.top + (getHeight() - insets.top - insets.bottom - bounds.get(i).height) / 2;
                }
            }
        }
    }

    @Override
    @Nonnull
    public Dimension getPreferredSize() {
        if (useDefaultLayout()) {
            return super.getPreferredSize();
        }

        final ArrayList<Rectangle> bounds = new ArrayList<>();
        calculateBounds(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE), bounds);//it doesn't take into account wrapping
        if (bounds.isEmpty()) {
            return JBUI.emptySize();
        }
        int forcedHeight = 0;
        int orientation = getOrientation();
        if (getWidth() > 0 && getLayoutPolicy() == ActionToolbar.WRAP_LAYOUT_POLICY && orientation == SwingConstants.HORIZONTAL) {
            final ArrayList<Rectangle> limitedBounds = new ArrayList<>();
            calculateBounds(new Dimension(getWidth(), Integer.MAX_VALUE), limitedBounds);
            Rectangle union = null;
            for (Rectangle bound : limitedBounds) {
                union = union == null ? bound : union.union(bound);
            }
            forcedHeight = union != null ? union.height : 0;
        }
        int xLeft = Integer.MAX_VALUE;
        int yTop = Integer.MAX_VALUE;
        int xRight = Integer.MIN_VALUE;
        int yBottom = Integer.MIN_VALUE;
        for (int i = bounds.size() - 1; i >= 0; i--) {
            final Rectangle each = bounds.get(i);
            if (each.x == Integer.MAX_VALUE) {
                continue;
            }
            xLeft = Math.min(xLeft, each.x);
            yTop = Math.min(yTop, each.y);
            xRight = Math.max(xRight, each.x + each.width);
            yBottom = Math.max(yBottom, each.y + each.height);
        }
        final Dimension dimension = new Dimension(xRight - xLeft, Math.max(yBottom - yTop, forcedHeight));

        JBInsets.addTo(dimension, getInsets());

        return dimension;
    }

    /**
     * Forces the minimum size of the toolbar to show all buttons, When set to {@code true}. By default ({@code false}) the
     * toolbar will shrink further and show the auto popup chevron button.
     */
    @Override
    public void setForceMinimumSize(boolean force) {
        myForceMinimumSize = force;
    }

    /**
     * By default minimum size is to show chevron only.
     * If this option is {@code true} toolbar shows at least one (the first) component plus chevron (if need)
     */
    @Override
    public void setForceShowFirstComponent(boolean showFirstComponent) {
        myForceShowFirstComponent = showFirstComponent;
    }

    /**
     * This option makes sense when you use a toolbar inside JBPopup
     * When some 'actions' are hidden under the chevron the popup with extra components would be shown/hidden
     * with size adjustments for the main popup (this is default behavior).
     * If this option is {@code true} size adjustments would be omitted
     */
    public void setSkipWindowAdjustments(boolean skipWindowAdjustments) {
        mySkipWindowAdjustments = skipWindowAdjustments;
    }

    @Override
    public Dimension getMinimumSize() {
        if (useDefaultLayout()) {
            return super.getMinimumSize();
        }

        if (myForceMinimumSize) {
            return getPreferredSize();
        }
        if (myLayoutPolicy == AUTO_LAYOUT_POLICY) {
            Dimension minSize = TargetAWT.to(myMinimumButtonSize);

            final Insets i = getInsets();
            if (myForceShowFirstComponent && getComponentCount() > 0) {
                Component c = getComponent(0);
                Dimension firstSize = c.getPreferredSize();
                if (getOrientation() == SwingConstants.HORIZONTAL) {
                    return new Dimension(firstSize.width + AllIcons.Ide.Link.getWidth() + i.left + i.right,
                        Math.max(firstSize.height, minSize.height) + i.top + i.bottom);
                }
                else {
                    return new Dimension(Math.max(firstSize.width, AllIcons.Ide.Link.getWidth()) + i.left + i.right,
                        firstSize.height + minSize.height + i.top + i.bottom);
                }
            }
            return new Dimension(AllIcons.Ide.Link.getWidth() + i.left + i.right, minSize.height + i.top + i.bottom);
        }
        else {
            return super.getMinimumSize();
        }
    }

    private final class MySeparator extends JComponent {
        @Nonnull
        private final LocalizeValue myTextValue;

        private MySeparator(@Nonnull LocalizeValue textValue) {
            myTextValue = textValue;
            setFont(JBUI.Fonts.toolbarSmallComboBoxFont());
        }

        @Override
        public Dimension getPreferredSize() {
            int gap = JBUIScale.scale(2);
            int center = JBUIScale.scale(3);
            int width = gap * 2 + center;
            int height = JBUIScale.scale(24);

            if (getOrientation() == SwingConstants.HORIZONTAL) {
                if (myTextValue != LocalizeValue.empty()) {
                    FontMetrics fontMetrics = getFontMetrics(getFont());

                    int textWidth = getTextWidth(fontMetrics, myTextValue.get(), getGraphics());
                    return new JBDimension(width + gap * 2 + textWidth, Math.max(fontMetrics.getHeight(), height), true);
                }
                else {
                    return new JBDimension(width, height, true);
                }
            }
            else {
                //noinspection SuspiciousNameCombination
                return new JBDimension(height, width, true);
            }
        }

        @Override
        protected void paintComponent(final Graphics g) {
            if (getParent() == null) {
                return;
            }

            int gap = JBUIScale.scale(2);
            int center = JBUIScale.scale(3);
            int offset;
            int orientation = getOrientation();
            if (orientation == SwingConstants.HORIZONTAL) {
                offset = ActionToolbarImpl.this.getHeight() - getMaxButtonHeight() - 1;
            }
            else {
                offset = ActionToolbarImpl.this.getWidth() - getMaxButtonWidth() - 1;
            }

            g.setColor(JBColor.border());
            if (orientation == SwingConstants.HORIZONTAL) {
                int y2 = ActionToolbarImpl.this.getHeight() - gap * 2 - offset;
                LinePainter2D.paint((Graphics2D) g, center, gap, center, y2);

                if (myTextValue != LocalizeValue.empty()) {
                    FontMetrics fontMetrics = getFontMetrics(getFont());
                    int top = (getHeight() - fontMetrics.getHeight()) / 2;
                    UISettingsUtil.setupAntialiasing(g);
                    g.setColor(JBColor.foreground());
                    g.drawString(myTextValue.getValue(), gap * 2 + center + gap, top + fontMetrics.getAscent());
                }
            }
            else {
                LinePainter2D.paint((Graphics2D) g, gap, center, ActionToolbarImpl.this.getWidth() - gap * 2 - offset, center);
            }
        }

        private int getTextWidth(@Nonnull FontMetrics fontMetrics, @Nonnull String text, @Nullable Graphics graphics) {
            if (graphics == null) {
                return fontMetrics.stringWidth(text);
            }
            else {
                Graphics g = graphics.create();
                try {
                    UISettingsUtil.setupAntialiasing(g);
                    return fontMetrics.getStringBounds(text, g).getBounds().width;
                }
                finally {
                    g.dispose();
                }
            }
        }
    }

    @Override
    public void adjustTheSameSize(final boolean value) {
        if (myAdjustTheSameSize == value) {
            return;
        }
        myAdjustTheSameSize = value;
        revalidate();
    }

    @Override
    public void setMinimumButtonSize(@Nonnull final Size size) {
        myMinimumButtonSize = size;
        for (int i = getComponentCount() - 1; i >= 0; i--) {
            final Component component = getComponent(i);
            if (component instanceof ActionButton button) {
                button.getComponent().setMinimumSize(TargetAWT.to(size));
            }
        }
        revalidate();
    }

    @RequiredUIAccess
    @Override
    public void updateActionsImmediately() {
        UIAccess.assertIsUIThread();
        myUpdater.updateActions(true, false);
    }

    private boolean myAlreadyUpdated;

    private void updateActionsImpl(boolean transparentOnly, boolean forced) {
        DataContext dataContext = getDataContext();
        boolean async =
            myAlreadyUpdated && Registry.is("actionSystem.update.actions.asynchronously") && ActionToolbarsHolder.contains(this) && isShowing();
        ActionUpdater updater =
            new ActionUpdater(myActionManager,
                LaterInvocator.isInModalContext(),
                myPresentationFactory,
                async ? DataManager.getInstance().createAsyncDataContext(dataContext) : dataContext,
                myPlace,
                false,
                true);
        if (async) {
            if (myLastUpdate != null) {
                myLastUpdate.cancel();
            }

            myLastUpdate = updater.expandActionGroupAsync(myActionGroup, false);
            myLastUpdate.onSuccess(actions -> actionsUpdated(forced, actions)).onProcessed(__ -> myLastUpdate = null);
        }
        else {
            actionsUpdated(forced, updater.expandActionGroupWithTimeout(myActionGroup, false));
            myAlreadyUpdated = true;
        }
    }

    private CancellablePromise<List<AnAction>> myLastUpdate;

    private void actionsUpdated(boolean forced, @Nonnull List<? extends AnAction> newVisibleActions) {
        if (forced || !newVisibleActions.equals(myVisibleActions)) {
            boolean shouldRebuildUI = newVisibleActions.isEmpty() || myVisibleActions.isEmpty();
            myVisibleActions = newVisibleActions;

            Dimension oldSize = getPreferredSize();

            removeAll();
            fillToolBar(myVisibleActions);

            Dimension newSize = getPreferredSize();

            if (!mySkipWindowAdjustments) {
                ((WindowManagerEx) WindowManager.getInstance()).adjustContainerWindow(this, oldSize, newSize);
            }

            if (shouldRebuildUI) {
                revalidate();
            }
            else {
                Container parent = getParent();
                if (parent != null) {
                    parent.invalidate();
                    parent.validate();
                }
            }

            repaint();
        }
    }


    @Override
    public boolean hasVisibleActions() {
        return !myVisibleActions.isEmpty();
    }

    @Override
    public void setTargetComponent(final JComponent component) {
        if (myTargetComponent == null) {
            putClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING, true);
        }

        myTargetComponent = component;

        if (myTargetComponent != component) {
            myTargetComponent = component;
            if (isShowing()) {
                updateActionsImmediately();
            }
        }
    }

    @Nonnull
    @Override
    public DataContext getToolbarDataContext() {
        return getDataContext();
    }

    @Override
    public void setShowSeparatorTitles(boolean showSeparatorTitles) {
        myShowSeparatorTitles = showSeparatorTitles;
    }

    @Nonnull
    protected DataContext getDataContext() {
        if (myTargetComponent == null && getClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING) == null && !ApplicationManager.getApplication()
            .isUnitTestMode()) {
            putClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING, true);
            LOG.warn("'" + myPlace + "' toolbar by default uses any focused component to update its actions. " +
                "Toolbar actions that need local UI context would be incorrectly disabled. " +
                "Please call toolbar.setTargetComponent() explicitly.", myCreationTrace);
        }
        Component target = myTargetComponent != null ? myTargetComponent : IJSwingUtilities.getFocusedComponentInWindowOrSelf(this);
        return myDataManager.getDataContext(target);
    }

    @Override
    protected void processMouseMotionEvent(final MouseEvent e) {
        super.processMouseMotionEvent(e);

        if (getLayoutPolicy() != AUTO_LAYOUT_POLICY) {
            return;
        }
        if (myAutoPopupRec != null && myAutoPopupRec.contains(e.getPoint())) {
            ApplicationIdeFocusManager.getInstance().doWhenFocusSettlesDown(() -> showAutoPopup());
        }
    }

    private void showAutoPopup() {
        if (isPopupShowing()) {
            return;
        }

        final ActionGroup group;
        int orientation = getOrientation();
        if (orientation == SwingConstants.HORIZONTAL) {
            group = myActionGroup;
        }
        else {
            final DefaultActionGroup outside = new DefaultActionGroup();
            for (int i = myFirstOutsideIndex; i < myVisibleActions.size(); i++) {
                outside.add(myVisibleActions.get(i));
            }
            group = outside;
        }

        PopupToolbar popupToolbar = new PopupToolbar(myPlace, group, this) {
            @Override
            protected void onOtherActionPerformed() {
                hidePopup();
            }

            @Nonnull
            @Override
            protected DataContext getDataContext() {
                return ActionToolbarImpl.this.getDataContext();
            }
        };
        popupToolbar.setLayoutPolicy(NOWRAP_LAYOUT_POLICY);
        popupToolbar.updateActionsImmediately();

        Point location;
        if (orientation == SwingConstants.HORIZONTAL) {
            location = getLocationOnScreen();
        }
        else {
            location = getLocationOnScreen();
            location.y = location.y + getHeight() - popupToolbar.getPreferredSize().height;
        }


        final ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(popupToolbar, null);
        builder.setResizable(false)
            .setMovable(true) // fit the screen automatically
            .setRequestFocus(false)
            .setTitle(null)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setCancelCallback(() -> {
                final boolean toClose = myActionManager.isActionPopupStackEmpty();
                if (toClose) {
                    myUpdater.updateActions(false, true);
                }
                return toClose;
            })
            .setCancelOnMouseOutCallback(event -> myAutoPopupRec != null && myActionManager.isActionPopupStackEmpty() && !new RelativeRectangle(
                this,
                myAutoPopupRec).contains(new RelativePoint(event)));

        builder.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(@Nonnull LightweightWindowEvent event) {
                processClosed();
            }
        });
        myPopup = builder.createPopup();
        Disposer.register(myPopup, popupToolbar);

        myPopup.showInScreenCoordinates(this, location);

        final Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            final ComponentAdapter componentAdapter = new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) {
                    hidePopup();
                }

                @Override
                public void componentMoved(final ComponentEvent e) {
                    hidePopup();
                }

                @Override
                public void componentShown(final ComponentEvent e) {
                    hidePopup();
                }

                @Override
                public void componentHidden(final ComponentEvent e) {
                    hidePopup();
                }
            };
            window.addComponentListener(componentAdapter);
            Disposer.register(popupToolbar, () -> window.removeComponentListener(componentAdapter));
        }
    }


    private boolean isPopupShowing() {
        return myPopup != null && !myPopup.isDisposed();
    }

    private void hidePopup() {
        if (myPopup != null) {
            myPopup.cancel();
            processClosed();
        }
    }

    private void processClosed() {
        if (myPopup == null) {
            return;
        }
        if (myPopup.isVisible()) {
            // setCancelCallback(..) can override cancel()
            return;
        }
        // cancel() already called Disposer.dispose()
        myPopup = null;
        myUpdater.updateActions(false, false);
    }

    abstract static class PopupToolbar extends ActionToolbarImpl implements AnActionListener, DataProvider, Disposable {
        private final JComponent myParent;

        PopupToolbar(@Nonnull String place, @Nonnull ActionGroup actionGroup, @Nonnull JComponent parent) {
            super(place, actionGroup, Style.HORIZONTAL, true);
            ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AnActionListener.class, this);
            myParent = parent;
            setBorder(myParent.getBorder());
        }

        @Nullable
        @Override
        public Object getData(@Nonnull Key dataId) {
            return getDataContext().getData(dataId);
        }

        @Override
        public Container getParent() {
            Container parent = super.getParent();
            return parent != null ? parent : myParent;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void afterActionPerformed(@Nonnull final AnAction action, @Nonnull final DataContext dataContext, @Nonnull AnActionEvent event) {
            if (!myVisibleActions.contains(action)) {
                onOtherActionPerformed();
            }
        }

        protected abstract void onOtherActionPerformed();
    }

    @Nonnull
    @Override
    public List<AnAction> getActions(boolean originalProvider) {
        return getActions();
    }

    @Nonnull
    @Override
    public List<AnAction> getActions() {
        AnAction[] kids = myActionGroup.getChildren(null);
        return List.of(kids);
    }

    @Override
    public void setMiniMode(boolean minimalMode) {
        if (minimalMode) {
            setMinimumButtonSize(Size.ZERO);
            setLayoutPolicy(NOWRAP_LAYOUT_POLICY);
            setBorder(JBUI.Borders.empty());
            setOpaque(false);
        }
        else {
            setBorder(JBUI.Borders.empty(2));
            setMinimumButtonSize(DEFAULT_MINIMUM_BUTTON_SIZE);
            setOpaque(true);
            setLayoutPolicy(AUTO_LAYOUT_POLICY);
        }

        myUpdater.updateActions(false, true);
    }

    @TestOnly
    public Presentation getPresentation(AnAction action) {
        return myPresentationFactory.getPresentation(action);
    }

    /**
     * Clear internal caches.
     * <p>
     * This method can be called after updating {@link ActionToolbarImpl#myActionGroup}
     * to make sure toolbar does not reference old {@link AnAction} instances.
     */
    @Override
    @RequiredUIAccess
    public void reset() {
        cancelCurrentUpdate();

        myPresentationFactory.reset();
        myVisibleActions.clear();
        removeAll();
    }

    private void cancelCurrentUpdate() {
        CancellablePromise<List<AnAction>> lastUpdate = myLastUpdate;
        myLastUpdate = null;
        if (lastUpdate != null) {
            lastUpdate.cancel();
        }
    }

    @Override
    public void forEachButton(Consumer<ActionButton> buttonConsumer) {
        for (Component component : getComponents()) {
            if (component instanceof ActionButton actionButton) {
                buttonConsumer.accept(actionButton);
            }
        }
    }

    @Override
    public void setContentAreaFilled(boolean contentAreaFilled) {
        myContentAreaFilled = contentAreaFilled;

        for (Component component : getComponents()) {
            if (component instanceof ActionButton actionButton) {
                actionButton.setContentAreaFilled(myContentAreaFilled);
            }
        }
    }
}
