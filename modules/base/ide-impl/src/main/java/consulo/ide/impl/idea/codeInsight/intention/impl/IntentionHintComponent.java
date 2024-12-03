// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.language.editor.CodeInsightBundle;
import consulo.ide.impl.idea.codeInsight.hint.*;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.hint.QuestionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.impl.internal.intention.IntentionManagerSettings;
import consulo.language.editor.refactoring.unwrap.ScopeHighlighter;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.application.AllIcons;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.dataContext.DataProvider;
import consulo.project.Project;
import consulo.application.util.registry.Registry;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.editor.refactoring.action.BaseRefactoringIntentionAction;
import consulo.ui.ex.awt.HintHint;
import consulo.ide.impl.idea.ui.LightweightHint;
import consulo.ide.impl.idea.ui.PopupMenuListenerAdapter;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBLabel;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.ThreeState;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 * @author Mike
 * @author Valentin
 * @author Eugene Belyaev
 * @author Konstantin Bulenkov
 */
public class IntentionHintComponent implements Disposable, ScrollAwareHint {
  private static final Logger LOG = Logger.getInstance(IntentionHintComponent.class);

  private static final Image ourInactiveArrowIcon = Image.empty(AllIcons.General.ArrowDown.getWidth(), AllIcons.General.ArrowDown.getHeight());

  private static final int NORMAL_BORDER_SIZE = 6;
  private static final int SMALL_BORDER_SIZE = 4;

  private static final Border INACTIVE_BORDER = BorderFactory.createEmptyBorder(NORMAL_BORDER_SIZE, NORMAL_BORDER_SIZE, NORMAL_BORDER_SIZE, NORMAL_BORDER_SIZE);
  private static final Border INACTIVE_BORDER_SMALL = BorderFactory.createEmptyBorder(SMALL_BORDER_SIZE, SMALL_BORDER_SIZE, SMALL_BORDER_SIZE, SMALL_BORDER_SIZE);

  @TestOnly
  public CachedIntentions getCachedIntentions() {
    return myCachedIntentions;
  }

  private final CachedIntentions myCachedIntentions;

