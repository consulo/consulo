// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.colorScheme.EditorColorsManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.hint.PriorityQuestionAction;
import consulo.ide.impl.idea.codeInsight.hint.ScrollAwareHint;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.hint.QuestionAction;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionActionWithTextCaching;
import consulo.language.editor.internal.intention.IntentionManagerSettings;
import consulo.language.editor.refactoring.action.BaseRefactoringIntentionAction;
import consulo.language.editor.refactoring.unwrap.ScopeHighlighter;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.RoundedLineBorder;
import consulo.ui.ex.awt.event.PopupMenuListenerAdapter;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

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

    private static Border createBaseBorder() {
        int arc = UIManager.getInt("Component.arc");
        Border lineBorder;
        if (arc > 0) {
            lineBorder = new RoundedLineBorder(TargetAWT.to(getBorderColor()), arc, 1);
        }
        else {
            lineBorder = BorderFactory.createLineBorder(TargetAWT.to(getBorderColor()), 1);
        }
        return lineBorder;
    }

    private static Border createActiveBorder() {
        return BorderFactory.createCompoundBorder(createBaseBorder(),
            BorderFactory.createEmptyBorder(NORMAL_BORDER_SIZE - 1, NORMAL_BORDER_SIZE - 1, NORMAL_BORDER_SIZE - 1, NORMAL_BORDER_SIZE - 1));
    }

    private static Border createActiveBorderSmall() {
        return BorderFactory.createCompoundBorder(createBaseBorder(),
            BorderFactory.createEmptyBorder(SMALL_BORDER_SIZE - 1, SMALL_BORDER_SIZE - 1, SMALL_BORDER_SIZE - 1, SMALL_BORDER_SIZE - 1));
    }

    private static ColorValue getBorderColor() {
        return EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.SELECTED_TEARLINE_COLOR);
    }

    public boolean isVisible() {
        return myPanel.isVisible();
    }

    private final Editor myEditor;

    private static final Alarm ourAlarm = new Alarm();

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
    @RequiredUIAccess
    public static IntentionHintComponent showIntentionHint(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull Editor editor, boolean showExpanded, @Nonnull CachedIntentions cachedIntentions) {
        UIAccess.assertIsUIThread();

        IntentionHintComponent component = new IntentionHintComponent(project, file, editor, cachedIntentions);

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
    @RequiredUIAccess
    public void dispose() {
        UIAccess.assertIsUIThread();
        myDisposed = true;
        myComponentHint.hide();
        myPanel.hide();

        if (myOuterComboboxPopupListener != null) {
            Container ancestor = SwingUtilities.getAncestorOfClass(JComboBox.class, myEditor.getContentComponent());
            if (ancestor != null) {
                ((JComboBox) ancestor).removePopupMenuListener(myOuterComboboxPopupListener);
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

    @RequiredUIAccess
    public void recreate() {
        UIAccess.assertIsUIThread();
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

    @RequiredUIAccess
    private void showIntentionHintImpl(boolean delay) {
        int offset = myEditor.getCaretModel().getOffset();

        myComponentHint.setShouldDelay(delay);

        HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();

        QuestionAction action = new PriorityQuestionAction() {
            @Override
            @RequiredUIAccess
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
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return new Point();
        }
        int offset = editor.getCaretModel().getOffset();
        VisualPosition pos = editor.offsetToVisualPosition(offset);
        int line = pos.line;

        Point position = editor.visualPositionToXY(new VisualPosition(line, 0));
        LOG.assertTrue(editor.getComponent().isDisplayable());

        JComponent convertComponent = editor.getContentComponent();

        Point realPoint;
        boolean oneLineEditor = editor.isOneLineMode();
        if (oneLineEditor) {
            // place bulb at the corner of the surrounding component
            JComponent contentComponent = editor.getContentComponent();
            Container ancestorOfClass = SwingUtilities.getAncestorOfClass(JComboBox.class, contentComponent);

            if (ancestorOfClass != null) {
                convertComponent = (JComponent) ancestorOfClass;
            }
            else {
                ancestorOfClass = SwingUtilities.getAncestorOfClass(JTextField.class, contentComponent);
                if (ancestorOfClass != null) {
                    convertComponent = (JComponent) ancestorOfClass;
                }
            }

            realPoint = new Point(-(Image.DEFAULT_ICON_SIZE / 2) - 4, -(Image.DEFAULT_ICON_SIZE / 2));
        }
        else {
            Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
            if (position.y < visibleArea.y || position.y >= visibleArea.y + visibleArea.height) {
                return null;
            }

            // try to place bulb on the same line
            int yShift = -(NORMAL_BORDER_SIZE + Image.DEFAULT_ICON_SIZE);
            if (canPlaceBulbOnTheSameLine(editor)) {
                yShift = -(NORMAL_BORDER_SIZE + (Image.DEFAULT_ICON_SIZE - editor.getLineHeight()) / 2 + 3);
            }
            else if (position.y < visibleArea.y + editor.getLineHeight()) {
                yShift = editor.getLineHeight() - NORMAL_BORDER_SIZE;
            }

            int xShift = Image.DEFAULT_ICON_SIZE;

            realPoint = new Point(Math.max(0, visibleArea.x - xShift), position.y + yShift);
        }

        Point location = SwingUtilities.convertPoint(convertComponent, realPoint, editor.getComponent().getRootPane().getLayeredPane());
        return new Point(location.x, location.y);
    }

    private static boolean canPlaceBulbOnTheSameLine(Editor editor) {
        if (ApplicationManager.getApplication().isUnitTestMode() || editor.isOneLineMode()) {
            return false;
        }
        if (Registry.is("always.show.intention.above.current.line", false)) {
            return false;
        }
        int offset = editor.getCaretModel().getOffset();
        VisualPosition pos = editor.offsetToVisualPosition(offset);
        int line = pos.line;

        int firstNonSpaceColumnOnTheLine = EditorActionUtil.findFirstNonSpaceColumnOnTheLine(editor, line);
        if (firstNonSpaceColumnOnTheLine == -1) {
            return false;
        }
        Point point = editor.visualPositionToXY(new VisualPosition(line, firstNonSpaceColumnOnTheLine));
        return point.x > Image.DEFAULT_ICON_SIZE + (editor.isOneLineMode() ? SMALL_BORDER_SIZE : NORMAL_BORDER_SIZE) * 2;
    }

    @RequiredUIAccess
    private IntentionHintComponent(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull final Editor editor, @Nonnull CachedIntentions cachedIntentions) {
        UIAccess.assertIsUIThread();

        myFile = file;
        myEditor = editor;
        myCachedIntentions = cachedIntentions;
        myPanel.setLayout(new BorderLayout());
        myPanel.setOpaque(false);

        boolean showRefactoringsBulb = ContainerUtil.exists(cachedIntentions.getInspectionFixes(), descriptor -> descriptor.getAction() instanceof BaseRefactoringIntentionAction);
        boolean showFix = !showRefactoringsBulb && ContainerUtil.exists(cachedIntentions.getErrorFixes(), descriptor -> IntentionManagerSettings.getInstance().isShowLightBulb(descriptor.getAction()));

        Image smartTagIcon;
        if (showRefactoringsBulb) {
            smartTagIcon = AllIcons.Actions.RefactoringBulb;
        }
        else if (showFix) {
            smartTagIcon = AllIcons.Actions.QuickfixBulb;
        }
        else {
            smartTagIcon = AllIcons.Actions.IntentionBulb;
        }

        myHighlightedIcon = ImageEffects.appendRight(smartTagIcon, AllIcons.General.ArrowDown);
        myInactiveIcon = ImageEffects.appendRight(smartTagIcon, ourInactiveArrowIcon);

        myIconLabel = new JBLabel(myInactiveIcon);
        myIconLabel.setOpaque(false);

        myPanel.add(myIconLabel, BorderLayout.CENTER);

        myPanel.setBorder(editor.isOneLineMode() ? INACTIVE_BORDER_SMALL : INACTIVE_BORDER);

        myIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            @RequiredUIAccess
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
    }

    public void hide() {
        myDisposed = true;
        Disposer.dispose(this);
    }

    private void onMouseExit(boolean small) {
        if (!myPopup.isVisible()) {
            myIconLabel.setIcon(TargetAWT.to(myInactiveIcon));
            myPanel.setBorder(small ? INACTIVE_BORDER_SMALL : INACTIVE_BORDER);
        }
    }

    private void onMouseEnter(boolean small) {
        myIconLabel.setIcon(TargetAWT.to(myHighlightedIcon));
        myPanel.setBorder(small ? createActiveBorderSmall() : createActiveBorder());

        String acceleratorsText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
        if (!acceleratorsText.isEmpty()) {
            myIconLabel.setToolTipText(CodeInsightBundle.message("lightbulb.tooltip", acceleratorsText));
        }
    }

    @TestOnly
    public LightweightHintImpl getComponentHint() {
        return myComponentHint;
    }

    @RequiredUIAccess
    private void closePopup() {
        UIAccess.assertIsUIThread();

        myPopup.cancel();
        myPopupShown = false;
    }

    @RequiredUIAccess
    private void showPopup(boolean mouseClick) {
        UIAccess.assertIsUIThread();

        if (myPopup == null || myPopup.isDisposed() || myPopupShown) {
            return;
        }

        if (mouseClick && myPanel.isShowing()) {
            RelativePoint swCorner = RelativePoint.getSouthWestOf(myPanel);
            int yOffset = canPlaceBulbOnTheSameLine(myEditor) ? 0 : myEditor.getLineHeight() - (myEditor.isOneLineMode() ? SMALL_BORDER_SIZE : NORMAL_BORDER_SIZE);
            myPopup.show(new RelativePoint(swCorner.getComponent(), new Point(swCorner.getPoint().x, swCorner.getPoint().y + yOffset)));
        }
        else {
            myEditor.showPopupInBestPositionFor(myPopup);
        }

        myPopupShown = true;
    }

    @RequiredUIAccess
    private void recreateMyPopup(@Nonnull ListPopupStep step) {
        UIAccess.assertIsUIThread();

        if (myPopup != null) {
            Disposer.dispose(myPopup);
        }
        myPopup = JBPopupFactory.getInstance().createListPopup(step);
        if (myPopup instanceof WizardPopup) {
            Shortcut[] shortcuts = KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS).getShortcuts();
            for (Shortcut shortcut : shortcuts) {
                if (shortcut instanceof KeyboardShortcut) {
                    KeyboardShortcut keyboardShortcut = (KeyboardShortcut) shortcut;
                    if (keyboardShortcut.getSecondKeyStroke() == null) {
                        ((WizardPopup) myPopup).registerAction("activateSelectedElement", keyboardShortcut.getFirstKeyStroke(), new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                myPopup.handleSelect(true);
                            }
                        });
                    }
                }
            }
        }

        Project project = myFile.getProject();
        boolean committed = PsiDocumentManager.getInstance(project).isCommitted(myEditor.getDocument());
        PsiFile injectedFile = committed ? InjectedLanguageManager.getInstance(project).findInjectedPsiNoCommit(myFile, myEditor.getCaretModel().getOffset()) : null;
        Editor injectedEditor = InjectedEditorManager.getInstance(project).getInjectedEditorForInjectedFile(myEditor, injectedFile);

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
            Object source = e.getSource();
            highlighter.dropHighlight();
            injectionHighlighter.dropHighlight();

            if (source instanceof DataProvider) {
                Object selectedItem = ((DataProvider) source).getData(PlatformDataKeys.SELECTED_ITEM);
                if (selectedItem instanceof IntentionActionWithTextCaching) {
                    IntentionAction action = IntentionActionDelegate.unwrap(((IntentionActionWithTextCaching) selectedItem).getAction());
                    if (action instanceof SuppressIntentionActionFromFix) {
                        if (injectedFile != null && ((SuppressIntentionActionFromFix) action).isShouldBeAppliedToInjectionHost() == ThreeState.NO) {
                            PsiElement at = injectedFile.findElementAt(injectedEditor.getCaretModel().getOffset());
                            PsiElement container = ((SuppressIntentionActionFromFix) action).getContainer(at);
                            if (container != null) {
                                injectionHighlighter.highlight(container, Collections.singletonList(container));
                            }
                        }
                        else {
                            PsiElement at = myFile.findElementAt(myEditor.getCaretModel().getOffset());
                            PsiElement container = ((SuppressIntentionActionFromFix) action).getContainer(at);
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
            Container ancestor = SwingUtilities.getAncestorOfClass(JComboBox.class, myEditor.getContentComponent());
            if (ancestor != null) {
                JComboBox comboBox = (JComboBox) ancestor;
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
        Disposer.register(myPopup, UIAccess::assertIsUIThread);
    }

    void canceled(@Nonnull ListPopupStep intentionListStep) {
        if (myPopup.getListStep() != intentionListStep || myDisposed) {
            return;
        }
        // Root canceled. Create new popup. This one cannot be reused.
        recreateMyPopup(intentionListStep);
    }

    private static class MyComponentHint extends LightweightHintImpl {
        private boolean myVisible;
        private boolean myShouldDelay;

        private MyComponentHint(JComponent component) {
            super(component);
        }

        @Override
        public void show(@Nonnull JComponent parentComponent, int x, int y, JComponent focusBackComponent, @Nonnull HintHint hintHint) {
            myVisible = true;
            if (myShouldDelay) {
                ourAlarm.cancelAllRequests();
                ourAlarm.addRequest(() -> showImpl(parentComponent, x, y, focusBackComponent), DELAY);
            }
            else {
                showImpl(parentComponent, x, y, focusBackComponent);
            }
        }

        private void showImpl(JComponent parentComponent, int x, int y, JComponent focusBackComponent) {
            if (!parentComponent.isShowing()) {
                return;
            }
            super.show(parentComponent, x, y, focusBackComponent, new HintHint(parentComponent, new Point(x, y)));
        }

        @Override
        public void hide() {
            super.hide();
            myVisible = false;
            ourAlarm.cancelAllRequests();
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
