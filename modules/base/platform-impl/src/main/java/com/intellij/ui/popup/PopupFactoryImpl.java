// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.popup;

import com.intellij.CommonBundle;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.ActionMenu;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.text.TextWithMnemonic;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.HintHint;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.ui.popup.mock.MockConfirmation;
import com.intellij.ui.popup.tree.TreePopupImpl;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Singleton
public class PopupFactoryImpl extends JBPopupFactory {

  /**
   * Allows to get an editor position for which a popup with auxiliary information might be shown.
   * <p/>
   * Primary intention for this key is to hint popup position for the non-caret location.
   */
  public static final Key<VisualPosition> ANCHOR_POPUP_POSITION = Key.create("popup.anchor.position");

  private static final Logger LOG = Logger.getInstance(PopupFactoryImpl.class);

  private final Map<Disposable, List<Balloon>> myStorage = ContainerUtil.createWeakMap();

  @Nonnull
  @Override
  public <T> IPopupChooserBuilder<T> createPopupChooserBuilder(@Nonnull List<? extends T> list) {
    return new PopupChooserBuilder<>(new JBList<>(new CollectionListModel<>(list)));
  }

  @Nonnull
  @Override
  public ListPopup createConfirmation(String title, final Runnable onYes, int defaultOptionIndex) {
    return createConfirmation(title, CommonBundle.getYesButtonText(), CommonBundle.getNoButtonText(), onYes, defaultOptionIndex);
  }

  @Nonnull
  @Override
  public ListPopup createConfirmation(String title, final String yesText, String noText, final Runnable onYes, int defaultOptionIndex) {
    return createConfirmation(title, yesText, noText, onYes, EmptyRunnable.getInstance(), defaultOptionIndex);
  }

  @Nonnull
  @Override
  public JBPopup createMessage(String text) {
    return createListPopup(new BaseListPopupStep<>(null, text));
  }

  @Override
  public Balloon getParentBalloonFor(@Nullable Component c) {
    if (c == null) return null;
    Component eachParent = c;
    while (eachParent != null) {
      if (eachParent instanceof JComponent) {
        Object balloon = ((JComponent)eachParent).getClientProperty(Balloon.KEY);
        if (balloon instanceof Balloon) {
          return (Balloon)balloon;
        }
      }
      eachParent = eachParent.getParent();
    }

    return null;
  }

  @Override
  protected PopupChooserBuilder.PopupComponentAdapter createPopupComponentAdapter(PopupChooserBuilder builder, JList list) {
    return new PopupListAdapter(builder, list);
  }

  @Override
  protected PopupChooserBuilder.PopupComponentAdapter createPopupComponentAdapter(PopupChooserBuilder builder, JTree tree) {
    return new PopupTreeAdapter(builder, tree);
  }

  @Override
  protected PopupChooserBuilder.PopupComponentAdapter createPopupComponentAdapter(PopupChooserBuilder builder, JTable table) {
    return new PopupTableAdapter(builder, table);
  }

  @Nonnull
  @Override
  public ListPopup createConfirmation(String title, final String yesText, String noText, final Runnable onYes, final Runnable onNo, int defaultOptionIndex) {
    final BaseListPopupStep<String> step = new BaseListPopupStep<String>(title, yesText, noText) {
      @Override
      public PopupStep onChosen(String selectedValue, final boolean finalChoice) {
        return doFinalStep(selectedValue.equals(yesText) ? onYes : onNo);
      }

      @Override
      public void canceled() {
        onNo.run();
      }

      @Override
      public boolean isMnemonicsNavigationEnabled() {
        return true;
      }
    };
    step.setDefaultOptionIndex(defaultOptionIndex);

    final Application app = ApplicationManager.getApplication();
    return app == null || !app.isUnitTestMode() ? new ListPopupImpl(step) : new MockConfirmation(step, yesText);
  }


  public static class ActionGroupPopup extends ListPopupImpl {

    private final Runnable myDisposeCallback;
    private final Component myComponent;
    private final String myActionPlace;

    public ActionGroupPopup(final String title,
                            @Nonnull ActionGroup actionGroup,
                            @Nonnull DataContext dataContext,
                            boolean showNumbers,
                            boolean useAlphaAsNumbers,
                            boolean showDisabledActions,
                            boolean honorActionMnemonics,
                            final Runnable disposeCallback,
                            final int maxRowCount,
                            final Condition<? super AnAction> preselectActionCondition,
                            @Nullable final String actionPlace) {
      this(title, actionGroup, dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics, disposeCallback, maxRowCount, preselectActionCondition, actionPlace, null, false);
    }

