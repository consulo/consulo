// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.TextWithMnemonic;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.ui.paint.LinePainter2D;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.switcher.QuickActionProvider;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ui.*;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.concurrency.CancellablePromise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ActionToolbarImpl extends JPanel implements ActionToolbar, QuickActionProvider {
  private static final Logger LOG = Logger.getInstance(ActionToolbarImpl.class);

  private static final Set<ActionToolbarImpl> ourToolbars = new LinkedHashSet<>();
  private static final String RIGHT_ALIGN_KEY = "RIGHT_ALIGN";
  private static final String SECONDARY_SHORTCUT = "SecondaryActions.shortcut";

  //static {
  //  JBUIScale.addUserScaleChangeListener(__ -> {
  //    ((JBDimension)ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE).update();
  //    ((JBDimension)ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE).update();
  //  });
  //}

  public static void updateAllToolbarsImmediately() {
    for (ActionToolbarImpl toolbar : new ArrayList<>(ourToolbars)) {
      toolbar.updateActionsImmediately();
      for (Component c : toolbar.getComponents()) {
        if (c instanceof ActionButton) {
          ((ActionButton)c).updateToolTipText();
          ((ActionButton)c).updateIcon();
        }
      }
    }
  }

  private static final String SUPPRESS_ACTION_COMPONENT_WARNING = "ActionToolbarImpl.suppressCustomComponentWarning";
  private static final String SUPPRESS_TARGET_COMPONENT_WARNING = "ActionToolbarImpl.suppressTargetComponentWarning";

  /**
   * This array contains Rectangles which define bounds of the corresponding
   * components in the toolbar. This list can be consider as a cache of the
   * Rectangle objects that are used in calculation of preferred sizes and
   * components layout.
   */
  private final List<Rectangle> myComponentBounds = new ArrayList<>();

  private Size myMinimumButtonSize = Size.ZERO;

  /**
   * @see ActionToolbar#getLayoutPolicy()
   */
  private int myLayoutPolicy;
  private int myOrientation;
  private final ActionGroup myActionGroup;
  @Nonnull
  private final String myPlace;
  List<? extends AnAction> myVisibleActions;
  private final PresentationFactory myPresentationFactory = new PresentationFactory();
  private final boolean myDecorateButtons;

  private final ToolbarUpdater myUpdater;

  /**
   * @see ActionToolbar#adjustTheSameSize(boolean)
   */
  private boolean myAdjustTheSameSize;

  private final DataManager myDataManager;
  protected final ActionManagerEx myActionManager;

  private Rectangle myAutoPopupRec;

  private final DefaultActionGroup mySecondaryActions = new DefaultActionGroup();
  private PopupStateModifier mySecondaryButtonPopupStateModifier;
  private boolean myForceMinimumSize;
  private boolean myForceShowFirstComponent;
  private boolean mySkipWindowAdjustments;
  private boolean myMinimalMode;
  private boolean myForceUseMacEnhancements;

  private final Throwable myCreationTrace = new Throwable("toolbar creation trace");

  public ActionButton getSecondaryActionsButton() {
    return mySecondaryActionsButton;
  }

  private ActionButton mySecondaryActionsButton;

  private int myFirstOutsideIndex = -1;
  private JBPopup myPopup;

  private JComponent myTargetComponent;
  private boolean myReservePlaceAutoPopupIcon = true;
  private boolean myShowSeparatorTitles;
  private boolean myNoGapMode;//if true secondary actions button would be layout side-by-side with other buttons

  public ActionToolbarImpl(@Nonnull String place, @Nonnull final ActionGroup actionGroup, boolean horizontal) {
    this(place, actionGroup, horizontal, false, false);
  }

  public ActionToolbarImpl(@Nonnull String place, @Nonnull ActionGroup actionGroup, boolean horizontal, boolean decorateButtons) {
    this(place, actionGroup, horizontal, decorateButtons, false);
  }

  public ActionToolbarImpl(@Nonnull String place, @Nonnull ActionGroup actionGroup, final boolean horizontal, final boolean decorateButtons, boolean updateActionsNow) {
    super(null);
    myActionManager = ActionManagerEx.getInstanceEx();
    myPlace = place;
    myActionGroup = actionGroup;
    myVisibleActions = new ArrayList<>();
    myDataManager = DataManager.getInstance();
    myDecorateButtons = decorateButtons;
    myUpdater = new ToolbarUpdater(KeymapManagerEx.getInstanceEx(), this) {
      @Override
      protected void updateActionsImpl(boolean transparentOnly, boolean forced) {
        if (!ApplicationManager.getApplication().isDisposedOrDisposeInProgress()) {
          ActionToolbarImpl.this.updateActionsImpl(transparentOnly, forced);
        }
      }
    };

    setOrientation(horizontal ? ActionToolbar.HORIZONTAL_ORIENTATION : ActionToolbar.VERTICAL_ORIENTATION);

    mySecondaryActions.getTemplatePresentation().setIcon(AllIcons.General.GearPlain);
    mySecondaryActions.setPopup(true);

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
    ourToolbars.add(this);

    // should update action right on the showing, otherwise toolbar may not be displayed at all,
    // since by default all updates are postponed until frame gets focused.
    updateActionsImmediately();
  }

  public void setForceUseMacEnhancements(boolean useMacEnhancements) {
    myForceUseMacEnhancements = useMacEnhancements;
  }

  private boolean isInsideNavBar() {
    return ActionPlaces.NAVIGATION_BAR_TOOLBAR.equals(myPlace);
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    ourToolbars.remove(this);

    if (myPopup != null) {
      myPopup.cancel();
      myPopup = null;
    }

    CancellablePromise<List<AnAction>> lastUpdate = myLastUpdate;
    if (lastUpdate != null) {
      lastUpdate.cancel();
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        try {
          lastUpdate.blockingGet(1, TimeUnit.DAYS);
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
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

  @Override
  protected Graphics getComponentGraphics(Graphics graphics) {
    return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(graphics));
  }

  @Nonnull
  public ActionGroup getActionGroup() {
    return myActionGroup;
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);

    if (myLayoutPolicy == AUTO_LAYOUT_POLICY && myAutoPopupRec != null) {
      if (myOrientation == SwingConstants.HORIZONTAL) {
        final int dy = myAutoPopupRec.height / 2 - AllIcons.Ide.Link.getHeight() / 2;
        TargetAWT.to(AllIcons.Ide.Link).paintIcon(this, g, (int)myAutoPopupRec.getMaxX() - AllIcons.Ide.Link.getWidth() - 1, myAutoPopupRec.y + dy);
      }
      else {
        final int dx = myAutoPopupRec.width / 2 - AllIcons.Ide.Link.getWidth() / 2;
        TargetAWT.to(AllIcons.Ide.Link).paintIcon(this, g, myAutoPopupRec.x + dx, (int)myAutoPopupRec.getMaxY() - AllIcons.Ide.Link.getWidth() - 1);
      }
    }
  }

  public void setSecondaryButtonPopupStateModifier(@Nonnull PopupStateModifier popupStateModifier) {
    mySecondaryButtonPopupStateModifier = popupStateModifier;
  }

  private void fillToolBar(@Nonnull final List<? extends AnAction> actions, boolean layoutSecondaries) {
    boolean isLastElementSeparator = false;
    final List<AnAction> rightAligned = new ArrayList<>();
    for (int i = 0; i < actions.size(); i++) {
      AnAction action = actions.get(i);
      if (action instanceof RightAlignedToolbarAction) {
        rightAligned.add(action);
        continue;
      }

      if (layoutSecondaries) {
        if (!myActionGroup.isPrimary(action)) {
          mySecondaryActions.add(action);
          continue;
        }
      }

      if (action instanceof AnSeparator) {
        if (isLastElementSeparator) continue;
        if (i > 0 && i < actions.size() - 1) {
          add(SEPARATOR_CONSTRAINT, new MySeparator(myShowSeparatorTitles ? ((AnSeparator)action).getTextValue() : LocalizeValue.empty()));
          isLastElementSeparator = true;
          continue;
        }
      }
      else if (action instanceof CustomComponentAction) {
        add(CUSTOM_COMPONENT_CONSTRAINT, getCustomComponent(action));
      }
      else {
        add(ACTION_BUTTON_CONSTRAINT, createToolbarButton(action));
      }
      isLastElementSeparator = false;
    }

    if (mySecondaryActions.getChildrenCount() > 0) {
      mySecondaryActionsButton = new ActionButton(mySecondaryActions, myPresentationFactory.getPresentation(mySecondaryActions), myPlace, getMinimumButtonSize()) {
        @Override
        @ButtonState
        public int getPopState() {
          return mySecondaryButtonPopupStateModifier != null && mySecondaryButtonPopupStateModifier.willModify() ? mySecondaryButtonPopupStateModifier.getModifiedPopupState() : super.getPopState();
        }
      };
      mySecondaryActionsButton.setNoIconsInPopup(true);
      add(mySecondaryActionsButton);
    }

    for (AnAction action : rightAligned) {
      JComponent button = action instanceof CustomComponentAction ? getCustomComponent(action) : createToolbarButton(action);
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
      customComponent = ((CustomComponentAction)action).createCustomComponent(presentation, myPlace);
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
  }

  @Nonnull
  private Size getMinimumButtonSize() {
    return isInsideNavBar() ? NAVBAR_MINIMUM_BUTTON_SIZE : DEFAULT_MINIMUM_BUTTON_SIZE;
  }

  @Nonnull
  protected ActionButton createToolbarButton(@Nonnull AnAction action,
                                             boolean minimalMode,
                                             boolean decorateButtons,
                                             @Nonnull String place,
                                             @Nonnull Presentation presentation,
                                             @Nonnull Size minimumSize) {
    if (action.displayTextInToolbar()) {
      int mnemonic = KeyEvent.getExtendedKeyCodeForChar(TextWithMnemonic.parse(action.getTemplatePresentation().getTextWithMnemonic()).getMnemonic());

      ActionButtonWithText buttonWithText = new ActionButtonWithText(action, presentation, place, minimumSize);

      if (mnemonic != KeyEvent.VK_UNDEFINED) {
        buttonWithText.registerKeyboardAction(__ -> buttonWithText.click(), KeyStroke.getKeyStroke(mnemonic, InputEvent.ALT_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
      }
      tweakActionComponentUI(buttonWithText);
      return buttonWithText;
    }

    ActionButton actionButton = new ActionButton(action, presentation, place, minimumSize) {
      @Override
      protected DataContext getDataContext() {
        return getToolbarDataContext();
      }
    };
    actionButton.setMinimalMode(minimalMode);
    actionButton.setDecorateButtons(decorateButtons);
    return actionButton;
  }

  @Nonnull
  private ActionButton createToolbarButton(@Nonnull AnAction action) {
    return createToolbarButton(action, myMinimalMode, myDecorateButtons, myPlace, myPresentationFactory.getPresentation(action), myMinimumButtonSize);
  }

  @Override
  public void doLayout() {
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
      if (myOrientation == SwingConstants.HORIZONTAL) {
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
      if (myOrientation == SwingConstants.HORIZONTAL) {
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

    if (myOrientation == SwingConstants.HORIZONTAL) {
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
            if (eachComp == mySecondaryActionsButton) {
              assert isLast;
              if (sizeToFit.width != Integer.MAX_VALUE && !myNoGapMode) {
                eachBound.x = sizeToFit.width - insets.right - eachBound.width;
                eachX = (int)eachBound.getMaxX() - insets.left;
              }
              else {
                eachBound.x = insets.left + eachX;
              }
            }
            else {
              eachBound.x = insets.left + eachX;
              eachX += eachBound.width;
            }
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

    if (myAdjustTheSameSize) {
      final int maxWidth = getMaxButtonWidth();
      final int maxHeight = getMaxButtonHeight();
      int xOffset = 0;
      int yOffset = 0;
      if (myOrientation == SwingConstants.HORIZONTAL) {

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
      if (myOrientation == SwingConstants.HORIZONTAL) {
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
      if (component instanceof JComponent && ((JComponent)component).getClientProperty(RIGHT_ALIGN_KEY) == Boolean.TRUE) {
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
        if (component instanceof JComponent && ((JComponent)component).getClientProperty(RIGHT_ALIGN_KEY) == Boolean.TRUE) {
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
    final ArrayList<Rectangle> bounds = new ArrayList<>();
    calculateBounds(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE), bounds);//it doesn't take into account wrapping
    if (bounds.isEmpty()) return JBUI.emptySize();
    int forcedHeight = 0;
    if (getWidth() > 0 && getLayoutPolicy() == ActionToolbar.WRAP_LAYOUT_POLICY && myOrientation == SwingConstants.HORIZONTAL) {
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
      if (each.x == Integer.MAX_VALUE) continue;
      xLeft = Math.min(xLeft, each.x);
      yTop = Math.min(yTop, each.y);
      xRight = Math.max(xRight, each.x + each.width);
      yBottom = Math.max(yBottom, each.y + each.height);
    }
    final Dimension dimension = new Dimension(xRight - xLeft, Math.max(yBottom - yTop, forcedHeight));

    if (myLayoutPolicy == AUTO_LAYOUT_POLICY && myReservePlaceAutoPopupIcon && !isInsideNavBar()) {
      if (myOrientation == SwingConstants.HORIZONTAL) {
        dimension.width += AllIcons.Ide.Link.getWidth();
      }
      else {
        dimension.height += AllIcons.Ide.Link.getHeight();
      }
    }

    JBInsets.addTo(dimension, getInsets());

    return dimension;
  }

  /**
   * Forces the minimum size of the toolbar to show all buttons, When set to {@code true}. By default ({@code false}) the
   * toolbar will shrink further and show the auto popup chevron button.
   */
  public void setForceMinimumSize(boolean force) {
    myForceMinimumSize = force;
  }

  /**
   * By default minimum size is to show chevron only.
   * If this option is {@code true} toolbar shows at least one (the first) component plus chevron (if need)
   */
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
    if (myForceMinimumSize) {
      return getPreferredSize();
    }
    if (myLayoutPolicy == AUTO_LAYOUT_POLICY) {
      Dimension minSize = TargetAWT.to(myMinimumButtonSize);

      final Insets i = getInsets();
      if (myForceShowFirstComponent && getComponentCount() > 0) {
        Component c = getComponent(0);
        Dimension firstSize = c.getPreferredSize();
        if (myOrientation == SwingConstants.HORIZONTAL) {
          return new Dimension(firstSize.width + AllIcons.Ide.Link.getWidth() + i.left + i.right, Math.max(firstSize.height, minSize.height) + i.top + i.bottom);
        }
        else {
          return new Dimension(Math.max(firstSize.width, AllIcons.Ide.Link.getWidth()) + i.left + i.right, firstSize.height + minSize.height + i.top + i.bottom);
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

      if (myOrientation == SwingConstants.HORIZONTAL) {
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
      if (getParent() == null) return;

      int gap = JBUIScale.scale(2);
      int center = JBUIScale.scale(3);
      int offset;
      if (myOrientation == SwingConstants.HORIZONTAL) {
        offset = ActionToolbarImpl.this.getHeight() - getMaxButtonHeight() - 1;
      }
      else {
        offset = ActionToolbarImpl.this.getWidth() - getMaxButtonWidth() - 1;
      }

      g.setColor(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground());
      if (myOrientation == SwingConstants.HORIZONTAL) {
        int y2 = ActionToolbarImpl.this.getHeight() - gap * 2 - offset;
        LinePainter2D.paint((Graphics2D)g, center, gap, center, y2);

        if (myTextValue != LocalizeValue.empty()) {
          FontMetrics fontMetrics = getFontMetrics(getFont());
          int top = (getHeight() - fontMetrics.getHeight()) / 2;
          UISettings.setupAntialiasing(g);
          g.setColor(JBColor.foreground());
          g.drawString(myTextValue.getValue(), gap * 2 + center + gap, top + fontMetrics.getAscent());
        }
      }
      else {
        LinePainter2D.paint((Graphics2D)g, gap, center, ActionToolbarImpl.this.getWidth() - gap * 2 - offset, center);
      }
    }

    private int getTextWidth(@Nonnull FontMetrics fontMetrics, @Nonnull String text, @Nullable Graphics graphics) {
      if (graphics == null) {
        return fontMetrics.stringWidth(text);
      }
      else {
        Graphics g = graphics.create();
        try {
          UISettings.setupAntialiasing(g);
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
      if (component instanceof ActionButton) {
        final ActionButton button = (ActionButton)component;
        button.setMinimumButtonSize(size);
      }
    }
    revalidate();
  }

  @Override
  public void setOrientation(@MagicConstant(intValues = {HORIZONTAL_ORIENTATION, VERTICAL_ORIENTATION}) int orientation) {
    if (HORIZONTAL_ORIENTATION != orientation && VERTICAL_ORIENTATION != orientation) {
      throw new IllegalArgumentException("wrong orientation: " + orientation);
    }
    myOrientation = orientation;
  }

  @MagicConstant(intValues = {HORIZONTAL_ORIENTATION, VERTICAL_ORIENTATION})
  public int getOrientation() {
    return myOrientation;
  }

  @RequiredUIAccess
  @Override
  public void updateActionsImmediately() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myUpdater.updateActions(true, false);
  }

  private boolean myAlreadyUpdated;

  private void updateActionsImpl(boolean transparentOnly, boolean forced) {
    DataContext dataContext = getDataContext();
    boolean async = myAlreadyUpdated && Registry.is("actionSystem.update.actions.asynchronously") && ourToolbars.contains(this) && isShowing();
    ActionUpdater updater =
            new ActionUpdater(LaterInvocator.isInModalContext(), myPresentationFactory, async ? DataManager.getInstance().createAsyncDataContext(dataContext) : dataContext, myPlace, false, true);
    if (async) {
      if (myLastUpdate != null) myLastUpdate.cancel();

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
      mySecondaryActions.removeAll();
      mySecondaryActionsButton = null;
      fillToolBar(myVisibleActions, getLayoutPolicy() == AUTO_LAYOUT_POLICY && myOrientation == SwingConstants.HORIZONTAL);

      Dimension newSize = getPreferredSize();

      if (!mySkipWindowAdjustments) {
        ((WindowManagerEx)WindowManager.getInstance()).adjustContainerWindow(this, oldSize, newSize);
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
    if (myTargetComponent == null && getClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING) == null && !ApplicationManager.getApplication().isUnitTestMode()) {
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
      IdeFocusManager.getInstance(null).doWhenFocusSettlesDown(() -> showAutoPopup());
    }
  }

  private void showAutoPopup() {
    if (isPopupShowing()) return;

    final ActionGroup group;
    if (myOrientation == SwingConstants.HORIZONTAL) {
      group = myActionGroup;
    }
    else {
      final DefaultActionGroup outside = new DefaultActionGroup();
      for (int i = myFirstOutsideIndex; i < myVisibleActions.size(); i++) {
        outside.add(myVisibleActions.get(i));
      }
      group = outside;
    }

    PopupToolbar popupToolbar = new PopupToolbar(myPlace, group, true, this) {
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
    if (myOrientation == SwingConstants.HORIZONTAL) {
      location = getLocationOnScreen();
    }
    else {
      location = getLocationOnScreen();
      location.y = location.y + getHeight() - popupToolbar.getPreferredSize().height;
    }


    final ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(popupToolbar, null);
    builder.setResizable(false).setMovable(true) // fit the screen automatically
            .setRequestFocus(false).setTitle(null).setCancelOnClickOutside(true).setCancelOnOtherWindowOpen(true).setCancelCallback(() -> {
      final boolean toClose = myActionManager.isActionPopupStackEmpty();
      if (toClose) {
        myUpdater.updateActions(false, true);
      }
      return toClose;
    }).setCancelOnMouseOutCallback(event -> myAutoPopupRec != null && myActionManager.isActionPopupStackEmpty() && !new RelativeRectangle(this, myAutoPopupRec).contains(new RelativePoint(event)));

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
    if (myPopup == null) return;
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

    PopupToolbar(@Nonnull String place, @Nonnull ActionGroup actionGroup, final boolean horizontal, @Nonnull JComponent parent) {
      super(place, actionGroup, horizontal, false, true);
      ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AnActionListener.TOPIC, this);
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

  @Override
  public void setReservePlaceAutoPopupIcon(final boolean reserve) {
    myReservePlaceAutoPopupIcon = reserve;
  }

  @Override
  public void setSecondaryActionsTooltip(@Nonnull String secondaryActionsTooltip) {
    mySecondaryActions.getTemplatePresentation().setDescription(secondaryActionsTooltip);
  }

  @Override
  public void setSecondaryActionsIcon(Image icon) {
    setSecondaryActionsIcon(icon, false);
  }

  @Override
  public void setSecondaryActionsIcon(Image icon, boolean hideDropdownIcon) {
    Presentation presentation = mySecondaryActions.getTemplatePresentation();
    presentation.setIcon(icon);
    presentation.putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, hideDropdownIcon ? Boolean.TRUE : null);
  }

  public void setSecondaryActionsShortcut(@Nonnull String secondaryActionsShortcut) {
    mySecondaryActions.getTemplatePresentation().putClientProperty(SECONDARY_SHORTCUT, secondaryActionsShortcut);
  }

  public void setNoGapMode() {
    myNoGapMode = true;
  }

  @Nonnull
  @Override
  public List<AnAction> getActions(boolean originalProvider) {
    return getActions();
  }

  @Nonnull
  @Override
  public List<AnAction> getActions() {
    ArrayList<AnAction> result = new ArrayList<>();

    ArrayList<AnAction> secondary = new ArrayList<>();
    AnAction[] kids = myActionGroup.getChildren(null);
    for (AnAction each : kids) {
      if (myActionGroup.isPrimary(each)) {
        result.add(each);
      }
      else {
        secondary.add(each);
      }
    }
    result.add(new AnSeparator());
    result.addAll(secondary);

    return result;
  }

  @Override
  public void setMiniMode(boolean minimalMode) {
    //if (myMinimalMode == minimalMode) return;

    myMinimalMode = minimalMode;
    if (myMinimalMode) {
      setMinimumButtonSize(Size.ZERO);
      setLayoutPolicy(NOWRAP_LAYOUT_POLICY);
      setBorder(JBUI.Borders.empty());
      setOpaque(false);
    }
    else {
      setBorder(JBUI.Borders.empty(2));
      setMinimumButtonSize(myDecorateButtons ? new Size(30, 20) : DEFAULT_MINIMUM_BUTTON_SIZE);
      setOpaque(true);
      setLayoutPolicy(AUTO_LAYOUT_POLICY);
    }

    myUpdater.updateActions(false, true);
  }

  @TestOnly
  public Presentation getPresentation(AnAction action) {
    return myPresentationFactory.getPresentation(action);
  }

  public void clearPresentationCache() {
    myPresentationFactory.reset();
  }

  public interface PopupStateModifier {
    @ActionButtonComponent.ButtonState
    int getModifiedPopupState();

    boolean willModify();
  }
}
