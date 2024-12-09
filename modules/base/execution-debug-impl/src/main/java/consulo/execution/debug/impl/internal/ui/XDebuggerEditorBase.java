/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.debug.impl.internal.ui;

import consulo.application.AllIcons;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.FoldingModelEx;
import consulo.codeEditor.util.SoftWrapUtil;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.XDebuggerHistoryManager;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.evaluation.XDebuggerEditorsProviderBase;
import consulo.execution.debug.internal.breakpoint.XExpressionImpl;
import consulo.language.Language;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.psi.PsiElement;
import consulo.language.util.LanguageUtil;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class XDebuggerEditorBase {
    private final Project myProject;
    private final XDebuggerEditorsProvider myDebuggerEditorsProvider;
    @Nonnull
    private final EvaluationMode myMode;
    @Nullable
    private final String myHistoryId;
    @Nullable
    private XSourcePosition mySourcePosition;
    private int myHistoryIndex = -1;
    @Nullable
    private PsiElement myContext;

    private final JLabel myLanguageChooser = new JLabel();
    private WeakReference<ListPopup> myPopup;

    private final JLabel myExpandButton = new JBLabel(AllIcons.General.ExpandComponent);
    private JBPopup myExpandedPopup;
    private Runnable myExpandHandler;

    protected XDebuggerEditorBase(final Project project,
                                  @Nonnull XDebuggerEditorsProvider debuggerEditorsProvider,
                                  @Nonnull EvaluationMode mode,
                                  @Nullable String historyId,
                                  final @Nullable XSourcePosition sourcePosition) {
        myProject = project;
        myDebuggerEditorsProvider = debuggerEditorsProvider;
        myMode = mode;
        myHistoryId = historyId;
        mySourcePosition = sourcePosition;

        myLanguageChooser.setToolTipText(XDebuggerBundle.message("xdebugger.evaluate.language.hint"));
        myLanguageChooser.setBorder(JBUI.Borders.empty(0, 3, 0, 3));
        new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
                if (myLanguageChooser.isEnabled()) {
                    ListPopup oldPopup = SoftReference.dereference(myPopup);
                    if (oldPopup != null && !oldPopup.isDisposed()) {
                        oldPopup.cancel();
                        myPopup = null;
                        return true;
                    }
                    ListPopup popup = createLanguagePopup();
                    popup.showUnderneathOf(myLanguageChooser);
                    myPopup = new WeakReference<>(popup);
                    return true;
                }
                return false;
            }
        }.installOn(myLanguageChooser);

        // setup expand button
        myExpandButton.setToolTipText(KeymapUtil.createTooltipText(CommonLocalize.actionExpand().get(), "ExpandExpandableComponent"));
        myExpandButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        myExpandButton.setBorder(JBUI.Borders.empty(0, 3));
        myExpandButton.setDisabledIcon(TargetAWT.to(AllIcons.General.ExpandComponent));
        new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
                expand();
                return true;
            }
        }.installOn(myExpandButton);
        myExpandButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                myExpandButton.setIcon(TargetAWT.to(AllIcons.General.ExpandComponentHover));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                myExpandButton.setIcon(TargetAWT.to(AllIcons.General.ExpandComponent));
            }
        });
    }

    private ListPopup createLanguagePopup() {
        DefaultActionGroup actions = new DefaultActionGroup();
        for (Language language : getSupportedLanguages()) {
            //noinspection ConstantConditions
            actions.add(new AnAction(language.getDisplayName(), null, language.getAssociatedFileType().getIcon()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    XExpression currentExpression = getExpression();
                    setExpression(new XExpressionImpl(currentExpression.getExpression(), language, currentExpression.getCustomInfo()));
                    requestFocusInEditor();
                }
            });
        }

        DataContext dataContext = DataManager.getInstance().getDataContext(getComponent());
        return JBPopupFactory.getInstance().createActionGroupPopup("Choose Language", actions, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }

    @Nonnull
    private Collection<Language> getSupportedLanguages() {
        XDebuggerEditorsProvider editorsProvider = getEditorsProvider();
        if (myContext != null && editorsProvider instanceof XDebuggerEditorsProviderBase) {
            return ((XDebuggerEditorsProviderBase) editorsProvider).getSupportedLanguages(myContext);
        }
        else {
            return editorsProvider.getSupportedLanguages(myProject, mySourcePosition);
        }
    }

    protected JComponent decorate(JComponent component, boolean multiline, boolean showEditor) {
        BorderLayoutPanel panel = JBUI.Panels.simplePanel();

        JPanel factoryPanel = JBUI.Panels.simplePanel();
        factoryPanel.add(myLanguageChooser, multiline ? BorderLayout.NORTH : BorderLayout.CENTER);
        panel.add(factoryPanel, BorderLayout.WEST);

        if (!multiline && showEditor) {
            component = addExpand(component, false);
        }

        panel.addToCenter(component);

        if (multiline) {
            JBLabel adLabel = new JBLabel(getAdText(), SwingConstants.RIGHT);
            adLabel.setComponentStyle(UIUtil.ComponentStyle.SMALL);
            adLabel.setFontColor(UIUtil.FontColor.BRIGHTER);
            panel.addToBottom(adLabel);
        }

        return panel;
    }

    protected JComponent addChooser(JComponent component) {
        BorderLayoutPanel panel = JBUI.Panels.simplePanel(component);
        panel.setBackground(component.getBackground());
        panel.addToRight(myLanguageChooser);
        return panel;
    }

    protected JComponent addExpand(JComponent component, boolean inheritBackground) {
        BorderLayoutPanel panel;
        if (inheritBackground) {
            panel = new BorderLayoutPanel() {
                @Override
                public Color getBackground() {
                    return component.getBackground();
                }
            };
        }
        else {
            panel = JBUI.Panels.simplePanel();
            panel.setOpaque(false);
        }
        panel.addToCenter(component);
        panel.addToRight(myExpandButton);
        return panel;
    }

    private static String getAdText() {
        return "";
        //return XDebuggerBundle.message("xdebugger.evaluate.history.navigate.ad", NlsMessages.formatAndList(
        //        Arrays.asList(KeymapUtil.getKeystrokeText(KeymapUtil.getKeyStroke(CommonShortcuts.MOVE_DOWN)), KeymapUtil.getKeystrokeText(KeymapUtil.getKeyStroke(CommonShortcuts.MOVE_UP)))));
    }

    public void setContext(@Nullable PsiElement context) {
        if (myContext != context) {
            myContext = context;
            setExpression(getExpression());
        }
    }

    public void setSourcePosition(@Nullable XSourcePosition sourcePosition) {
        if (mySourcePosition != sourcePosition) {
            mySourcePosition = sourcePosition;
            setExpression(getExpression());
        }
    }

    @Nonnull
    public EvaluationMode getMode() {
        return myMode;
    }

    @Nullable
    public abstract Editor getEditor();

    public abstract JComponent getComponent();

    public JComponent getEditorComponent() {
        return getComponent();
    }

    protected abstract void doSetText(XExpression text);

    public void setExpression(@Nullable XExpression text) {
        if (text == null) {
            text = getMode() == EvaluationMode.EXPRESSION ? XExpression.EMPTY_EXPRESSION : XExpression.EMPTY_CODE_FRAGMENT;
        }
        Language language = text.getLanguage();
        if (language == null) {
            if (myContext != null) {
                language = myContext.getLanguage();
            }
            if (language == null && mySourcePosition != null) {
                language = LanguageUtil.getFileLanguage(mySourcePosition.getFile());
            }
            if (language == null) {
                language = LanguageUtil.getFileTypeLanguage(getEditorsProvider().getFileType());
            }
            text = new XExpressionImpl(text.getExpression(), language, text.getCustomInfo(), text.getMode());
        }

        Collection<Language> languages = getSupportedLanguages();
        boolean many = languages.size() > 1;

        if (language != null) {
            myLanguageChooser.setVisible(many);
        }
        myLanguageChooser.setVisible(myLanguageChooser.isVisible() || many);
        //myChooseFactory.setEnabled(many && languages.contains(language));

        if (language != null && language.getAssociatedFileType() != null) {
            Image fileTypeIcon = ImageEffects.layered(language.getAssociatedFileType().getIcon(), AllIcons.General.Dropdown);
            myLanguageChooser.setIcon(TargetAWT.to(fileTypeIcon));
            myLanguageChooser.setDisabledIcon(TargetAWT.to(ImageEffects.grayed(fileTypeIcon)));
        }

        doSetText(text);
    }

    public abstract XExpression getExpression();

    @Nullable
    public abstract JComponent getPreferredFocusedComponent();

    public void requestFocusInEditor() {
        JComponent preferredFocusedComponent = getPreferredFocusedComponent();
        if (preferredFocusedComponent != null) {
            IdeFocusManager instanceForProject = ApplicationIdeFocusManager.getInstance().getInstanceForProject(myProject);

            instanceForProject.requestFocus(preferredFocusedComponent, true);
        }
    }

    public abstract void selectAll();

    protected void onHistoryChanged() {
    }

    public List<XExpression> getRecentExpressions() {
        if (myHistoryId != null) {
            return XDebuggerHistoryManager.getInstance(myProject).getRecentExpressions(myHistoryId);
        }
        return Collections.emptyList();
    }

    public void saveTextInHistory() {
        saveTextInHistory(getExpression());
    }

    private void saveTextInHistory(final XExpression text) {
        if (myHistoryId != null) {
            boolean update = XDebuggerHistoryManager.getInstance(myProject).addRecentExpression(myHistoryId, text);
            myHistoryIndex = -1; //meaning not from the history list
            if (update) {
                onHistoryChanged();
            }
        }
    }

    @Nonnull
    protected FileType getFileType(@Nonnull XExpression expression) {
        FileType fileType = LanguageUtil.getLanguageFileType(expression.getLanguage());
        if (fileType != null) {
            return fileType;
        }
        return getEditorsProvider().getFileType();
    }

    public XDebuggerEditorsProvider getEditorsProvider() {
        return myDebuggerEditorsProvider;
    }

    public Project getProject() {
        return myProject;
    }

    protected Document createDocument(final XExpression text) {
        XDebuggerEditorsProvider provider = getEditorsProvider();
        if (myContext != null && provider instanceof XDebuggerEditorsProviderBase) {
            return ((XDebuggerEditorsProviderBase) provider).createDocument(getProject(), text, myContext, myMode);
        }
        else {
            return provider.createDocument(getProject(), text, mySourcePosition, myMode);
        }
    }

    public boolean canGoBackward() {
        return myHistoryIndex < getRecentExpressions().size() - 1;
    }

    public boolean canGoForward() {
        return myHistoryIndex > 0;
    }

    public void goBackward() {
        final List<XExpression> expressions = getRecentExpressions();
        if (myHistoryIndex < expressions.size() - 1) {
            myHistoryIndex++;
            setExpression(expressions.get(myHistoryIndex));
        }
    }

    protected void prepareEditor(EditorEx editor) {
    }

    protected final void setExpandable(Editor editor) {
        //editor.getContentComponent().putClientProperty(Expandable.class, this);
    }

    public void collapse() {
        if (myExpandedPopup != null) {
            myExpandedPopup.cancel();
        }
    }

    public void expand() {
        if (myExpandedPopup != null || !getComponent().isEnabled()) {
            return;
        }

        if (myExpandHandler != null) {
            myExpandHandler.run();
            return;
        }

        XDebuggerExpressionEditorImpl expressionEditor = new XDebuggerExpressionEditorImpl(myProject, myDebuggerEditorsProvider, myHistoryId, mySourcePosition, getExpression(), true, true, false) {
            @Override
            protected JComponent decorate(JComponent component, boolean multiline, boolean showEditor) {
                return component;
            }
        };

        EditorTextField editorTextField = (EditorTextField) expressionEditor.getEditorComponent();
        editorTextField.addSettingsProvider(this::prepareEditor);
        editorTextField.addSettingsProvider(this::setExpandable);
        editorTextField.setBorder(JBUI.Borders.empty());
        editorTextField.setFont(editorTextField.getFont().deriveFont((float) getEditor().getColorsScheme().getEditorFontSize()));

        JComponent component = expressionEditor.getComponent();
        component.setPreferredSize(new Dimension(getComponent().getWidth(), 100));

        myExpandedPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, expressionEditor.getPreferredFocusedComponent()).setMayBeParent(true).setFocusable(true).setResizable(true)
            .setRequestFocus(true).setLocateByContent(true).setCancelOnWindowDeactivation(false).setAdText(getAdText()).setKeyboardActions(Collections.singletonList(Pair.create(event -> {
                collapse();
                Window window = ComponentUtil.getWindow(getComponent());
                if (window != null) {
                    window.dispatchEvent(new KeyEvent(getComponent(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), InputEvent.CTRL_MASK, KeyEvent.VK_ENTER, '\r'));
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK)))).setCancelCallback(() -> {
                setExpression(expressionEditor.getExpression());
                requestFocusInEditor();
                Editor baseEditor = getEditor();
                if (baseEditor != null) {
                    foldNewLines((EditorEx) baseEditor);
                    Editor newEditor = expressionEditor.getEditor();
                    if (newEditor != null) {
                        copyCaretPosition(newEditor, baseEditor);

                        ApplicationPropertiesComponent.getInstance().setValue(SOFT_WRAPS_KEY, newEditor.getSoftWrapModel().isSoftWrappingEnabled());
                    }
                }
                myExpandedPopup = null;
                return true;
            }).createPopup();

        myExpandedPopup.show(new RelativePoint(getComponent(), new Point(0, 0)));

        EditorEx editor = (EditorEx) expressionEditor.getEditor();
        copyCaretPosition(getEditor(), editor);
        editor.getSettings().setUseSoftWraps(isUseSoftWraps());

        addCollapseButton(editor, this::collapse);

        expressionEditor.requestFocusInEditor();
    }

    public void setExpandHandler(Runnable handler) {
        myExpandHandler = handler;
    }

    private static void addCollapseButton(EditorEx editor, Runnable handler) {
        //ErrorStripeEditorCustomization.DISABLED.customize(editor);
        // TODO: copied from ExpandableTextField
        JScrollPane pane = editor.getScrollPane();
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pane.getVerticalScrollBar().add(JBScrollBar.LEADING, new JBLabel(AllIcons.General.CollapseComponent) {{
            setToolTipText(KeymapUtil.createTooltipText(CommonLocalize.actionCollapse().get(), "CollapseExpandableComponent"));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(JBUI.Borders.empty(5, 0, 5, 5));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    setIcon(TargetAWT.to(AllIcons.General.CollapseComponentHover));
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    setIcon(TargetAWT.to(AllIcons.General.CollapseComponent));
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    handler.run();
                }
            });
        }});
    }

    public static void copyCaretPosition(@Nullable Editor source, @Nullable Editor destination) {
        if (source != null && destination != null) {
            destination.getCaretModel().moveToOffset(source.getCaretModel().getOffset());
        }
    }

    public void goForward() {
        final List<XExpression> expressions = getRecentExpressions();
        if (myHistoryIndex > 0) {
            myHistoryIndex--;
            setExpression(expressions.get(myHistoryIndex));
        }
    }

    protected static void foldNewLines(EditorEx editor) {
        editor.getColorsScheme().setAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES, null);
        editor.reinitSettings();
        FoldingModelEx foldingModel = editor.getFoldingModel();
        CharSequence text = editor.getDocument().getCharsSequence();
        foldingModel.runBatchFoldingOperation(() -> {
            foldingModel.clearFoldRegions();
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    foldingModel.createFoldRegion(i, i + 1, "\u23ce", null, true);
                }
            }
        });
    }

    private static final String SOFT_WRAPS_KEY = "XDebuggerExpressionEditor_Use_Soft_Wraps";

    public boolean isUseSoftWraps() {
        return ApplicationPropertiesComponent.getInstance().getBoolean(SOFT_WRAPS_KEY, true);
    }

    public void setUseSoftWraps(boolean use) {
        ApplicationPropertiesComponent.getInstance().setValue(SOFT_WRAPS_KEY, use);
        Editor editor = getEditor();
        if (editor != null) {
            SoftWrapUtil.toggleSoftWraps(editor, null, use);
        }
    }
}