    public ActionGroupPopup(String title,
                            @Nonnull ActionGroup actionGroup,
                            @Nonnull DataContext dataContext,
                            boolean showNumbers,
                            boolean useAlphaAsNumbers,
                            boolean showDisabledActions,
                            boolean honorActionMnemonics,
                            Runnable disposeCallback,
                            int maxRowCount,
                            Condition<? super AnAction> preselectActionCondition,
                            @Nullable final String actionPlace,
                            boolean autoSelection) {
      this(title, actionGroup, dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics, disposeCallback, maxRowCount, preselectActionCondition, actionPlace, null,
           autoSelection);
    }

    public ActionGroupPopup(String title,
                            @Nonnull ActionGroup actionGroup,
                            @Nonnull DataContext dataContext,
                            boolean showNumbers,
                            boolean useAlphaAsNumbers,
                            boolean showDisabledActions,
                            boolean honorActionMnemonics,
                            Runnable disposeCallback,
                            int maxRowCount,
                            Condition<? super AnAction> preselectActionCondition,
                            @Nullable final String actionPlace,
                            @Nullable PresentationFactory presentationFactory,
                            boolean autoSelection) {
      this(null, createStep(title, actionGroup, dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics, preselectActionCondition, actionPlace, presentationFactory,
                            autoSelection), disposeCallback, dataContext, actionPlace, maxRowCount);
    }

    protected ActionGroupPopup(@Nullable WizardPopup aParent,
                               @Nonnull ListPopupStep step,
                               @Nullable Runnable disposeCallback,
                               @Nonnull DataContext dataContext,
                               @Nullable String actionPlace,
                               int maxRowCount) {
      super(dataContext.getData(CommonDataKeys.PROJECT), aParent, step, null);
      setMaxRowCount(maxRowCount);
      myDisposeCallback = disposeCallback;
      myComponent = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      myActionPlace = actionPlace == null ? ActionPlaces.UNKNOWN : actionPlace;

      registerAction("handleActionToggle1", KeyEvent.VK_SPACE, 0, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          handleToggleAction();
        }
      });