  private static Border createActiveBorder() {
    return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(TargetAWT.to(getBorderColor()), 1),
                                              BorderFactory.createEmptyBorder(NORMAL_BORDER_SIZE - 1, NORMAL_BORDER_SIZE - 1, NORMAL_BORDER_SIZE - 1, NORMAL_BORDER_SIZE - 1));
  }

  private static Border createActiveBorderSmall() {
    return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(TargetAWT.to(getBorderColor()), 1),
                                              BorderFactory.createEmptyBorder(SMALL_BORDER_SIZE - 1, SMALL_BORDER_SIZE - 1, SMALL_BORDER_SIZE - 1, SMALL_BORDER_SIZE - 1));
  }

  private static ColorValue getBorderColor() {
    return EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.SELECTED_TEARLINE_COLOR);
  }

  public boolean isVisible() {
    return myPanel.isVisible();
  }

  private final Editor myEditor;

  private static final Alarm myAlarm = new Alarm();

  private final Image myHighlightedIcon;
  private final JLabel myIconLabel;

  private final Image myInactiveIcon;

  private static final int DELAY = 500;
  private final MyComponentHint myComponentHint;
  private volatile boolean myPopupShown;
  private boolean myDisposed;
  private volatile ListPopup myPopup;
  private final PsiFile myFile;
  private final JPanel myPanel = new JPanel() {
    @Override
    public synchronized void addMouseListener(MouseListener l) {
      // avoid this (transparent) panel consuming mouse click events
    }
  };

  private PopupMenuListener myOuterComboboxPopupListener;

  @Nonnull
  public static IntentionHintComponent showIntentionHint(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull Editor editor, boolean showExpanded, @Nonnull CachedIntentions cachedIntentions) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final IntentionHintComponent component = new IntentionHintComponent(project, file, editor, cachedIntentions);

    if (editor.getSettings().isShowIntentionBulb()) {
      component.showIntentionHintImpl(!showExpanded);
    }
    if (showExpanded) {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (!editor.isDisposed() && editor.getComponent().isShowing()) {
          component.showPopup(false);
        }
      }, project.getDisposed());
    }

    return component;
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public void dispose() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myDisposed = true;
    myComponentHint.hide();
    myPanel.hide();

    if (myOuterComboboxPopupListener != null) {
      final Container ancestor = SwingUtilities.getAncestorOfClass(JComboBox.class, myEditor.getContentComponent());
      if (ancestor != null) {
        ((JComboBox)ancestor).removePopupMenuListener(myOuterComboboxPopupListener);
      }

      myOuterComboboxPopupListener = null;
    }
  }

  @Override
  public void editorScrolled() {
    closePopup();
  }

  public boolean isForEditor(@Nonnull Editor editor) {
    return editor == myEditor;
  }


  public enum PopupUpdateResult {
    NOTHING_CHANGED,    // intentions did not change
    CHANGED_INVISIBLE,  // intentions changed but the popup has not been shown yet, so can recreate list silently
    HIDE_AND_RECREATE   // ahh, has to close already shown popup, recreate and re-show again
  }

  @Nonnull
  public PopupUpdateResult getPopupUpdateResult(boolean actionsChanged) {
    if (myPopup.isDisposed() || !myFile.isValid()) {
      return PopupUpdateResult.HIDE_AND_RECREATE;
    }
    if (!actionsChanged) {
      return PopupUpdateResult.NOTHING_CHANGED;
    }
    return myPopupShown ? PopupUpdateResult.HIDE_AND_RECREATE : PopupUpdateResult.CHANGED_INVISIBLE;
  }

  public void recreate() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    ListPopupStep step = myPopup.getListStep();
    recreateMyPopup(step);
  }

  @Nullable
  @TestOnly
  public IntentionAction getAction(int index) {
    if (myPopup == null || myPopup.isDisposed()) {
      return null;
    }
    List<IntentionActionWithTextCaching> values = myCachedIntentions.getAllActions();
    if (values.size() <= index) {
      return null;
    }
    return values.get(index).getAction();
  }

  private void showIntentionHintImpl(final boolean delay) {
    final int offset = myEditor.getCaretModel().getOffset();

    myComponentHint.setShouldDelay(delay);

    HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();

    QuestionAction action = new PriorityQuestionAction() {
      @Override
      public boolean execute() {
        showPopup(false);
        return true;
      }

      @Override
      public int getPriority() {
        return -10;
      }
    };
    if (hintManager.canShowQuestionAction(action)) {
      Point position = getHintPosition(myEditor);
      if (position != null) {
        hintManager.showQuestionHint(myEditor, position, offset, offset, myComponentHint, action, HintManager.ABOVE);
      }
    }
  }

  @Nullable
  private static Point getHintPosition(Editor editor) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return new Point();
    final int offset = editor.getCaretModel().getOffset();
    final VisualPosition pos = editor.offsetToVisualPosition(offset);
    int line = pos.line;

    final Point position = editor.visualPositionToXY(new VisualPosition(line, 0));
    LOG.assertTrue(editor.getComponent().isDisplayable());

    JComponent convertComponent = editor.getContentComponent();

    Point realPoint;
    final boolean oneLineEditor = editor.isOneLineMode();
    if (oneLineEditor) {
      // place bulb at the corner of the surrounding component
      final JComponent contentComponent = editor.getContentComponent();
      Container ancestorOfClass = SwingUtilities.getAncestorOfClass(JComboBox.class, contentComponent);

      if (ancestorOfClass != null) {
        convertComponent = (JComponent)ancestorOfClass;
      }
      else {
        ancestorOfClass = SwingUtilities.getAncestorOfClass(JTextField.class, contentComponent);
        if (ancestorOfClass != null) {
          convertComponent = (JComponent)ancestorOfClass;
        }
      }

      realPoint = new Point(-(Image.DEFAULT_ICON_SIZE / 2) - 4, -(Image.DEFAULT_ICON_SIZE / 2));
    }
    else {
      Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
      if (position.y < visibleArea.y || position.y >= visibleArea.y + visibleArea.height) return null;

      // try to place bulb on the same line
      int yShift = -(NORMAL_BORDER_SIZE + Image.DEFAULT_ICON_SIZE);
      if (canPlaceBulbOnTheSameLine(editor)) {
        yShift = -(NORMAL_BORDER_SIZE + (Image.DEFAULT_ICON_SIZE - editor.getLineHeight()) / 2 + 3);
      }
      else if (position.y < visibleArea.y + editor.getLineHeight()) {
        yShift = editor.getLineHeight() - NORMAL_BORDER_SIZE;
      }

      final int xShift = Image.DEFAULT_ICON_SIZE;

      realPoint = new Point(Math.max(0, visibleArea.x - xShift), position.y + yShift);
    }

    Point location = SwingUtilities.convertPoint(convertComponent, realPoint, editor.getComponent().getRootPane().getLayeredPane());
    return new Point(location.x, location.y);
  }

  private static boolean canPlaceBulbOnTheSameLine(Editor editor) {
    if (ApplicationManager.getApplication().isUnitTestMode() || editor.isOneLineMode()) return false;
    if (Registry.is("always.show.intention.above.current.line", false)) return false;
    final int offset = editor.getCaretModel().getOffset();
    final VisualPosition pos = editor.offsetToVisualPosition(offset);
    int line = pos.line;

    final int firstNonSpaceColumnOnTheLine = EditorActionUtil.findFirstNonSpaceColumnOnTheLine(editor, line);
    if (firstNonSpaceColumnOnTheLine == -1) return false;
    final Point point = editor.visualPositionToXY(new VisualPosition(line, firstNonSpaceColumnOnTheLine));
    return point.x > Image.DEFAULT_ICON_SIZE + (editor.isOneLineMode() ? SMALL_BORDER_SIZE : NORMAL_BORDER_SIZE) * 2;
  }

  private IntentionHintComponent(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull final Editor editor, @Nonnull CachedIntentions cachedIntentions) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myFile = file;
    myEditor = editor;
    myCachedIntentions = cachedIntentions;
    myPanel.setLayout(new BorderLayout());
    myPanel.setOpaque(false);

    boolean showRefactoringsBulb = ContainerUtil.exists(cachedIntentions.getInspectionFixes(), descriptor -> descriptor.getAction() instanceof BaseRefactoringIntentionAction);
    boolean showFix = !showRefactoringsBulb && ContainerUtil.exists(cachedIntentions.getErrorFixes(), descriptor -> IntentionManagerSettings.getInstance().isShowLightBulb(descriptor.getAction()));

    Image smartTagIcon = showRefactoringsBulb ? AllIcons.Actions.RefactoringBulb : showFix ? AllIcons.Actions.QuickfixBulb : AllIcons.Actions.IntentionBulb;

    myHighlightedIcon = ImageEffects.appendRight(smartTagIcon, AllIcons.General.ArrowDown);
    myInactiveIcon = ImageEffects.appendRight(smartTagIcon, ourInactiveArrowIcon);

    myIconLabel = new JBLabel(myInactiveIcon);
    myIconLabel.setOpaque(false);

    myPanel.add(myIconLabel, BorderLayout.CENTER);

    myPanel.setBorder(editor.isOneLineMode() ? INACTIVE_BORDER_SMALL : INACTIVE_BORDER);

    myIconLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(@Nonnull MouseEvent e) {
        if (!e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON1) {
          AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
          AnActionEvent event = AnActionEvent.createFromInputEvent(e, ActionPlaces.MOUSE_SHORTCUT, new Presentation(), SimpleDataContext.getProjectContext(project));
          //ActionsCollector.getInstance().record(project, action, event, file.getLanguage());

          showPopup(true);
        }
      }

      @Override
      public void mouseEntered(@Nonnull MouseEvent e) {
        onMouseEnter(editor.isOneLineMode());
      }

      @Override
      public void mouseExited(@Nonnull MouseEvent e) {
        onMouseExit(editor.isOneLineMode());
      }
    });

    myComponentHint = new MyComponentHint(myPanel);
    ListPopupStep step = new IntentionListStep(this, myEditor, myFile, project, myCachedIntentions);
    recreateMyPopup(step);
    EditorUtil.disposeWithEditor(myEditor, this);
    //DynamicPlugins.onPluginUnload(this, () -> Disposer.dispose(this));
  }

  public void hide() {
    myDisposed = true;
    Disposer.dispose(this);
  }

  private void onMouseExit(final boolean small) {
    if (!myPopup.isVisible()) {
      myIconLabel.setIcon(TargetAWT.to(myInactiveIcon));
      myPanel.setBorder(small ? INACTIVE_BORDER_SMALL : INACTIVE_BORDER);
    }
  }

  private void onMouseEnter(final boolean small) {
    myIconLabel.setIcon(TargetAWT.to(myHighlightedIcon));
    myPanel.setBorder(small ? createActiveBorderSmall() : createActiveBorder());

    String acceleratorsText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
    if (!acceleratorsText.isEmpty()) {
      myIconLabel.setToolTipText(CodeInsightBundle.message("lightbulb.tooltip", acceleratorsText));
    }
  }

  @TestOnly
  public LightweightHint getComponentHint() {
    return myComponentHint;
  }

  private void closePopup() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myPopup.cancel();
    myPopupShown = false;
  }

  private void showPopup(boolean mouseClick) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myPopup == null || myPopup.isDisposed() || myPopupShown) return;

    if (mouseClick && myPanel.isShowing()) {
      final RelativePoint swCorner = RelativePoint.getSouthWestOf(myPanel);
      final int yOffset = canPlaceBulbOnTheSameLine(myEditor) ? 0 : myEditor.getLineHeight() - (myEditor.isOneLineMode() ? SMALL_BORDER_SIZE : NORMAL_BORDER_SIZE);
      myPopup.show(new RelativePoint(swCorner.getComponent(), new Point(swCorner.getPoint().x, swCorner.getPoint().y + yOffset)));
    }
    else {
      myEditor.showPopupInBestPositionFor(myPopup);
    }

    myPopupShown = true;
  }

  private void recreateMyPopup(@Nonnull ListPopupStep step) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myPopup != null) {
      Disposer.dispose(myPopup);
    }
    myPopup = JBPopupFactory.getInstance().createListPopup(step);
    if (myPopup instanceof WizardPopup) {
      Shortcut[] shortcuts = KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS).getShortcuts();
      for (Shortcut shortcut : shortcuts) {
        if (shortcut instanceof KeyboardShortcut) {
          KeyboardShortcut keyboardShortcut = (KeyboardShortcut)shortcut;
          if (keyboardShortcut.getSecondKeyStroke() == null) {
            ((WizardPopup)myPopup).registerAction("activateSelectedElement", keyboardShortcut.getFirstKeyStroke(), new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                myPopup.handleSelect(true);
              }
            });
          }
        }
      }
    }

    boolean committed = PsiDocumentManager.getInstance(myFile.getProject()).isCommitted(myEditor.getDocument());
    final PsiFile injectedFile = committed ? InjectedLanguageUtil.findInjectedPsiNoCommit(myFile, myEditor.getCaretModel().getOffset()) : null;
    final Editor injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(myEditor, injectedFile);

    final ScopeHighlighter highlighter = new ScopeHighlighter(myEditor);
    final ScopeHighlighter injectionHighlighter = new ScopeHighlighter(injectedEditor);

    myPopup.addListener(new JBPopupListener() {
      @Override
      public void onClosed(@Nonnull LightweightWindowEvent event) {
        highlighter.dropHighlight();
        injectionHighlighter.dropHighlight();
        myPopupShown = false;
      }
    });
    myPopup.addListSelectionListener(e -> {
      final Object source = e.getSource();
      highlighter.dropHighlight();
      injectionHighlighter.dropHighlight();

      if (source instanceof DataProvider) {
        final Object selectedItem = ((DataProvider)source).getData(PlatformDataKeys.SELECTED_ITEM);
        if (selectedItem instanceof IntentionActionWithTextCaching) {
          IntentionAction action = IntentionActionDelegate.unwrap(((IntentionActionWithTextCaching)selectedItem).getAction());
          if (action instanceof SuppressIntentionActionFromFix) {
            if (injectedFile != null && ((SuppressIntentionActionFromFix)action).isShouldBeAppliedToInjectionHost() == ThreeState.NO) {
              final PsiElement at = injectedFile.findElementAt(injectedEditor.getCaretModel().getOffset());
              final PsiElement container = ((SuppressIntentionActionFromFix)action).getContainer(at);
              if (container != null) {
                injectionHighlighter.highlight(container, Collections.singletonList(container));
              }
            }
            else {
              final PsiElement at = myFile.findElementAt(myEditor.getCaretModel().getOffset());
              final PsiElement container = ((SuppressIntentionActionFromFix)action).getContainer(at);
              if (container != null) {
                highlighter.highlight(container, Collections.singletonList(container));
              }
            }
          }
        }
      }
    });

    if (myEditor.isOneLineMode()) {
      // hide popup on combobox popup show
      final Container ancestor = SwingUtilities.getAncestorOfClass(JComboBox.class, myEditor.getContentComponent());
      if (ancestor != null) {
        final JComboBox comboBox = (JComboBox)ancestor;
        myOuterComboboxPopupListener = new PopupMenuListenerAdapter() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            hide();
          }
        };

        comboBox.addPopupMenuListener(myOuterComboboxPopupListener);
      }
    }

    Disposer.register(this, myPopup);
    Disposer.register(myPopup, ApplicationManager.getApplication()::assertIsDispatchThread);
  }

  void canceled(@Nonnull ListPopupStep intentionListStep) {
    if (myPopup.getListStep() != intentionListStep || myDisposed) {
      return;
    }
    // Root canceled. Create new popup. This one cannot be reused.
    recreateMyPopup(intentionListStep);
  }

  private static class MyComponentHint extends LightweightHint {
    private boolean myVisible;
    private boolean myShouldDelay;

    private MyComponentHint(JComponent component) {
      super(component);
    }

    @Override
    public void show(@Nonnull final JComponent parentComponent, final int x, final int y, final JComponent focusBackComponent, @Nonnull HintHint hintHint) {
      myVisible = true;
      if (myShouldDelay) {
        myAlarm.cancelAllRequests();
        myAlarm.addRequest(() -> showImpl(parentComponent, x, y, focusBackComponent), DELAY);
      }
      else {
        showImpl(parentComponent, x, y, focusBackComponent);
      }
    }

    private void showImpl(JComponent parentComponent, int x, int y, JComponent focusBackComponent) {
      if (!parentComponent.isShowing()) return;
      super.show(parentComponent, x, y, focusBackComponent, new HintHint(parentComponent, new Point(x, y)));
    }

    @Override
    public void hide() {
      super.hide();
      myVisible = false;
      myAlarm.cancelAllRequests();
    }

    @Override
    public boolean isVisible() {
      return myVisible || super.isVisible();
    }

    private void setShouldDelay(boolean shouldDelay) {
      myShouldDelay = shouldDelay;
    }
  }
}
