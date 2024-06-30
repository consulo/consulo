// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.util.gotoByName;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.HelpManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.application.impl.internal.progress.ReadTask;
import consulo.application.internal.TooManyUsagesStatus;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.Patches;
import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.MatcherHolder;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.*;
import consulo.disposer.Disposer;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.ide.impl.idea.ide.actions.CopyReferenceAction;
import consulo.ide.impl.idea.ide.actions.GotoFileAction;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeManagerEx;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.PopupOwner;
import consulo.ide.impl.idea.ui.popup.PopupPositionManager;
import consulo.ide.impl.idea.ui.popup.PopupUpdateProcessor;
import consulo.ide.impl.idea.usages.UsageLimitUtil;
import consulo.ide.impl.idea.usages.impl.UsageViewManagerImpl;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.psi.statistics.StatisticsInfo;
import consulo.ide.impl.psi.statistics.StatisticsManager;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.ui.awt.EditorAWTUtil;
import consulo.language.impl.internal.psi.AstLoadingFilter;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.logging.Logger;
import consulo.ide.localize.IdeLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.style.StyleManager;
import consulo.usage.*;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public abstract class ChooseByNameBase implements ChooseByNameViewModel {
  public static final String TEMPORARILY_FOCUSABLE_COMPONENT_KEY = "ChooseByNameBase.TemporarilyFocusableComponent";

  private static final Logger LOG = Logger.getInstance(ChooseByNameBase.class);

  @Nullable
  protected final Project myProject;
  protected final ChooseByNameModel myModel;
  @Nonnull
  protected ChooseByNameItemProvider myProvider;
  final String myInitialText;
  private boolean mySearchInAnyPlace;

  Component myPreviouslyFocusedComponent;
  private boolean myInitialized;

  final JPanelProvider myTextFieldPanel = new JPanelProvider();// Located in the layered pane
  protected final MyTextField myTextField = new MyTextField();
  private final CardLayout myCard = new CardLayout();
  private final JPanel myCardContainer = new JPanel(myCard);
  protected final JCheckBox myCheckBox = new JCheckBox();
  /**
   * the tool area of the popup, it is just after card box
   */
  private JComponent myToolArea;

  JScrollPane myListScrollPane; // Located in the layered pane
  private final SmartPointerListModel<Object> myListModel = new SmartPointerListModel<>();
  protected final JList<Object> myList = new JBList<>(myListModel);
  private final List<Pair<String, Integer>> myHistory = new ArrayList<>();
  private final List<Pair<String, Integer>> myFuture = new ArrayList<>();

  protected ChooseByNamePopupComponent.Callback myActionListener;

  protected final Alarm myAlarm = new Alarm();

  private boolean myDisposedFlag;

  private final String[][] myNames = new String[2][];
  private volatile CalcElementsThread myCalcElementsThread;
  private int myListSizeIncreasing = 30;
  private int myMaximumListSizeLimit = 30;
  @NonNls
  private static final String NOT_FOUND_IN_PROJECT_CARD = "syslib";
  @NonNls
  private static final String NOT_FOUND_CARD = "nfound";
  @NonNls
  private static final String CHECK_BOX_CARD = "chkbox";
  @NonNls
  private static final String SEARCHING_CARD = "searching";
  private final int myRebuildDelay;

  private final Alarm myHideAlarm = new Alarm();
  private static final boolean myShowListAfterCompletionKeyStroke = false;
  JBPopup myTextPopup;
  protected JBPopup myDropdownPopup;

  private boolean myClosedByShiftEnter;
  final int myInitialIndex;
  private String myFindUsagesTitle;
  private ShortcutSet myCheckBoxShortcut;
  private final boolean myInitIsDone;
  private boolean myAlwaysHasMore;
  private Point myFocusPoint;
  @Nullable
  SelectionSnapshot currentChosenInfo;

  public boolean checkDisposed() {
    return myDisposedFlag;
  }

  public void setDisposed(boolean disposedFlag) {
    myDisposedFlag = disposedFlag;
    if (disposedFlag) {
      setNamesSync(true, null);
      setNamesSync(false, null);
    }
  }

  private void setNamesSync(boolean checkboxState, @Nullable String[] value) {
    synchronized (myNames) {
      myNames[checkboxState ? 1 : 0] = value;
    }
  }

  /**
   * @param initialText initial text which will be in the lookup text field
   */
  protected ChooseByNameBase(Project project, @Nonnull ChooseByNameModel model, String initialText, PsiElement context) {
    this(project, model, ChooseByNameModelEx.getItemProvider(model, context), initialText, 0);
  }

  @SuppressWarnings("UnusedDeclaration") // Used in MPS
  protected ChooseByNameBase(Project project,
                             @Nonnull ChooseByNameModel model,
                             @Nonnull ChooseByNameItemProvider provider,
                             String initialText) {
    this(project, model, provider, initialText, 0);
  }

  /**
   * @param initialText initial text which will be in the lookup text field
   */
  protected ChooseByNameBase(@Nullable Project project,
                             @Nonnull ChooseByNameModel model,
                             @Nonnull ChooseByNameItemProvider provider,
                             String initialText,
                             final int initialIndex) {
    myProject = project;
    myModel = model;
    myInitialText = initialText;
    myProvider = provider;
    myInitialIndex = initialIndex;
    mySearchInAnyPlace = Registry.is("ide.goto.middle.matching") && model.useMiddleMatching();
    myRebuildDelay = Registry.intValue("ide.goto.rebuild.delay");

    myTextField.setText(myInitialText);
    myInitIsDone = true;
  }

  @Nullable
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public boolean isSearchInAnyPlace() {
    return mySearchInAnyPlace;
  }

  public void setSearchInAnyPlace(boolean searchInAnyPlace) {
    mySearchInAnyPlace = searchInAnyPlace;
  }

  public boolean isClosedByShiftEnter() {
    return myClosedByShiftEnter;
  }

  public boolean isOpenInCurrentWindowRequested() {
    return isClosedByShiftEnter();
  }

  /**
   * Set tool area. The method may be called only before invoke.
   *
   * @param toolArea a tool area component
   */
  public void setToolArea(@Nonnull JComponent toolArea) {
    if (myToolArea != null) {
      throw new IllegalStateException("Tool area is modifiable only before invoke()");
    }
    myToolArea = toolArea;
  }

  public void setFindUsagesTitle(@Nullable String findUsagesTitle) {
    myFindUsagesTitle = findUsagesTitle;
  }

  public void invoke(final ChooseByNamePopupComponent.Callback callback,
                     final ModalityState modalityState,
                     boolean allowMultipleSelection) {
    initUI(callback, modalityState, allowMultipleSelection);
  }

  @Nonnull
  @Override
  public ChooseByNameModel getModel() {
    return myModel;
  }

  public class JPanelProvider extends JPanel implements DataProvider, QuickSearchComponent {
    private JBPopup myHint;
    private boolean myFocusRequested;

    JPanelProvider() {
    }

    @Override
    public Object getData(@Nonnull Key dataId) {
      if (PlatformDataKeys.SEARCH_INPUT_TEXT == dataId) {
        return myTextField.getText();
      }

      if (HelpManager.HELP_ID == dataId) {
        return myModel.getHelpId();
      }

      if (myCalcElementsThread != null) {
        return null;
      }
      if (PsiElement.KEY == dataId) {
        Object element = getChosenElement();

        if (element instanceof PsiElement) {
          return element;
        }

        if (element instanceof DataProvider dataProvider) {
          return dataProvider.getData(dataId);
        }
      }
      else if (PsiElement.KEY_OF_ARRAY == dataId) {
        final List<Object> chosenElements = getChosenElements();
        List<PsiElement> result = new ArrayList<>(chosenElements.size());
        for (Object element : chosenElements) {
          if (element instanceof PsiElement psiElement) {
            result.add(psiElement);
          }
        }
        return PsiUtilCore.toPsiElementArray(result);
      }
      else if (UIExAWTDataKey.DOMINANT_HINT_AREA_RECTANGLE == dataId) {
        return getBounds();
      }
      return null;
    }

    @Override
    public void registerHint(@Nonnull JBPopup h) {
      if (myHint != null && myHint.isVisible() && myHint != h) {
        myHint.cancel();
      }
      myHint = h;
    }

    boolean focusRequested() {
      boolean focusRequested = myFocusRequested;

      myFocusRequested = false;

      return focusRequested;
    }

    @Override
    public void requestFocus() {
      myFocusRequested = true;
    }

    @Override
    public void unregisterHint() {
      myHint = null;
    }

    public void hideHint() {
      if (myHint != null) {
        myHint.cancel();
      }
    }

    @Nullable
    public JBPopup getHint() {
      return myHint;
    }

    void updateHint(PsiElement element) {
      if (myHint == null || !myHint.isVisible()) return;
      final PopupUpdateProcessor updateProcessor = myHint.getUserData(PopupUpdateProcessor.class);
      if (updateProcessor != null) {
        updateProcessor.updatePopup(element);
      }
    }

    void repositionHint() {
      if (myHint == null || !myHint.isVisible()) return;
      PopupPositionManager.positionPopupInBestPosition(myHint, null, null);
    }
  }

  /**
   * @param modalityState - if not null rebuilds list in given {@link ModalityState}
   */
  protected void initUI(final ChooseByNamePopupComponent.Callback callback,
                        final ModalityState modalityState,
                        final boolean allowMultipleSelection) {
    myPreviouslyFocusedComponent = WindowManagerEx.getInstanceEx().getFocusedComponent(myProject);

    myActionListener = callback;
    myTextFieldPanel.setLayout(new BoxLayout(myTextFieldPanel, BoxLayout.Y_AXIS));

    final JPanel hBox = new JPanel();
    hBox.setLayout(new BoxLayout(hBox, BoxLayout.X_AXIS));

    JPanel caption2Tools = new JPanel(new BorderLayout());

    if (myModel.getPromptText() != null) {
      JLabel label = new JLabel(myModel.getPromptText());
      label.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
      caption2Tools.add(label, BorderLayout.WEST);
    }

    caption2Tools.add(hBox, BorderLayout.EAST);

    String checkBoxName = myModel.getCheckBoxName();
    Color fg = UIUtil.getLabelDisabledForeground();
    Color color = StyleManager.get().getCurrentStyle().isDark() ? ColorUtil.shift(fg, 1.2) : ColorUtil.shift(fg, 0.7);
    String text = checkBoxName == null
      ? ""
      : "<html>" + checkBoxName +
      (myCheckBoxShortcut != null && myCheckBoxShortcut.getShortcuts().length > 0 ? " <b color='" +
        ColorUtil.toHex(color) +
        "'>" +
        KeymapUtil.getShortcutsText(myCheckBoxShortcut.getShortcuts()) +
        "</b>" : "") +
      "</html>";
    myCheckBox.setText(text);
    myCheckBox.setAlignmentX(SwingConstants.RIGHT);
    myCheckBox.setBorder(null);

    myCheckBox.setSelected(myModel.loadInitialCheckBoxState());

    if (checkBoxName == null) {
      myCheckBox.setVisible(false);
    }

    addCard(myCheckBox, CHECK_BOX_CARD);

    addCard(new HintLabel(myModel.getNotInMessage()), NOT_FOUND_IN_PROJECT_CARD);
    addCard(new HintLabel(IdeLocalize.labelChoosebynameNoMatchesFound().get()), NOT_FOUND_CARD);
    JPanel searching = new JPanel(new BorderLayout(5, 0));
    searching.add(new AsyncProcessIcon("searching"), BorderLayout.WEST);
    searching.add(new HintLabel(IdeLocalize.labelChoosebynameSearching().get()), BorderLayout.CENTER);
    addCard(searching, SEARCHING_CARD);
    myCard.show(myCardContainer, CHECK_BOX_CARD);

    if (isCheckboxVisible()) {
      hBox.add(myCardContainer);
    }

    final DefaultActionGroup group = new DefaultActionGroup();
    group.add(new ShowFindUsagesAction() {
      @Nonnull
      @Override
      public PsiElement[] getElements() {
        List<Object> objects = myListModel.getItems();
        List<PsiElement> elements = new ArrayList<>(objects.size());
        for (Object object : objects) {
          if (object instanceof PsiElement psiElement) {
            elements.add(psiElement);
          }
          else if (object instanceof DataProvider dataProvider) {
            ContainerUtil.addIfNotNull(elements, dataProvider.getDataUnchecked(PsiElement.KEY));
          }
        }
        return PsiUtilCore.toPsiElementArray(elements);
      }
    });
    final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ChooseByNameBase", group, true);
    actionToolbar.setTargetComponent(hBox);
    actionToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    actionToolbar.updateActionsImmediately(); // we need valid ActionToolbar.getPreferredSize() to calc size of popup
    final JComponent toolbarComponent = actionToolbar.getComponent();
    toolbarComponent.setBorder(null);

    if (myToolArea == null) {
      myToolArea = new JLabel(JBUI.scale(EmptyIcon.create(1, 24)));
    }
    else {
      myToolArea.setBorder(JBUI.Borders.emptyLeft(6)); // space between checkbox and filter/show all in view buttons
    }
    hBox.add(myToolArea);
    hBox.add(toolbarComponent);

    myTextFieldPanel.add(caption2Tools);

    new MyCopyReferenceAction().registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY).getShortcutSet(),
                                                          myTextField);

    myTextFieldPanel.add(myTextField);
    Font editorFont = EditorAWTUtil.getEditorFont();
    myTextField.setFont(editorFont);
    myTextField.putClientProperty("caretWidth", JBUIScale.scale(EditorUtil.getDefaultCaretWidth()));

    if (checkBoxName != null) {
      if (myCheckBoxShortcut != null) {
        new DumbAwareAction("change goto check box", null, null) {
          @RequiredUIAccess
          @Override
          public void actionPerformed(@Nonnull AnActionEvent e) {
            myCheckBox.setSelected(!myCheckBox.isSelected());
          }
        }.registerCustomShortcutSet(myCheckBoxShortcut, myTextField);
      }
    }

    if (isCloseByFocusLost()) {
      myTextField.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(@Nonnull final FocusEvent e) {
          if (Registry.is("focus.follows.mouse.workarounds")) {
            if (myFocusPoint != null) {
              PointerInfo pointerInfo = MouseInfo.getPointerInfo();
              if (pointerInfo != null && myFocusPoint.equals(pointerInfo.getLocation())) {
                // Ignore the loss of focus if the mouse hasn't moved between the last dropdown resize
                // and the loss of focus event. This happens in focus follows mouse mode if the mouse is
                // over the dropdown and it resizes to leave the mouse outside the dropdown.
                ProjectIdeFocusManager.getInstance(myProject).requestFocus(myTextField, true);
                myFocusPoint = null;
                return;
              }
            }
            myFocusPoint = null;
          }
          cancelListUpdater(); // cancel thread as early as possible
          myHideAlarm.addRequest(() -> {
            JBPopup popup = JBPopupFactory.getInstance().getChildFocusedPopup(e.getComponent());
            if (popup != null) {
              popup.addListener(new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                  if (event.isOk()) {
                    hideHint();
                  }
                }
              });
            }
            else {
              Component oppositeComponent = e.getOppositeComponent();
              if (oppositeComponent == myCheckBox) {
                ProjectIdeFocusManager.getInstance(myProject).requestFocus(myTextField, true);
                return;
              }
              if (oppositeComponent != null && !(oppositeComponent instanceof JFrame) &&
                myList.isShowing() &&
                (oppositeComponent == myList || SwingUtilities.isDescendingFrom(myList, oppositeComponent))) {
                ProjectIdeFocusManager.getInstance(myProject).requestFocus(myTextField, true);// Otherwise me may skip some KeyEvents
                return;
              }

              if (isDescendingFromTemporarilyFocusableToolWindow(oppositeComponent)) {
                return; // Allow toolwindows to gain focus (used by QuickDoc shown in a toolwindow)
              }

              if (UIUtil.haveCommonOwner(oppositeComponent, e.getComponent())) {
                return;
              }

              hideHint();
            }
          }, 5);
        }
      });
    }

    myCheckBox.addItemListener(__ -> rebuildList(false));
    myCheckBox.setFocusable(false);

    myTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@Nonnull DocumentEvent e) {
        SelectionPolicy toSelect =
          currentChosenInfo != null && currentChosenInfo.hasSamePattern(ChooseByNameBase.this) ? PreserveSelection.INSTANCE : SelectMostRelevant.INSTANCE;
        rebuildList(toSelect, myRebuildDelay, IdeaModalityState.current(), null);
      }
    });

    final Set<KeyStroke> upShortcuts = getShortcuts(IdeActions.ACTION_EDITOR_MOVE_CARET_UP);
    final Set<KeyStroke> downShortcuts = getShortcuts(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN);
    myTextField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(@Nonnull KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && (e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
          myClosedByShiftEnter = true;
          close(true);
        }
        if (!myListScrollPane.isVisible()) {
          return;
        }
        final int keyCode;

        // Add support for user-defined 'caret up/down' shortcuts.
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
        if (upShortcuts.contains(stroke)) {
          keyCode = KeyEvent.VK_UP;
        }
        else if (downShortcuts.contains(stroke)) {
          keyCode = KeyEvent.VK_DOWN;
        }
        else {
          keyCode = e.getKeyCode();
        }
        switch (keyCode) {
          case KeyEvent.VK_DOWN:
            ScrollingUtil.moveDown(myList, e.getModifiersEx());
            break;
          case KeyEvent.VK_UP:
            ScrollingUtil.moveUp(myList, e.getModifiersEx());
            break;
          case KeyEvent.VK_PAGE_UP:
            ScrollingUtil.movePageUp(myList);
            break;
          case KeyEvent.VK_PAGE_DOWN:
            ScrollingUtil.movePageDown(myList);
            break;
          case KeyEvent.VK_TAB:
            close(true);
            break;
          case KeyEvent.VK_ENTER:
            if (myList.getSelectedValue() == EXTRA_ELEM) {
              myMaximumListSizeLimit += myListSizeIncreasing;
              rebuildList(new SelectIndex(myList.getSelectedIndex()), myRebuildDelay, IdeaModalityState.current(), null);
              e.consume();
            }
            break;
        }
      }
    });

    myTextField.addActionListener(__ -> {
      if (!getChosenElements().isEmpty()) {
        doClose(true);
      }
    });

    myList.setFocusable(false);
    myList.setSelectionMode(allowMultipleSelection ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    new ClickListener() {
      @Override
      public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
        if (!myTextField.hasFocus()) {
          ProjectIdeFocusManager.getInstance(myProject).requestFocus(myTextField, true);
        }

        if (clickCount == 2) {
          int selectedIndex = myList.getSelectedIndex();
          Rectangle selectedCellBounds = myList.getCellBounds(selectedIndex, selectedIndex);

          if (selectedCellBounds != null && selectedCellBounds.contains(e.getPoint())) { // Otherwise it was reselected in the selection listener
            if (myList.getSelectedValue() == EXTRA_ELEM) {
              myMaximumListSizeLimit += myListSizeIncreasing;
              rebuildList(new SelectIndex(selectedIndex), myRebuildDelay, IdeaModalityState.current(), null);
            }
            else {
              doClose(true);
            }
          }
          return true;
        }

        return false;
      }
    }.installOn(myList);

    ListCellRenderer modelRenderer = myModel.getListCellRenderer();
    //noinspection unchecked
    myList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> AstLoadingFilter.disallowTreeLoading(() -> modelRenderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus)));
    myList.setVisibleRowCount(16);
    myList.setFont(editorFont);

    myList.addListSelectionListener(__ -> {
      if (checkDisposed()) {
        return;
      }

      chosenElementMightChange();
      updateDocumentation();

      List<Object> chosenElements = getChosenElements();
      if (!chosenElements.isEmpty()) {
        currentChosenInfo = new SelectionSnapshot(getTrimmedText(), new HashSet<>(chosenElements));
      }
    });

    myListScrollPane = ScrollPaneFactory.createScrollPane(myList, true);

    myTextFieldPanel.setBorder(JBUI.Borders.empty(5));

    showTextFieldPanel();

    myInitialized = true;

    if (modalityState != null) {
      rebuildList(SelectionPolicyKt.fromIndex(myInitialIndex), 0, modalityState, null);
    }
  }

  private boolean isDescendingFromTemporarilyFocusableToolWindow(@Nullable Component component) {
    if (component == null || myProject == null || myProject.isDisposed()) return false;

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(toolWindowManager.getActiveToolWindowId());
    JComponent toolWindowComponent = toolWindow != null ? toolWindow.getComponent() : null;
    return toolWindowComponent != null && toolWindowComponent.getClientProperty(TEMPORARILY_FOCUSABLE_COMPONENT_KEY) != null && SwingUtilities
      .isDescendingFrom(component, toolWindowComponent);
  }

  private void addCard(@Nonnull JComponent comp, @Nonnull String cardId) {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(comp, BorderLayout.EAST);
    myCardContainer.add(wrapper, cardId);
  }

  public void setCheckBoxShortcut(@Nonnull ShortcutSet shortcutSet) {
    myCheckBoxShortcut = shortcutSet;
  }

  @Nonnull
  private static Set<KeyStroke> getShortcuts(@Nonnull String actionId) {
    Set<KeyStroke> result = new HashSet<>();
    for (Shortcut shortcut : KeymapUtil.getActiveKeymapShortcuts(actionId).getShortcuts()) {
      if (shortcut instanceof KeyboardShortcut) {
        KeyboardShortcut keyboardShortcut = (KeyboardShortcut)shortcut;
        result.add(keyboardShortcut.getFirstKeyStroke());
      }
    }
    return result;
  }

  private void hideHint() {
    if (!myTextFieldPanel.focusRequested()) {
      doClose(false);
      myTextFieldPanel.hideHint();
    }
  }

  /**
   * Default rebuild list. It uses {@link #myRebuildDelay} and current modality state.
   */
  public void rebuildList(boolean initial) {
    // TODO this method is public, because the chooser does not listed for the model.
    rebuildList(initial ? SelectionPolicyKt.fromIndex(myInitialIndex) : SelectMostRelevant.INSTANCE,
                myRebuildDelay,
                IdeaModalityState.current(),
                null);
  }

  private void updateDocumentation() {
    final JBPopup hint = myTextFieldPanel.getHint();
    final Object element = getChosenElement();
    if (hint != null) {
      if (element instanceof PsiElement) {
        myTextFieldPanel.updateHint((PsiElement)element);
      }
      else if (element instanceof DataProvider dataProvider) {
        final Object o = dataProvider.getData(PsiElement.KEY);
        if (o instanceof PsiElement psiElement) {
          myTextFieldPanel.updateHint(psiElement);
        }
      }
    }
  }

  @Nonnull
  @Override
  public String transformPattern(@Nonnull String pattern) {
    return pattern;
  }

  protected void doClose(final boolean ok) {
    if (checkDisposed()) return;

    if (closeForbidden(ok)) return;

    cancelListUpdater();
    close(ok);

    myListModel.removeAll();
  }

  protected boolean closeForbidden(boolean ok) {
    return false;
  }

  void cancelListUpdater() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (checkDisposed()) return;

    final CalcElementsThread calcElementsThread = myCalcElementsThread;
    if (calcElementsThread != null) {
      calcElementsThread.cancel();
      myCalcElementsThread = null;
    }
  }

  @Nonnull
  public String getTrimmedText() {
    return StringUtil.trimLeading(StringUtil.notNullize(myTextField.getText()));
  }

  @Nonnull
  private synchronized String[] ensureNamesLoaded(boolean checkboxState) {
    String[] cached = getNamesSync(checkboxState);
    if (cached != null) return cached;

    if (checkboxState && myModel instanceof ContributorsBasedGotoByModel && ((ContributorsBasedGotoByModel)myModel).sameNamesForProjectAndLibraries() && getNamesSync(
      false) != null) {
      // there is no way in indices to have different keys for project symbols vs libraries, we always have same ones
      String[] allNames = getNamesSync(false);
      setNamesSync(true, allNames);
      return allNames;
    }

    String[] result = myModel.getNames(checkboxState);
    //noinspection ConstantConditions
    assert result != null : "Model " + myModel + "(" + myModel.getClass() + ") returned null names";
    setNamesSync(checkboxState, result);

    return result;
  }

  @Nonnull
  public String[] getNames(boolean checkboxState) {
    setNamesSync(checkboxState, null);
    return ensureNamesLoaded(checkboxState);
  }

  private String[] getNamesSync(boolean checkboxState) {
    synchronized (myNames) {
      return myNames[checkboxState ? 1 : 0];
    }
  }

  @Nonnull
  protected Set<Object> filter(@Nonnull Set<Object> elements) {
    return elements;
  }

  protected abstract boolean isCheckboxVisible();

  protected abstract boolean isShowListForEmptyPattern();

  protected abstract boolean isCloseByFocusLost();

  protected void showTextFieldPanel() {
    final JLayeredPane layeredPane = getLayeredPane();
    final Dimension preferredTextFieldPanelSize = myTextFieldPanel.getPreferredSize();
    final int x = (layeredPane.getWidth() - preferredTextFieldPanelSize.width) / 2;
    final int paneHeight = layeredPane.getHeight();
    final int y = paneHeight / 3 - preferredTextFieldPanelSize.height / 2;

    ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(myTextFieldPanel, myTextField);
    builder.setLocateWithinScreenBounds(false);
    builder.setKeyEventHandler(event -> {
      if (myTextPopup == null || !AbstractPopup.isCloseRequest(event) || !myTextPopup.isCancelKeyEnabled()) {
        return false;
      }

      IdeFocusManager focusManager = ProjectIdeFocusManager.getInstance(myProject);
      if (isDescendingFromTemporarilyFocusableToolWindow(focusManager.getFocusOwner())) {
        focusManager.requestFocus(myTextField, true);
        return false;
      }
      else {
        myTextPopup.cancel(event);
        return true;
      }
    }).setCancelCallback(() -> {
      myTextPopup = null;
      close(false);
      return Boolean.TRUE;
    }).setFocusable(true).setRequestFocus(true).setModalContext(false).setCancelOnClickOutside(false);

    Point point = new Point(x, y);
    SwingUtilities.convertPointToScreen(point, layeredPane);
    Rectangle bounds = new Rectangle(point, new Dimension(preferredTextFieldPanelSize.width + 20, preferredTextFieldPanelSize.height));
    myTextPopup = builder.createPopup();
    myTextPopup.setSize(bounds.getSize());
    myTextPopup.setLocation(bounds.getLocation());

    MnemonicHelper.init(myTextFieldPanel);
    if (myProject != null && !myProject.isDefault()) {
      DaemonCodeAnalyzer.getInstance(myProject).disableUpdateByTimer(myTextPopup);
    }

    Disposer.register(myTextPopup, () -> cancelListUpdater());
    IdeEventQueueProxy.getInstance().closeAllPopups(false);
    myTextPopup.show(layeredPane);
  }

  private JLayeredPane getLayeredPane() {
    JLayeredPane layeredPane;
    final Window window = TargetAWT.to(WindowManager.getInstance().suggestParentWindow(myProject));

    if (window instanceof JFrame jFrame) {
      layeredPane = jFrame.getLayeredPane();
    }
    else if (window instanceof JDialog jDialog) {
      layeredPane = jDialog.getLayeredPane();
    }
    else if (window instanceof JWindow jWindow) {
      layeredPane = jWindow.getLayeredPane();
    }
    else {
      throw new IllegalStateException("cannot find parent window: project=" + myProject + (myProject != null ? "; open=" + myProject.isOpen() : "") + "; window=" + window);
    }
    return layeredPane;
  }

  void rebuildList(@Nonnull SelectionPolicy pos,
                   final int delay,
                   @Nonnull final ModalityState modalityState,
                   @Nullable final Runnable postRunnable) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!myInitialized) {
      return;
    }

    myAlarm.cancelAllRequests();

    if (delay > 0) {
      myAlarm.addRequest(() -> rebuildList(pos, 0, modalityState, postRunnable), delay, IdeaModalityState.stateForComponent(myTextField));
      return;
    }

    final CalcElementsThread calcElementsThread = myCalcElementsThread;
    if (calcElementsThread != null) {
      calcElementsThread.cancel();
    }

    final String text = getTrimmedText();
    if (!canShowListForEmptyPattern() && text.isEmpty()) {
      myListModel.removeAll();
      hideList();
      myTextFieldPanel.hideHint();
      myCard.show(myCardContainer, CHECK_BOX_CARD);
      return;
    }

    ListCellRenderer cellRenderer = myList.getCellRenderer();
    if (cellRenderer instanceof ExpandedItemListCellRendererWrapper) {
      cellRenderer = ((ExpandedItemListCellRendererWrapper)cellRenderer).getWrappee();
    }
    final String pattern = patternToLowerCase(transformPattern(text));
    final Matcher matcher = buildPatternMatcher(isSearchInAnyPlace() ? "*" + pattern : pattern);
    if (cellRenderer instanceof MatcherHolder) {
      ((MatcherHolder)cellRenderer).setPatternMatcher(matcher);
    }
    MatcherHolder.associateMatcher(myList, matcher);

    scheduleCalcElements(text, myCheckBox.isSelected(), modalityState, pos, elements -> {
      ApplicationManager.getApplication().assertIsDispatchThread();

      if (postRunnable != null) {
        postRunnable.run();
      }
    });
  }

  private void backgroundCalculationFinished(@Nonnull Collection<?> result, @Nonnull SelectionPolicy toSelect) {
    myCalcElementsThread = null;
    setElementsToList(toSelect, result);
    myList.repaint();
    chosenElementMightChange();

    if (result.isEmpty()) {
      myTextFieldPanel.hideHint();
    }
  }

  public void scheduleCalcElements(@Nonnull String text,
                                   boolean checkboxState,
                                   @Nonnull ModalityState modalityState,
                                   @Nonnull SelectionPolicy policy,
                                   @Nonnull Consumer<? super Set<?>> callback) {
    new CalcElementsThread(text, checkboxState, modalityState, policy, callback).scheduleThread();
  }

  private static boolean isShowListAfterCompletionKeyStroke() {
    return myShowListAfterCompletionKeyStroke;
  }

  private void setElementsToList(@Nonnull SelectionPolicy pos, @Nonnull Collection<?> elements) {
    if (checkDisposed()) return;
    if (isCloseByFocusLost() && Registry.is("focus.follows.mouse.workarounds")) {
      PointerInfo pointerInfo = MouseInfo.getPointerInfo();
      if (pointerInfo != null) {
        myFocusPoint = pointerInfo.getLocation();
      }
    }
    if (elements.isEmpty()) {
      myListModel.removeAll();
      myTextField.setForeground(JBColor.red);
      hideList();
      return;
    }

    Object[] oldElements = myListModel.getItems().toArray();
    Object[] newElements = elements.toArray();
    if (ArrayUtil.contains(null, newElements)) {
      LOG.error("Null after filtering elements by " + this);
    }
    List<ModelDiff.Cmd> commands = ModelDiff.createDiffCmds(myListModel, oldElements, newElements);

    myTextField.setForeground(UIUtil.getTextFieldForeground());
    if (commands == null || commands.isEmpty()) {
      applySelection(pos);
      showList();
      myTextFieldPanel.repositionHint();
    }
    else {
      appendToModel(commands, pos);
    }
  }

  @VisibleForTesting
  public int calcSelectedIndex(@Nonnull Object[] modelElements, @Nonnull String trimmedText) {
    if (myModel instanceof Comparator) {
      return 0;
    }

    Matcher matcher = buildPatternMatcher(transformPattern(trimmedText));
    final String statContext = statisticsContext();
    Comparator<Object> itemComparator = Comparator.
                                                    comparing(e -> trimmedText.equalsIgnoreCase(myModel.getElementName(e))).
                                                    thenComparing(e -> matchingDegree(matcher, e)).
                                                    thenComparing(e -> getUseCount(statContext, e)).
                                                    reversed();

    int bestPosition = 0;
    while (bestPosition < modelElements.length - 1 && isSpecialElement(modelElements[bestPosition])) bestPosition++;

    for (int i = 1; i < modelElements.length; i++) {
      final Object modelElement = modelElements[i];
      if (isSpecialElement(modelElement)) continue;

      if (itemComparator.compare(modelElement, modelElements[bestPosition]) < 0) {
        bestPosition = i;
      }
    }

    return bestPosition;
  }

  private static boolean isSpecialElement(@Nonnull Object modelElement) {
    return EXTRA_ELEM.equals(modelElement);
  }

  private int getUseCount(@Nonnull String statContext, @Nonnull Object modelElement) {
    String text = myModel.getFullName(modelElement);
    return text == null ? Integer.MIN_VALUE : StatisticsManager.getInstance().getUseCount(new StatisticsInfo(statContext, text));
  }

  private int matchingDegree(@Nonnull Matcher matcher, @Nonnull Object modelElement) {
    String name = myModel.getElementName(modelElement);
    return name != null && matcher instanceof MinusculeMatcher ? ((MinusculeMatcher)matcher).matchingDegree(name) : Integer.MIN_VALUE;
  }

  @Nonnull
  @NonNls
  String statisticsContext() {
    return "choose_by_name#" + myModel.getPromptText() + "#" + myCheckBox.isSelected() + "#" + getTrimmedText();
  }

  private void appendToModel(@Nonnull List<? extends ModelDiff.Cmd> commands, @Nonnull SelectionPolicy selection) {
    for (ModelDiff.Cmd command : commands) {
      command.apply();
    }
    showList();

    myTextFieldPanel.repositionHint();

    if (!myListModel.isEmpty()) {
      applySelection(selection);
    }
  }

  private void applySelection(@Nonnull SelectionPolicy selection) {
    List<Integer> indices = selection.performSelection(this, myListModel);
    myList.setSelectedIndices(Ints.toArray(indices));
    if (!indices.isEmpty()) {
      ScrollingUtil.ensureIndexIsVisible(myList, indices.get(0).intValue(), 0);
    }
  }

  /**
   * @deprecated unused
   */
  @Deprecated
  public boolean hasPostponedAction() {
    return false;
  }

  protected abstract void showList();

  protected abstract void hideList();

  protected abstract void close(boolean isOk);

  @Nullable
  public Object getChosenElement() {
    final List<Object> elements = getChosenElements();
    return elements.size() == 1 ? elements.get(0) : null;
  }

  @Nonnull
  protected List<Object> getChosenElements() {
    return ContainerUtil.filter(myList.getSelectedValuesList(), o -> o != null && !isSpecialElement(o));
  }

  protected void chosenElementMightChange() {
  }

  protected final class MyTextField extends JTextField implements PopupOwner, TypeSafeDataProvider {
    private final KeyStroke myCompletionKeyStroke;
    private final KeyStroke forwardStroke;
    private final KeyStroke backStroke;

    private boolean completionKeyStrokeHappened;

    private MyTextField() {
      super(40);
      enableEvents(AWTEvent.KEY_EVENT_MASK);
      myCompletionKeyStroke = getShortcut(IdeActions.ACTION_CODE_COMPLETION);
      forwardStroke = getShortcut(IdeActions.ACTION_GOTO_FORWARD);
      backStroke = getShortcut(IdeActions.ACTION_GOTO_BACK);
      setFocusTraversalKeysEnabled(false);
      putClientProperty("JTextField.variant", "search");
      setDocument(new PlainDocument() {
        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
          super.insertString(offs, str, a);
          if (str != null && str.length() > 1) {
            handlePaste(str);
          }
        }
      });
    }

    @Nullable
    private KeyStroke getShortcut(@Nonnull String actionCodeCompletion) {
      final Shortcut[] shortcuts = KeymapUtil.getActiveKeymapShortcuts(actionCodeCompletion).getShortcuts();
      for (final Shortcut shortcut : shortcuts) {
        if (shortcut instanceof KeyboardShortcut) {
          return ((KeyboardShortcut)shortcut).getFirstKeyStroke();
        }
      }
      return null;
    }

    @Override
    public void calcData(@Nonnull final Key key, @Nonnull final DataSink sink) {
      if (LangDataKeys.POSITION_ADJUSTER_POPUP.equals(key)) {
        if (myDropdownPopup != null && myDropdownPopup.isVisible()) {
          sink.put(key, myDropdownPopup);
        }
      }
      else if (LangDataKeys.PARENT_POPUP.equals(key)) {
        if (myTextPopup != null && myTextPopup.isVisible()) {
          sink.put(key, myTextPopup);
        }
      }
    }

    @Override
    protected void processKeyEvent(@Nonnull KeyEvent e) {
      final KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);

      if (keyStroke.equals(myCompletionKeyStroke)) {
        completionKeyStrokeHappened = true;
        e.consume();
        final String pattern = getTrimmedText();
        final int oldPos = myList.getSelectedIndex();
        myHistory.add(Pair.create(pattern, oldPos));
        final Runnable postRunnable = () -> fillInCommonPrefix(pattern);
        rebuildList(SelectMostRelevant.INSTANCE, 0, IdeaModalityState.current(), postRunnable);
        return;
      }
      if (keyStroke.equals(backStroke)) {
        e.consume();
        if (!myHistory.isEmpty()) {
          final String oldText = getTrimmedText();
          final int oldPos = myList.getSelectedIndex();
          final Pair<String, Integer> last = myHistory.remove(myHistory.size() - 1);
          myTextField.setText(last.first);
          myFuture.add(Pair.create(oldText, oldPos));
          rebuildList(SelectMostRelevant.INSTANCE, 0, IdeaModalityState.current(), null);
        }
        return;
      }
      if (keyStroke.equals(forwardStroke)) {
        e.consume();
        if (!myFuture.isEmpty()) {
          final String oldText = getTrimmedText();
          final int oldPos = myList.getSelectedIndex();
          final Pair<String, Integer> next = myFuture.remove(myFuture.size() - 1);
          myTextField.setText(next.first);
          myHistory.add(Pair.create(oldText, oldPos));
          rebuildList(SelectMostRelevant.INSTANCE, 0, IdeaModalityState.current(), null);
        }
        return;
      }
      int position = myTextField.getCaretPosition();
      int code = keyStroke.getKeyCode();
      int modifiers = keyStroke.getModifiers();
      try {
        super.processKeyEvent(e);
      }
      catch (NullPointerException e1) {
        if (!Patches.SUN_BUG_ID_6322854) {
          throw e1;
        }
      }
      finally {
        if ((code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) && modifiers == 0) {
          myTextField.setCaretPosition(position);
        }
      }
    }

    private void fillInCommonPrefix(@Nonnull final String pattern) {
      final List<String> list = myProvider.filterNames(ChooseByNameBase.this, getNames(myCheckBox.isSelected()), pattern);
      if (list.isEmpty()) return;

      if (isComplexPattern(pattern)) return; //TODO: support '*'
      final String oldText = getTrimmedText();
      final int oldPos = myList.getSelectedIndex();

      String commonPrefix = null;
      if (!list.isEmpty()) {
        for (String name : list) {
          final String string = StringUtil.toLowerCase(name);
          if (commonPrefix == null) {
            commonPrefix = string;
          }
          else {
            while (!commonPrefix.isEmpty()) {
              if (string.startsWith(commonPrefix)) {
                break;
              }
              commonPrefix = commonPrefix.substring(0, commonPrefix.length() - 1);
            }
            if (commonPrefix.isEmpty()) break;
          }
        }
        commonPrefix = list.get(0).substring(0, commonPrefix.length());
        for (int i = 1; i < list.size(); i++) {
          final String string = list.get(i).substring(0, commonPrefix.length());
          if (!string.equals(commonPrefix)) {
            commonPrefix = StringUtil.toLowerCase(commonPrefix);
            break;
          }
        }
      }
      if (commonPrefix == null) commonPrefix = "";
      if (!StringUtil.startsWithIgnoreCase(commonPrefix, pattern)) {
        commonPrefix = pattern;
      }
      final String newPattern = commonPrefix;

      myHistory.add(Pair.create(oldText, oldPos));
      myTextField.setText(newPattern);
      myTextField.setCaretPosition(newPattern.length());

      rebuildList(false);
    }

    private boolean isComplexPattern(@Nonnull final String pattern) {
      if (pattern.indexOf('*') >= 0) return true;
      for (String s : myModel.getSeparators()) {
        if (pattern.contains(s)) return true;
      }

      return false;
    }

    @Override
    @Nonnull
    public Point getBestPopupPosition() {
      return new Point(myTextFieldPanel.getWidth(), getHeight());
    }

    @Override
    protected void paintComponent(@Nonnull final Graphics g) {
      GraphicsUtil.setupAntialiasing(g);
      super.paintComponent(g);
    }

    boolean isCompletionKeyStroke() {
      return completionKeyStrokeHappened;
    }
  }

  @Nonnull
  public ChooseByNameItemProvider getProvider() {
    return myProvider;
  }

  private void handlePaste(@Nonnull String str) {
    if (!myInitIsDone) return;
    if (myModel instanceof GotoClassModel2 && isFileName(str)) {
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        GotoFileAction gotoFile = new GotoFileAction();
        DataContext context = DataManager.getInstance().getDataContext(myTextField);
        gotoFile.actionPerformed(AnActionEvent.createFromAnAction(gotoFile, null, ActionPlaces.UNKNOWN, context));
      });
    }
  }

  private static boolean isFileName(@Nonnull String name) {
    final int index = name.lastIndexOf('.');
    if (index > 0) {
      String ext = name.substring(index + 1);
      if (ext.contains(":")) {
        ext = ext.substring(0, ext.indexOf(':'));
      }
      if (FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(ext) != UnknownFileType.INSTANCE) {
        return true;
      }
    }
    return false;
  }

  public static final String EXTRA_ELEM = "...";

  private class CalcElementsThread extends ReadTask {
    @Nonnull
    private final String myPattern;
    private final boolean myCheckboxState;
    @Nonnull
    private final Consumer<? super Set<?>> myCallback;
    private final ModalityState myModalityState;
    @Nonnull
    private SelectionPolicy mySelectionPolicy;

    private final ProgressIndicator myProgress = new ProgressIndicatorBase();

    CalcElementsThread(@Nonnull String pattern,
                       boolean checkboxState,
                       @Nonnull ModalityState modalityState,
                       @Nonnull SelectionPolicy policy,
                       @Nonnull Consumer<? super Set<?>> callback) {
      myPattern = pattern;
      myCheckboxState = checkboxState;
      myCallback = callback;
      myModalityState = modalityState;
      mySelectionPolicy = policy;
    }

    private final Alarm myShowCardAlarm = new Alarm();
    private final Alarm myUpdateListAlarm = new Alarm();

    void scheduleThread() {
      ApplicationManager.getApplication().assertIsDispatchThread();
      myCalcElementsThread = this;
      showCard(SEARCHING_CARD, 200);
      ProgressIndicatorUtils.scheduleWithWriteActionPriority(myProgress, this);
    }

    @Override
    public Continuation runBackgroundProcess(@Nonnull final ProgressIndicator indicator) {
      if (myProject == null || DumbService.isDumbAware(myModel)) return super.runBackgroundProcess(indicator);

      return DumbService.getInstance(myProject).runReadActionInSmartMode(() -> performInReadAction(indicator));
    }

    @RequiredReadAction
    @Nullable
    @Override
    public Continuation performInReadAction(@Nonnull ProgressIndicator indicator) throws ProcessCanceledException {
      if (isProjectDisposed()) return null;

      Set<Object> elements = Collections.synchronizedSet(new LinkedHashSet<>());
      scheduleIncrementalListUpdate(elements, 0);

      boolean scopeExpanded = populateElements(elements);
      final String cardToShow = elements.isEmpty() ? NOT_FOUND_CARD : scopeExpanded ? NOT_FOUND_IN_PROJECT_CARD : CHECK_BOX_CARD;

      AnchoredSet resultSet = new AnchoredSet(filter(elements));
      return new Continuation(() -> {
        if (!checkDisposed() && !myProgress.isCanceled()) {
          CalcElementsThread currentBgProcess = myCalcElementsThread;
          LOG.assertTrue(currentBgProcess == this, currentBgProcess);

          showCard(cardToShow, 0);

          Set<Object> filtered = resultSet.getElements();
          backgroundCalculationFinished(filtered, mySelectionPolicy);
          myCallback.accept(filtered);
        }
      }, myModalityState);
    }

    private void scheduleIncrementalListUpdate(@Nonnull Set<Object> elements, int lastCount) {
      if (ApplicationManager.getApplication().isUnitTestMode()) return;
      myUpdateListAlarm.addRequest(() -> {
        if (myCalcElementsThread != this || !myProgress.isRunning()) return;

        int count = elements.size();
        if (count > lastCount) {
          setElementsToList(mySelectionPolicy, new ArrayList<>(elements));
          if (currentChosenInfo != null) {
            mySelectionPolicy = PreserveSelection.INSTANCE;
          }
        }
        scheduleIncrementalListUpdate(elements, count);
      }, 200);
    }

    private boolean populateElements(@Nonnull Set<Object> elements) {
      boolean scopeExpanded = false;
      try {
        scopeExpanded = fillWithScopeExpansion(elements, myPattern);

        String lowerCased = patternToLowerCase(myPattern);
        if (elements.isEmpty() && !lowerCased.equals(myPattern)) {
          scopeExpanded = fillWithScopeExpansion(elements, lowerCased);
        }
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error(e);
      }
      return scopeExpanded;
    }

    private boolean fillWithScopeExpansion(@Nonnull Set<Object> elements, @Nonnull String pattern) {
      addElementsByPattern(pattern, elements, myProgress, myCheckboxState);

      if (elements.isEmpty() && !myCheckboxState) {
        addElementsByPattern(pattern, elements, myProgress, true);
        return true;
      }
      return false;
    }

    @Override
    public void onCanceled(@Nonnull ProgressIndicator indicator) {
      LOG.assertTrue(myCalcElementsThread == this, myCalcElementsThread);

      if (!isProjectDisposed() && !checkDisposed()) {
        new CalcElementsThread(myPattern, myCheckboxState, myModalityState, mySelectionPolicy, myCallback).scheduleThread();
      }
    }

    private void addElementsByPattern(@Nonnull String pattern,
                                      @Nonnull final Set<Object> elements,
                                      @Nonnull final ProgressIndicator indicator,
                                      boolean everywhere) {
      long start = System.currentTimeMillis();
      myProvider.filterElements(ChooseByNameBase.this, pattern, everywhere, indicator, o -> {
        if (indicator.isCanceled()) return false;
        if (o == null) {
          LOG.error("Null returned from " + myProvider + " with " + myModel + " in " + ChooseByNameBase.this);
          return true;
        }
        elements.add(o);

        if (isOverflow(elements)) {
          elements.add(EXTRA_ELEM);
          return false;
        }
        return true;
      });
      if (myAlwaysHasMore) {
        elements.add(EXTRA_ELEM);
      }
      if (ContributorsBasedGotoByModel.LOG.isDebugEnabled()) {
        long end = System.currentTimeMillis();
        ContributorsBasedGotoByModel.LOG.debug("addElementsByPattern(" + pattern + "): " + (end - start) + "ms; " + elements.size() + " elements");
      }
    }

    private void showCard(@Nonnull String card, final int delay) {
      if (ApplicationManager.getApplication().isUnitTestMode()) return;
      myShowCardAlarm.cancelAllRequests();
      myShowCardAlarm.addRequest(() -> {
        if (!myProgress.isCanceled()) {
          myCard.show(myCardContainer, card);
        }
      }, delay, myModalityState);
    }

    protected boolean isOverflow(@Nonnull Set<Object> elementsArray) {
      return elementsArray.size() >= myMaximumListSizeLimit;
    }

    private void cancel() {
      ApplicationManager.getApplication().assertIsDispatchThread();
      myProgress.cancel();
    }

  }

  private boolean isProjectDisposed() {
    return myProject != null && myProject.isDisposed();
  }

  @Nonnull
  private static String patternToLowerCase(@Nonnull String pattern) {
    return StringUtil.toLowerCase(pattern);
  }

  @Override
  public boolean canShowListForEmptyPattern() {
    return isShowListForEmptyPattern() || isShowListAfterCompletionKeyStroke() && lastKeyStrokeIsCompletion();
  }

  private boolean lastKeyStrokeIsCompletion() {
    return myTextField.isCompletionKeyStroke();
  }

  @Nonnull
  private static Matcher buildPatternMatcher(@Nonnull String pattern) {
    return NameUtil.buildMatcher(pattern, NameUtil.MatchingCaseSensitivity.NONE);
  }

  private static class HintLabel extends JLabel {
    private HintLabel(@Nonnull String text) {
      super(text, RIGHT);
      setForeground(JBColor.DARK_GRAY);
    }
  }

  @Override
  public int getMaximumListSizeLimit() {
    return myMaximumListSizeLimit;
  }

  public void setMaximumListSizeLimit(final int maximumListSizeLimit) {
    myMaximumListSizeLimit = maximumListSizeLimit;
  }

  public void setListSizeIncreasing(final int listSizeIncreasing) {
    myListSizeIncreasing = listSizeIncreasing;
  }

  /**
   * Display <tt>...</tt> item at the end of the list regardless of whether it was filled up or not.
   * This option can be useful in cases, when it can't be said beforehand, that the next call to {@link ChooseByNameItemProvider}
   * won't give new items.
   */
  public void setAlwaysHasMore(boolean enabled) {
    myAlwaysHasMore = enabled;
  }

  private static final String ACTION_NAME = "Show All in View";

  private abstract class ShowFindUsagesAction extends DumbAwareAction {
    ShowFindUsagesAction() {
      super(ACTION_NAME, ACTION_NAME, AllIcons.General.Pin_tab);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull final AnActionEvent e) {
      cancelListUpdater();

      final UsageViewPresentation presentation = new UsageViewPresentation();
      final String text = getTrimmedText();
      final String prefixPattern = myFindUsagesTitle + " \'" + text + "\'";
      presentation.setCodeUsagesString(prefixPattern);
      presentation.setUsagesInGeneratedCodeString(prefixPattern + " in generated code");
      presentation.setTabName(prefixPattern);
      presentation.setTabText(prefixPattern);
      presentation.setTargetsNodeText("Unsorted " + StringUtil.toLowerCase(patternToLowerCase(prefixPattern)));
      PsiElement[] elements = getElements();
      final List<PsiElement> targets = new ArrayList<>();
      final Set<Usage> usages = new LinkedHashSet<>();
      fillUsages(Arrays.asList(elements), usages, targets);
      if (myListModel.contains(EXTRA_ELEM)) { //start searching for the rest
        final boolean everywhere = myCheckBox.isSelected();
        hideHint();
        final Set<Object> collected = new LinkedHashSet<>();
        ProgressManager.getInstance().run(new Task.Modal(myProject, prefixPattern, true) {
          private ChooseByNameBase.CalcElementsThread myCalcUsagesThread;

          @Override
          public void run(@Nonnull final ProgressIndicator indicator) {
            ensureNamesLoaded(everywhere);
            indicator.setIndeterminate(true);
            final TooManyUsagesStatus tooManyUsagesStatus = TooManyUsagesStatus.createFor(indicator);
            myCalcUsagesThread = new CalcElementsThread(text, everywhere, IdeaModalityState.nonModal(), PreserveSelection.INSTANCE, __ -> {
            }) {
              @Override
              protected boolean isOverflow(@Nonnull Set<Object> elementsArray) {
                tooManyUsagesStatus.pauseProcessingIfTooManyUsages();
                if (elementsArray.size() > UsageLimitUtil.USAGES_LIMIT - myMaximumListSizeLimit && tooManyUsagesStatus.switchTooManyUsagesStatus()) {
                  int usageCount = elementsArray.size() + myMaximumListSizeLimit;
                  UsageViewManagerImpl.showTooManyUsagesWarningLater((Project)getProject(),
                                                                     tooManyUsagesStatus,
                                                                     indicator,
                                                                     presentation,
                                                                     usageCount,
                                                                     null);
                }
                return false;
              }
            };

            ApplicationManager.getApplication().runReadAction(() -> {
              myCalcUsagesThread.addElementsByPattern(text, collected, indicator, everywhere);

              indicator.setText("Prepare...");
              fillUsages(collected, usages, targets);
            });
          }

          @RequiredUIAccess
          @Override
          public void onSuccess() {
            showUsageView(targets, usages, presentation);
          }

          @RequiredUIAccess
          @Override
          public void onCancel() {
            myCalcUsagesThread.cancel();
          }

          @Override
          public void onThrowable(@Nonnull Throwable error) {
            super.onThrowable(error);
            myCalcUsagesThread.cancel();
          }
        });
      }
      else {
        hideHint();
        showUsageView(targets, usages, presentation);
      }
    }

    private void fillUsages(@Nonnull Collection<Object> matchElementsArray,
                            @Nonnull Collection<? super Usage> usages,
                            @Nonnull List<? super PsiElement> targets) {
      for (Object o : matchElementsArray) {
        if (o instanceof PsiElement) {
          PsiElement element = (PsiElement)o;
          if (element.getTextRange() != null) {
            usages.add(new MyUsageInfo2UsageAdapter(element, false));
          }
          else {
            targets.add(element);
          }
        }
      }
    }

    private void showUsageView(@Nonnull List<? extends PsiElement> targets,
                               @Nonnull Collection<? extends Usage> usages,
                               @Nonnull UsageViewPresentation presentation) {
      UsageTarget[] usageTargets =
        targets.isEmpty() ? UsageTarget.EMPTY_ARRAY : PsiElement2UsageTargetAdapter.convert(PsiUtilCore.toPsiElementArray(targets));
      UsageViewManager.getInstance(myProject).showUsages(usageTargets, usages.toArray(Usage.EMPTY_ARRAY), presentation);
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
      if (myFindUsagesTitle == null || myProject == null) {
        e.getPresentation().setVisible(false);
        return;
      }
      PsiElement[] elements = getElements();
      e.getPresentation().setEnabled(elements.length > 0);
    }

    @Nonnull
    public abstract PsiElement[] getElements();
  }

  private static class MyUsageInfo2UsageAdapter extends UsageInfo2UsageAdapter {
    private final PsiElement myElement;
    private final boolean mySeparateGroup;

    MyUsageInfo2UsageAdapter(@Nonnull PsiElement element, boolean separateGroup) {
      super(new UsageInfo(element) {
        @Override
        public boolean isDynamicUsage() {
          return separateGroup || super.isDynamicUsage();
        }
      });
      myElement = element;
      mySeparateGroup = separateGroup;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MyUsageInfo2UsageAdapter)) return false;

      MyUsageInfo2UsageAdapter adapter = (MyUsageInfo2UsageAdapter)o;

      if (mySeparateGroup != adapter.mySeparateGroup) return false;
      if (!myElement.equals(adapter.myElement)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myElement.hashCode();
      result = 31 * result + (mySeparateGroup ? 1 : 0);
      return result;
    }
  }

  @Nonnull
  public JTextField getTextField() {
    return myTextField;
  }

  private class MyCopyReferenceAction extends DumbAwareAction {
    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(myTextField.getSelectedText() == null && getChosenElement() instanceof PsiElement);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      CopyReferenceAction.doCopy((PsiElement)getChosenElement(), myProject);
    }
  }
}