      addListSelectionListener(e -> {
        final JList list = (JList)e.getSource();
        final ActionItem actionItem = (ActionItem)list.getSelectedValue();
        if (actionItem == null) return;
        Presentation presentation = updateActionItem(actionItem);
        ActionMenu.showDescriptionInStatusBar(true, myComponent, presentation.getDescription());
      });
    }

    @Nonnull
    private Presentation updateActionItem(@Nonnull ActionItem actionItem) {
      AnAction action = actionItem.getAction();
      Presentation presentation = new Presentation();
      presentation.setDescription(action.getTemplatePresentation().getDescription());

      final AnActionEvent actionEvent = new AnActionEvent(null, DataManager.getInstance().getDataContext(myComponent), myActionPlace, presentation, ActionManager.getInstance(), 0);
      actionEvent.setInjectedContext(action.isInInjectedContext());
      ActionUtil.performDumbAwareUpdate(LaterInvocator.isInModalContext(), action, actionEvent, false);
      return presentation;
    }

    private static ListPopupStep<ActionItem> createStep(String title,
                                                        @Nonnull ActionGroup actionGroup,
                                                        @Nonnull DataContext dataContext,
                                                        boolean showNumbers,
                                                        boolean useAlphaAsNumbers,
                                                        boolean showDisabledActions,
                                                        boolean honorActionMnemonics,
                                                        Condition<? super AnAction> preselectActionCondition,
                                                        @Nullable String actionPlace,
                                                        @Nullable PresentationFactory presentationFactory,
                                                        boolean autoSelection) {
      final Component component = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      LOG.assertTrue(component != null, "dataContext has no component for new ListPopupStep");

      List<ActionItem> items = ActionPopupStep.createActionItems(actionGroup, dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics, actionPlace, presentationFactory);

      return new ActionPopupStep(items, title, getComponentContextSupplier(component), actionPlace, showNumbers || honorActionMnemonics && itemsHaveMnemonics(items), preselectActionCondition,
                                 autoSelection, showDisabledActions, presentationFactory);
    }

    /**
     * @deprecated Use {@link ActionPopupStep#createActionItems(ActionGroup, DataContext, boolean, boolean, boolean, boolean, String, PresentationFactory)} instead.
     */
    @Deprecated
    @Nonnull
    public static List<ActionItem> getActionItems(@Nonnull ActionGroup actionGroup,
                                                  @Nonnull DataContext dataContext,
                                                  boolean showNumbers,
                                                  boolean useAlphaAsNumbers,
                                                  boolean showDisabledActions,
                                                  boolean honorActionMnemonics,
                                                  @Nullable String actionPlace) {
      return ActionPopupStep.createActionItems(actionGroup, dataContext, showNumbers, useAlphaAsNumbers, showDisabledActions, honorActionMnemonics, actionPlace, null);
    }

    @Override
    public void dispose() {
      if (myDisposeCallback != null) {
        myDisposeCallback.run();
      }
      ActionMenu.showDescriptionInStatusBar(true, myComponent, null);
      super.dispose();
    }

    @Override
    public void handleSelect(boolean handleFinalChoices, InputEvent e) {
      final Object selectedValue = getList().getSelectedValue();
      final ActionPopupStep actionPopupStep = ObjectUtils.tryCast(getListStep(), ActionPopupStep.class);

      if (actionPopupStep != null) {
        KeepingPopupOpenAction dontClosePopupAction = getActionByClass(selectedValue, actionPopupStep, KeepingPopupOpenAction.class);
        if (dontClosePopupAction != null) {
          actionPopupStep.performAction((AnAction)dontClosePopupAction, e != null ? e.getModifiers() : 0, e);
          for (ActionItem item : actionPopupStep.getValues()) {
            updateActionItem(item);
          }
          getList().repaint();
          return;
        }
      }

      super.handleSelect(handleFinalChoices, e);
    }

    protected void handleToggleAction() {
      final Object[] selectedValues = getList().getSelectedValues();

      ListPopupStep<Object> listStep = getListStep();
      final ActionPopupStep actionPopupStep = ObjectUtils.tryCast(listStep, ActionPopupStep.class);
      if (actionPopupStep == null) return;

      List<ToggleAction> filtered = ContainerUtil.mapNotNull(selectedValues, o -> getActionByClass(o, actionPopupStep, ToggleAction.class));

      for (ToggleAction action : filtered) {
        actionPopupStep.performAction(action, 0);
      }

      for (ActionItem item : actionPopupStep.getValues()) {
        updateActionItem(item);
      }

      getList().repaint();
    }

    @Nullable
    private static <T> T getActionByClass(@Nullable Object value, @Nonnull ActionPopupStep actionPopupStep, @Nonnull Class<T> actionClass) {
      ActionItem item = value instanceof ActionItem ? (ActionItem)value : null;
      if (item == null) return null;
      if (!actionPopupStep.isSelectable(item)) return null;
      return actionClass.isInstance(item.getAction()) ? actionClass.cast(item.getAction()) : null;
    }
  }

  @Nonnull
  private static Supplier<DataContext> getComponentContextSupplier(Component component) {
    return () -> DataManager.getInstance().getDataContext(component);
  }

  @Override
  @Nonnull
  public ListPopup createActionGroupPopup(String title,
                                          @Nonnull ActionGroup actionGroup,
                                          @Nonnull DataContext dataContext,
                                          ActionSelectionAid aid,
                                          boolean showDisabledActions,
                                          Runnable disposeCallback,
                                          int maxRowCount,
                                          Condition<? super AnAction> preselectActionCondition,
                                          @Nullable String actionPlace) {
    return new ActionGroupPopup(title, actionGroup, dataContext, aid == ActionSelectionAid.ALPHA_NUMBERING || aid == ActionSelectionAid.NUMBERING, aid == ActionSelectionAid.ALPHA_NUMBERING,
                                showDisabledActions, aid == ActionSelectionAid.MNEMONICS, disposeCallback, maxRowCount, preselectActionCondition, actionPlace);
  }

  @Nonnull
  @Override
  public ListPopup createActionGroupPopup(final String title,
                                          @Nonnull final ActionGroup actionGroup,
                                          @Nonnull DataContext dataContext,
                                          boolean showNumbers,
                                          boolean showDisabledActions,
                                          boolean honorActionMnemonics,
                                          final Runnable disposeCallback,
                                          final int maxRowCount,
                                          final Condition<? super AnAction> preselectActionCondition) {
    return new ActionGroupPopup(title, actionGroup, dataContext, showNumbers, true, showDisabledActions, honorActionMnemonics, disposeCallback, maxRowCount, preselectActionCondition, null);
  }

  @Nonnull
  @Override
  public ListPopupStep<ActionItem> createActionsStep(@Nonnull ActionGroup actionGroup,
                                                     @Nonnull DataContext dataContext,
                                                     @Nullable String actionPlace,
                                                     boolean showNumbers,
                                                     boolean showDisabledActions,
                                                     String title,
                                                     Component component,
                                                     boolean honorActionMnemonics,
                                                     int defaultOptionIndex,
                                                     boolean autoSelectionEnabled) {
    return ActionPopupStep
            .createActionsStep(actionGroup, dataContext, showNumbers, true, showDisabledActions, title, honorActionMnemonics, autoSelectionEnabled, getComponentContextSupplier(component), actionPlace,
                               null, defaultOptionIndex, null);
  }

  private static boolean itemsHaveMnemonics(final List<? extends ActionItem> items) {
    for (ActionItem item : items) {
      if (TextWithMnemonic.parse(item.getAction().getTemplatePresentation().getTextWithMnemonic()).getMnemonic() != 0) return true;
    }

    return false;
  }

  @Nonnull
  @Override
  public ListPopup createListPopup(@Nonnull ListPopupStep step) {
    return new ListPopupImpl(step);
  }

  @Nonnull
  @Override
  public ListPopup createListPopup(@Nonnull ListPopupStep step, int maxRowCount) {
    ListPopupImpl popup = new ListPopupImpl(step);
    popup.setMaxRowCount(maxRowCount);
    return popup;
  }

  @Nonnull
  @Override
  public TreePopup createTree(JBPopup parent, @Nonnull TreePopupStep aStep, Object parentValue) {
    return new TreePopupImpl(aStep.getProject(), parent, aStep, parentValue);
  }

  @Nonnull
  @Override
  public TreePopup createTree(@Nonnull TreePopupStep aStep) {
    return new TreePopupImpl(aStep.getProject(), null, aStep, null);
  }

  @Nonnull
  @Override
  public ComponentPopupBuilder createComponentPopupBuilder(@Nonnull JComponent content, JComponent preferableFocusComponent) {
    return new ComponentPopupBuilderImpl(content, preferableFocusComponent);
  }


  @Nonnull
  @Override
  public RelativePoint guessBestPopupLocation(@Nonnull DataContext dataContext) {
    Component component = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    JComponent focusOwner = component instanceof JComponent ? (JComponent)component : null;

    if (focusOwner == null) {
      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      JFrame frame = project == null ? null : WindowManager.getInstance().getFrame(project);
      focusOwner = frame == null ? null : frame.getRootPane();
      if (focusOwner == null) {
        throw new IllegalArgumentException("focusOwner cannot be null");
      }
    }

    final Point point = dataContext.getData(PlatformDataKeys.CONTEXT_MENU_POINT);
    if (point != null) {
      return new RelativePoint(focusOwner, point);
    }

    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (editor != null && focusOwner == editor.getContentComponent()) {
      return guessBestPopupLocation(editor);
    }
    return guessBestPopupLocation(focusOwner);
  }

  @Nonnull
  @Override
  public RelativePoint guessBestPopupLocation(@Nonnull final JComponent component) {
    Point popupMenuPoint = null;
    final Rectangle visibleRect = component.getVisibleRect();
    if (component instanceof JList) { // JList
      JList list = (JList)component;
      int firstVisibleIndex = list.getFirstVisibleIndex();
      int lastVisibleIndex = list.getLastVisibleIndex();
      int[] selectedIndices = list.getSelectedIndices();
      for (int index : selectedIndices) {
        if (firstVisibleIndex <= index && index <= lastVisibleIndex) {
          Rectangle cellBounds = list.getCellBounds(index, index);
          popupMenuPoint = new Point(visibleRect.x + visibleRect.width / 4, cellBounds.y + cellBounds.height - 1);
          break;
        }
      }
    }
    else if (component instanceof JTree) { // JTree
      JTree tree = (JTree)component;
      int[] selectionRows = tree.getSelectionRows();
      if (selectionRows != null) {
        Arrays.sort(selectionRows);
        for (int row : selectionRows) {
          Rectangle rowBounds = tree.getRowBounds(row);
          if (visibleRect.contains(rowBounds)) {
            popupMenuPoint = new Point(rowBounds.x + 2, rowBounds.y + rowBounds.height - 1);
            break;
          }
        }
        if (popupMenuPoint == null) {//All selected rows are out of visible rect
          Point visibleCenter = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
          double minDistance = Double.POSITIVE_INFINITY;
          int bestRow = -1;
          for (int row : selectionRows) {
            Rectangle rowBounds = tree.getRowBounds(row);
            Point rowCenter = new Point(rowBounds.x + rowBounds.width / 2, rowBounds.y + rowBounds.height / 2);
            double distance = visibleCenter.distance(rowCenter);
            if (minDistance > distance) {
              minDistance = distance;
              bestRow = row;
            }
          }

          if (bestRow != -1) {
            Rectangle rowBounds = tree.getRowBounds(bestRow);
            tree.scrollRectToVisible(new Rectangle(rowBounds.x, rowBounds.y, Math.min(visibleRect.width, rowBounds.width), rowBounds.height));
            popupMenuPoint = new Point(rowBounds.x + 2, rowBounds.y + rowBounds.height - 1);
          }
        }
      }
    }
    else if (component instanceof JTable) {
      JTable table = (JTable)component;
      int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
      int row = Math.max(table.getSelectionModel().getLeadSelectionIndex(), table.getSelectionModel().getAnchorSelectionIndex());
      Rectangle rect = table.getCellRect(row, column, false);
      if (!visibleRect.intersects(rect)) {
        table.scrollRectToVisible(rect);
      }
      popupMenuPoint = new Point(rect.x, rect.y + rect.height - 1);
    }
    else if (component instanceof PopupOwner) {
      popupMenuPoint = ((PopupOwner)component).getBestPopupPosition();
    }
    if (popupMenuPoint == null) {
      popupMenuPoint = new Point(visibleRect.x + visibleRect.width / 2, visibleRect.y + visibleRect.height / 2);
    }

    return new RelativePoint(component, popupMenuPoint);
  }

  @Override
  public boolean isBestPopupLocationVisible(@Nonnull Editor editor) {
    return getVisibleBestPopupLocation(editor) != null;
  }

  @Nonnull
  @Override
  public RelativePoint guessBestPopupLocation(@Nonnull Editor editor) {
    Point p = getVisibleBestPopupLocation(editor);
    if (p == null) {
      final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
      p = new Point(visibleArea.x + visibleArea.width / 3, visibleArea.y + visibleArea.height / 2);
    }
    return new RelativePoint(editor.getContentComponent(), p);
  }

  @Nullable
  private static Point getVisibleBestPopupLocation(@Nonnull Editor editor) {
    VisualPosition visualPosition = editor.getUserData(ANCHOR_POPUP_POSITION);

    if (visualPosition == null) {
      CaretModel caretModel = editor.getCaretModel();
      if (caretModel.isUpToDate()) {
        visualPosition = caretModel.getVisualPosition();
      }
      else {
        visualPosition = editor.offsetToVisualPosition(caretModel.getOffset());
      }
    }

    final int lineHeight = editor.getLineHeight();
    Point p = editor.visualPositionToXY(visualPosition);
    p.y += lineHeight;

    final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    return !visibleArea.contains(p) && !visibleArea.contains(p.x, p.y - lineHeight) ? null : p;
  }

  @Override
  public Point getCenterOf(JComponent container, JComponent content) {
    return AbstractPopup.getCenterOf(container, content);
  }

  @Override
  @Nonnull
  public List<JBPopup> getChildPopups(@Nonnull final Component component) {
    return AbstractPopup.getChildPopups(component);
  }

  @Override
  public boolean isPopupActive() {
    return IdeEventQueue.getInstance().isPopupActive();
  }

  @Nonnull
  @Override
  public BalloonBuilder createBalloonBuilder(@Nonnull final JComponent content) {
    return new BalloonPopupBuilderImpl(myStorage, content);
  }

  @Nonnull
  @Override
  public BalloonBuilder createDialogBalloonBuilder(@Nonnull JComponent content, String title) {
    final BalloonPopupBuilderImpl builder = new BalloonPopupBuilderImpl(myStorage, content);
    final Color bg = UIManager.getColor("Panel.background");
    final Color borderOriginal = Color.darkGray;
    final Color border = ColorUtil.toAlpha(borderOriginal, 75);
    builder.setDialogMode(true).setTitle(title).setAnimationCycle(200).setFillColor(bg).setBorderColor(border).setHideOnClickOutside(false).setHideOnKeyOutside(false).setHideOnAction(false)
            .setCloseButtonEnabled(true).setShadow(true);

    return builder;
  }

  @Nonnull
  @Override
  public BalloonBuilder createHtmlTextBalloonBuilder(@Nonnull final String htmlContent, @Nullable final Image icon, Color textColor, final Color fillColor, @Nullable final HyperlinkListener listener) {
    JEditorPane text = IdeTooltipManager.initPane(htmlContent, new HintHint().setTextFg(textColor).setAwtTooltip(true), null);

    if (listener != null) {
      text.addHyperlinkListener(listener);
    }
    text.setEditable(false);
    NonOpaquePanel.setTransparent(text);
    text.setBorder(null);


    JLabel label = new JLabel();
    final JPanel content = new NonOpaquePanel(new BorderLayout((int)(label.getIconTextGap() * 1.5), (int)(label.getIconTextGap() * 1.5)));

    final NonOpaquePanel textWrapper = new NonOpaquePanel(new GridBagLayout());
    JScrollPane scrolledText = ScrollPaneFactory.createScrollPane(text, true);
    scrolledText.setBackground(fillColor);
    scrolledText.getViewport().setBackground(fillColor);
    textWrapper.add(scrolledText);
    content.add(textWrapper, BorderLayout.CENTER);
    if (icon != null) {
      final NonOpaquePanel north = new NonOpaquePanel(new BorderLayout());
      north.add(new JBLabel(icon), BorderLayout.NORTH);
      content.add(north, BorderLayout.WEST);
    }

    content.setBorder(JBUI.Borders.empty(2, 4));

    final BalloonBuilder builder = createBalloonBuilder(content);

    builder.setFillColor(fillColor);

    return builder;
  }

  @Nonnull
  @Override
  public BalloonBuilder createHtmlTextBalloonBuilder(@Nonnull String htmlContent, MessageType messageType, @Nullable HyperlinkListener listener) {
    return createHtmlTextBalloonBuilder(htmlContent, messageType.getDefaultIcon(), messageType.getPopupBackground(), listener);
  }


  public static class ActionItem implements ShortcutProvider {
    private final AnAction myAction;
    private LocalizeValue myTextValue;
    private final boolean myIsEnabled;
    private final Image myIcon;
    private final Image mySelectedIcon;
    private final boolean myPrependWithSeparator;
    private final String mySeparatorText;
    private final String myDescription;

    ActionItem(@Nonnull AnAction action,
               @Nonnull LocalizeValue textValue,
               @Nullable String description,
               boolean enabled,
               @Nullable Image icon,
               @Nullable Image selectedIcon,
               final boolean prependWithSeparator,
               String separatorText) {
      myAction = action;
      myTextValue = textValue;
      myIsEnabled = enabled;
      myIcon = icon;
      mySelectedIcon = selectedIcon;
      myPrependWithSeparator = prependWithSeparator;
      mySeparatorText = separatorText;
      myDescription = description;
      myAction.getTemplatePresentation().addPropertyChangeListener(evt -> {
        if (evt.getPropertyName().equals(Presentation.PROP_TEXT)) {
          myTextValue = myAction.getTemplatePresentation().getTextValue();
        }
      });
    }

    @Nonnull
    public AnAction getAction() {
      return myAction;
    }

    @Nonnull
    public LocalizeValue getText() {
      return myTextValue;
    }

    @Nullable
    public Image getIcon(boolean selected) {
      return selected && mySelectedIcon != null ? mySelectedIcon : myIcon;
    }

    public boolean isPrependWithSeparator() {
      return myPrependWithSeparator;
    }

    public String getSeparatorText() {
      return mySeparatorText;
    }

    public boolean isEnabled() {
      return myIsEnabled;
    }

    public String getDescription() {
      return myDescription;
    }

    @Nullable
    @Override
    public ShortcutSet getShortcut() {
      return myAction.getShortcutSet();
    }

    @Override
    public String toString() {
      return myTextValue.getValue();
    }
  }
}
