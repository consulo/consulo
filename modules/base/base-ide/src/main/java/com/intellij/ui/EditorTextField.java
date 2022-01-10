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
package com.intellij.ui;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ex.AbstractDelegatingToRootTraversalPolicy;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class EditorTextField extends NonOpaquePanel implements DocumentListener, TextComponent, DataProvider, DocumentBasedComponent {
  private static final String uiClassID = "EditorTextFieldUI";

  private static final Logger LOG = Logger.getInstance(EditorTextField.class);
  public static final Key<Boolean> SUPPLEMENTARY_KEY = Key.create("Supplementary");
  private static final Key<Boolean> MANAGED_BY_FIELD = Key.create("MANAGED_BY_FIELD");

  private class ProxyListeners implements FocusListener, MouseListener {
    @Override
    public void focusGained(FocusEvent e) {
      if (myFocusListeners != null) {
        myFocusListeners.focusGained(e);
      }
    }

    @Override
    public void focusLost(FocusEvent e) {
      if (myFocusListeners != null) {
        myFocusListeners.focusLost(e);
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if(myMouseListeners != null) {
        myMouseListeners.mouseClicked(e);
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (myMouseListeners != null) {
        myMouseListeners.mousePressed(e);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (myMouseListeners != null) {
        myMouseListeners.mouseReleased(e);
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      if (myMouseListeners != null) {
        myMouseListeners.mouseEntered(e);
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      if (myMouseListeners != null) {
        myMouseListeners.mouseExited(e);
      }
    }
  }

  private Document myDocument;
  private final Project myProject;
  private FileType myFileType;
  private EditorEx myEditor;
  private Component myNextFocusable;
  private boolean myWholeTextSelected;
  private final List<DocumentListener> myDocumentListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private boolean myIsListenerInstalled;
  private boolean myIsViewer;
  private boolean myIsSupplementary;
  private boolean myInheritSwingFont = true;
  private Color myEnforcedBgColor;
  private boolean myOneLineMode; // use getter to access this field! It is allowed to override getter and change initial behaviour
  private boolean myEnsureWillComputePreferredSize;
  private Dimension myPassivePreferredSize;
  private CharSequence myHintText;
  private boolean myIsRendererWithSelection;
  private Color myRendererBg;
  private Color myRendererFg;
  private int myPreferredWidth = -1;
  private int myCaretPosition = -1;
  private final List<EditorSettingsProvider> mySettingsProviders = new ArrayList<>();
  private Disposable myDisposable;

  private FocusListener myFocusListeners;
  private MouseListener myMouseListeners;
  private ProxyListeners myProxyListeners = new ProxyListeners();

  public EditorTextField() {
    this("");
  }

  public EditorTextField(@Nonnull String text) {
    this(EditorFactory.getInstance().createDocument(text), null, PlainTextFileType.INSTANCE);
  }

  public EditorTextField(@Nonnull String text, Project project, FileType fileType) {
    this(EditorFactory.getInstance().createDocument(text), project, fileType, false, true);
  }

  public EditorTextField(Document document, Project project, FileType fileType) {
    this(document, project, fileType, false, true);
  }

  public EditorTextField(Document document, Project project, FileType fileType, boolean isViewer) {
    this(document, project, fileType, isViewer, true);
  }

  public EditorTextField(Document document, Project project, FileType fileType, boolean isViewer, boolean oneLineMode) {
    super(new BorderLayout());
    myOneLineMode = oneLineMode;
    myIsViewer = isViewer;
    setDocument(document);
    myProject = project;
    myFileType = fileType;
    enableEvents(AWTEvent.KEY_EVENT_MASK);
    // todo[dsl,max]
    setFocusable(true);
    // dsl: this is a weird way of doing things....
    super.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        requestFocus();
      }
    });

    setFocusTraversalPolicyProvider(true);
    setFocusTraversalPolicy(new Jdk7DelegatingToRootTraversalPolicy());

    setFont(UIManager.getFont("TextField.font"));
  }

  public void setSupplementary(boolean supplementary) {
    myIsSupplementary = supplementary;
    if (myEditor != null) {
      myEditor.putUserData(SUPPLEMENTARY_KEY, supplementary);
    }
  }

  @Override
  public String getUIClassID() {
    return uiClassID;
  }

  public void setFontInheritedFromLAF(boolean b) {
    myInheritSwingFont = b;
    setDocument(myDocument); // reinit editor.
  }

  @Override
  public String getText() {
    return myDocument.getText();
  }

  @Override
  public void setBackground(Color bg) {
    super.setBackground(bg);
    myEnforcedBgColor = bg;
    if (myEditor != null) {
      myEditor.setBackgroundColor(TargetAWT.from(bg));
    }
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  public void addDocumentListener(DocumentListener listener) {
    myDocumentListeners.add(listener);
    installDocumentListener();
  }

  public void removeDocumentListener(DocumentListener listener) {
    myDocumentListeners.remove(listener);
    uninstallDocumentListener(false);
  }

  @Override
  public void beforeDocumentChange(DocumentEvent event) {
    for (DocumentListener documentListener : myDocumentListeners) {
      documentListener.beforeDocumentChange(event);
    }
  }

  @Override
  public void documentChanged(DocumentEvent event) {
    for (DocumentListener documentListener : myDocumentListeners) {
      documentListener.documentChanged(event);
    }
  }

  public Project getProject() {
    return myProject;
  }

  @Override
  public Document getDocument() {
    return myDocument;
  }

  public void setDocument(Document document) {
    if (myDocument != null) {
      uninstallDocumentListener(true);
    }

    myDocument = document;
    installDocumentListener();
    if (myEditor != null) {
      //MainWatchPanel watches the oldEditor's focus in order to remove debugger combobox when focus is lost
      //we should first transfer focus to new oldEditor and only then remove current oldEditor
      //MainWatchPanel check that oldEditor.getParent == newEditor.getParent and does not remove oldEditor in such cases

      boolean isFocused = isFocusOwner();
      EditorEx newEditor = createEditor();
      releaseEditor(myEditor);
      myEditor = newEditor;
      add(myEditor.getComponent(), BorderLayout.CENTER);

      validate();
      if (isFocused) {
        myEditor.getContentComponent().requestFocus();
      }
    }
  }

  private void installDocumentListener() {
    if (myDocument != null && !myDocumentListeners.isEmpty() && !myIsListenerInstalled) {
      myIsListenerInstalled = true;
      myDocument.addDocumentListener(this);
    }
  }

  private void uninstallDocumentListener(boolean force) {
    if (myDocument != null && myIsListenerInstalled && (force || myDocumentListeners.isEmpty())) {
      myIsListenerInstalled = false;
      myDocument.removeDocumentListener(this);
    }
  }

  public void setText(@Nullable final String text) {
    ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().executeCommand(getProject(), () -> {
      myDocument.replaceString(0, myDocument.getTextLength(), text == null ? "" : text);
      if (myEditor != null) {
        final CaretModel caretModel = myEditor.getCaretModel();
        if (caretModel.getOffset() >= myDocument.getTextLength()) {
          caretModel.moveToOffset(myDocument.getTextLength());
        }
      }
    }, null, null, UndoConfirmationPolicy.DEFAULT, getDocument()));
  }

  /**
   * Allows to define {@link EditorEx#setPlaceholder(CharSequence) editor's placeholder}. The trick here is that the editor
   * is instantiated lazily by the editor text field and provided placeholder text is applied to the editor during its
   * actual construction then.
   *
   * @param text {@link EditorEx#setPlaceholder(CharSequence) editor's placeholder} text to use
   */
  public void setPlaceholder(@Nullable CharSequence text) {
    myHintText = text;
    if (myEditor != null) {
      myEditor.setPlaceholder(text);
    }
  }

  public void selectAll() {
    if (myEditor != null) {
      doSelectAll(myEditor);
    }
    else {
      myWholeTextSelected = true;
    }
  }

  private static void doSelectAll(@Nonnull Editor editor) {
    editor.getCaretModel().removeSecondaryCarets();
    editor.getCaretModel().getPrimaryCaret().setSelection(0, editor.getDocument().getTextLength(), false);
  }

  public void removeSelection() {
    if (myEditor != null) {
      myEditor.getSelectionModel().removeSelection();
    }
    else {
      myWholeTextSelected = false;
    }
  }

  /**
   * @see javax.swing.text.JTextComponent#setCaretPosition(int)
   */
  public void setCaretPosition(int position) {
    Document document = getDocument();
    if (document != null) {
      if (position > document.getTextLength() || position < 0) {
        throw new IllegalArgumentException("bad position: " + position);
      }
      if (myEditor != null) {
        myEditor.getCaretModel().moveToOffset(myCaretPosition);
      }
      else {
        myCaretPosition = position;
      }
    }
  }

  public CaretModel getCaretModel() {
    return myEditor.getCaretModel();
  }

  @Override
  public boolean isFocusOwner() {
    if (myEditor != null) {
      return IJSwingUtilities.hasFocus(myEditor.getContentComponent());
    }
    return super.isFocusOwner();
  }

  @Override
  public void addNotify() {
    myDisposable = Disposable.newDisposable("ETF dispose");
    Disposer.register(myDisposable, this::releaseEditorLater);
    if (myProject != null) {
      ProjectManagerListener listener = new ProjectManagerListener() {
        @Override
        public void projectClosing(Project project) {
          releaseEditor(myEditor);
          myEditor = null;
        }
      };
      ProjectManager.getInstance().addProjectManagerListener(myProject, listener);
      Disposer.register(myDisposable, () -> ProjectManager.getInstance().removeProjectManagerListener(myProject, listener));
    }

    if (myEditor != null) {
      releaseEditorLater();
    }

    boolean isFocused = isFocusOwner();

    initEditor();

    super.addNotify();

    if (myNextFocusable != null) {
      myEditor.getContentComponent().setNextFocusableComponent(myNextFocusable);
      myNextFocusable = null;
    }
    revalidate();
    if (isFocused) {
      IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(this);
    }
  }

  private void initEditor() {
    myEditor = createEditor();
    myEditor.getContentComponent().setEnabled(isEnabled());
    if (myCaretPosition >= 0) {
      myEditor.getCaretModel().moveToOffset(myCaretPosition);
      myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }
    add(myEditor.getComponent(), BorderLayout.CENTER);
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    if (myDisposable != null) {
      Disposer.dispose(myDisposable);
    }
  }

  private void releaseEditor(Editor editor) {
    if (editor == null) return;

    // todo IMHO this should be removed completely
    if (myProject != null && !myProject.isDisposed() && myIsViewer) {
      final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        DaemonCodeAnalyzer.getInstance(myProject).setHighlightingEnabled(psiFile, true);
      }
    }

    remove(editor.getComponent());

    editor.getContentComponent().removeMouseListener(myProxyListeners);
    editor.getContentComponent().removeFocusListener(myProxyListeners);

    if (!editor.isDisposed()) {
      EditorFactory.getInstance().releaseEditor(editor);
    }
  }

  void releaseEditorLater() {
    // releasing an editor implies removing it from a component hierarchy
    // invokeLater is required because releaseEditor() may be called from
    // removeNotify(), so we need to let swing complete its removeNotify() chain
    // and only then execute another removal from the hierarchy. Otherwise
    // swing goes nuts because of nested removals and indices get corrupted
    EditorEx editor = myEditor;
    SwingUtilities.invokeLater(() -> releaseEditor(editor));
    myEditor = null;
  }

  @Override
  public void setFont(Font font) {
    super.setFont(font);
    if (myEditor != null) {
      setupEditorFont(myEditor);
    }
  }

  /**
   * This option will be used for embedded editor creation. It's ok to override this method if you don't want to configure
   * it using class constructor
   *
   * @return is one line mode or not
   */
  public boolean isOneLineMode() {
    return myOneLineMode;
  }

  private void initOneLineMode(final EditorEx editor) {
    final boolean isOneLineMode = isOneLineMode();

    // set mode in editor
    editor.setOneLineMode(isOneLineMode);

    EditorColorsManager colorsManager = EditorColorsManager.getInstance();
    final EditorColorsScheme defaultScheme = UIUtil.isUnderDarcula() ? colorsManager.getGlobalScheme() : colorsManager.getScheme(EditorColorsManager.DEFAULT_SCHEME_NAME);
    EditorColorsScheme customGlobalScheme = isOneLineMode ? defaultScheme : null;

    editor.setColorsScheme(editor.createBoundColorSchemeDelegate(customGlobalScheme));

    EditorColorsScheme colorsScheme = editor.getColorsScheme();
    editor.getSettings().setCaretRowShown(false);

    // color scheme settings:
    setupEditorFont(editor);
    updateBorder(editor);
    editor.setBackgroundColor(TargetAWT.from(getBackgroundColor(isEnabled())));
  }

  public void setOneLineMode(boolean oneLineMode) {
    myOneLineMode = oneLineMode;
  }

  protected EditorEx createEditor() {
    LOG.assertTrue(myDocument != null);

    final EditorFactory factory = EditorFactory.getInstance();
    EditorEx editor = (EditorEx)(myIsViewer ? factory.createViewer(myDocument, myProject) : factory.createEditor(myDocument, myProject));
    editor.putUserData(MANAGED_BY_FIELD, Boolean.TRUE);

    final EditorSettings settings = editor.getSettings();
    settings.setAdditionalLinesCount(0);
    settings.setAdditionalColumnsCount(1);
    settings.setRightMarginShown(false);
    settings.setRightMargin(-1);
    settings.setFoldingOutlineShown(false);
    settings.setLineNumbersShown(false);
    settings.setLineMarkerAreaShown(false);
    settings.setIndentGuidesShown(false);
    settings.setVirtualSpace(false);
    settings.setWheelFontChangeEnabled(false);
    settings.setAdditionalPageAtBottom(false);
    editor.setHorizontalScrollbarVisible(false);
    editor.setVerticalScrollbarVisible(false);
    editor.setCaretEnabled(!myIsViewer);
    settings.setLineCursorWidth(1);

    JScrollPane scrollPane = editor.getScrollPane();
    scrollPane.setBorder(JBUI.Borders.empty());
    scrollPane.setViewportBorder(JBUI.Borders.empty());

    if (myProject != null) {
      PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        DaemonCodeAnalyzer.getInstance(myProject).setHighlightingEnabled(psiFile, !myIsViewer);
      }
    }

    if (myProject != null && myFileType != null) {
      editor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(myProject, myFileType));
    }

    editor.getSettings().setCaretRowShown(false);

    editor.setOneLineMode(myOneLineMode);
    editor.getCaretModel().moveToOffset(myDocument.getTextLength());

    if (myIsViewer) {
      editor.getSelectionModel().removeSelection();
    }
    else if (myWholeTextSelected) {
      doSelectAll(editor);
      myWholeTextSelected = false;
    }

    editor.putUserData(SUPPLEMENTARY_KEY, myIsSupplementary);
    editor.getContentComponent().setFocusCycleRoot(false);
    editor.getContentComponent().addFocusListener(myProxyListeners);
    editor.getContentComponent().addMouseListener(myProxyListeners);

    editor.setPlaceholder(myHintText);

    initOneLineMode(editor);

    if (myIsRendererWithSelection) {
      ((DesktopEditorImpl)editor).setPaintSelection(true);
      editor.getColorsScheme().setColor(EditorColors.SELECTION_BACKGROUND_COLOR, TargetAWT.from(myRendererBg));
      editor.getColorsScheme().setColor(EditorColors.SELECTION_FOREGROUND_COLOR, TargetAWT.from(myRendererFg));
      editor.getSelectionModel().setSelection(0, myDocument.getTextLength());
      editor.setBackgroundColor(TargetAWT.from(myRendererBg));
    }

    for (EditorSettingsProvider provider : mySettingsProviders) {
      provider.customizeSettings(editor);
    }

    return editor;
  }

  @Deprecated
  protected void updateBorder(@Nonnull final EditorEx editor) {
  }

  public static boolean managesEditor(@Nonnull Editor editor) {
    return editor.getUserData(MANAGED_BY_FIELD) == Boolean.TRUE;
  }

  private void setupEditorFont(final EditorEx editor) {
    if (myInheritSwingFont) {
      ((DesktopEditorImpl)editor).setUseEditorAntialiasing(false);
      editor.getColorsScheme().setEditorFontName(getFont().getFontName());
      editor.getColorsScheme().setEditorFontSize(getFont().getSize());
      return;
    }
    UISettings settings = UISettings.getInstance();
    if (settings.PRESENTATION_MODE) editor.setFontSize(settings.PRESENTATION_MODE_FONT_SIZE);
  }

  protected boolean shouldHaveBorder() {
    return true;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (isEnabled() != enabled) {
      super.setEnabled(enabled);
      setViewerEnabled(enabled);
      EditorEx editor = myEditor;
      if (editor != null) {
        releaseEditor(editor);
        initEditor();
        revalidate();
      }
    }
  }

  protected void setViewerEnabled(boolean enabled) {
    myIsViewer = !enabled;
  }

  public boolean isViewer() {
    return myIsViewer;
  }

  @Override
  public Color getBackground() {
    return getBackgroundColor(isEnabled());
  }

  @Nonnull
  private Color getBackgroundColor(boolean enabled) {
    if (myEnforcedBgColor != null) return myEnforcedBgColor;
    if (UIUtil.getParentOfType(CellRendererPane.class, this) != null) {
      return getParent().getBackground();
    }
    return enabled ? UIUtil.getTextFieldBackground() : UIUtil.getInactiveTextFieldBackgroundColor();
  }

  @Override
  protected void addImpl(Component comp, Object constraints, int index) {
    if (myEditor == null || comp != myEditor.getComponent()) {
      assert false : "You are not allowed to add anything to EditorTextField";
    }

    super.addImpl(comp, constraints, index);
  }

  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }

    boolean toReleaseEditor = false;
    if (myEditor == null && myEnsureWillComputePreferredSize) {
      myEnsureWillComputePreferredSize = false;
      initEditor();
      toReleaseEditor = true;
    }


    Dimension size = new Dimension(100, 20);
    if (myEditor != null) {
      final Dimension preferredSize = new Dimension(myEditor.getComponent().getPreferredSize());

      if (myPreferredWidth != -1) {
        preferredSize.width = myPreferredWidth;
      }

      JBInsets.addTo(preferredSize, getInsets());
      size = preferredSize;
    }
    else if (myPassivePreferredSize != null) {
      size = myPassivePreferredSize;
    }

    if (toReleaseEditor) {
      releaseEditor(myEditor);
      myEditor = null;
      myPassivePreferredSize = size;
    }

    return size;
  }

  //@Override
  //public Dimension getMinimumSize() {
  //  if (isMinimumSizeSet()) {
  //    return super.getMinimumSize();
  //  }
  //
  //  Dimension size = new Dimension(1, 20);
  //  if (myEditor != null) {
  //    size.height = myEditor.getLineHeight();
  //
  //    JBInsets.addTo(size, getInsets());
  //    JBInsets.addTo(size, myEditor.getInsets());
  //  }
  //
  //  return size;
  //}

  public void setPreferredWidth(int preferredWidth) {
    myPreferredWidth = preferredWidth;
  }

  @Override
  public Component getNextFocusableComponent() {
    if (myEditor == null && myNextFocusable == null) return super.getNextFocusableComponent();
    if (myEditor == null) return myNextFocusable;
    return myEditor.getContentComponent().getNextFocusableComponent();
  }

  @Override
  public void setNextFocusableComponent(Component aComponent) {
    if (myEditor != null) {
      myEditor.getContentComponent().setNextFocusableComponent(aComponent);
      return;
    }
    myNextFocusable = aComponent;
  }


  @Override
  protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
    if (e.isConsumed() || myEditor != null && !myEditor.processKeyTyped(e)) {
      return super.processKeyBinding(ks, e, condition, pressed);
    }
    return true;
  }

  @Override
  public void requestFocus() {
    Editor editor = myEditor;
    if (editor != null) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
        Editor e = myEditor;
        if (e != null) IdeFocusManager.getGlobalInstance().requestFocus(e.getContentComponent(), true);
      });
      editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }
  }

  @Override
  public boolean hasFocus() {
    if (myEditor != null) {
      return myEditor.getContentComponent().hasFocus();
    }
    else {
      return super.requestFocusInWindow();
    }
  }

  @Override
  public boolean requestFocusInWindow() {
    if (myEditor != null) {
      final boolean b = myEditor.getContentComponent().requestFocusInWindow();
      myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
      return b;
    }
    else {
      return super.requestFocusInWindow();
    }
  }

  /**
   * @return null if the editor is not initialized (e.g. if the field is not added to a container)
   * @see #createEditor()
   * @see #addNotify()
   */
  @Nullable
  public Editor getEditor() {
    return myEditor;
  }

  public JComponent getFocusTarget() {
    return myEditor == null ? this : myEditor.getContentComponent();
  }

  @Override
  public synchronized void addFocusListener(FocusListener l) {
    myFocusListeners = AWTEventMulticaster.add(myFocusListeners, l);
  }

  @Override
  public synchronized void removeFocusListener(FocusListener l) {
    myFocusListeners = AWTEventMulticaster.remove(myFocusListeners, l);
  }

  @Override
  public synchronized void addMouseListener(MouseListener l) {
    myMouseListeners = AWTEventMulticaster.add(myMouseListeners, l);
  }

  @Override
  public synchronized void removeMouseListener(MouseListener l) {
    myMouseListeners = AWTEventMulticaster.remove(myMouseListeners, l);
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (myEditor != null && myEditor.isRendererMode()) {
      if (PlatformDataKeys.COPY_PROVIDER == dataId) {
        return myEditor.getCopyProvider();
      }
      return null;
    }

    if (CommonDataKeys.EDITOR == dataId) {
      return myEditor;
    }

    return null;
  }

  public void setFileType(FileType fileType) {
    setNewDocumentAndFileType(fileType, getDocument());
  }

  public void setNewDocumentAndFileType(@Nonnull FileType fileType, Document document) {
    myFileType = fileType;
    setDocument(document);
  }

  public void ensureWillComputePreferredSize() {
    myEnsureWillComputePreferredSize = true;
  }

  public void setAsRendererWithSelection(Color backgroundColor, Color foregroundColor) {
    myIsRendererWithSelection = true;
    myRendererBg = backgroundColor;
    myRendererFg = foregroundColor;
  }

  public void addSettingsProvider(@Nonnull EditorSettingsProvider provider) {
    mySettingsProviders.add(provider);
  }

  public boolean removeSettingsProvider(@Nonnull EditorSettingsProvider provider) {
    return mySettingsProviders.remove(provider);
  }

  private static class Jdk7DelegatingToRootTraversalPolicy extends AbstractDelegatingToRootTraversalPolicy {
    private boolean invokedFromBeforeOrAfter;

    @Override
    public Component getFirstComponent(Container aContainer) {
      return getDefaultComponent(aContainer);
    }

    @Override
    public Component getLastComponent(Container aContainer) {
      return getDefaultComponent(aContainer);
    }

    @Override
    public Component getComponentAfter(Container aContainer, Component aComponent) {
      invokedFromBeforeOrAfter = true;
      Component after;
      try {
        after = super.getComponentAfter(aContainer, aComponent);
      }
      finally {
        invokedFromBeforeOrAfter = false;
      }
      return after != aComponent ? after : null;  // escape our container
    }

    @Override
    public Component getComponentBefore(Container aContainer, Component aComponent) {
      Component before = super.getComponentBefore(aContainer, aComponent);
      return before != aComponent ? before : null;  // escape our container
    }

    @Override
    public Component getDefaultComponent(Container aContainer) {
      if (invokedFromBeforeOrAfter) return null;     // escape our container
      Editor editor = aContainer instanceof EditorTextField ? ((EditorTextField)aContainer).getEditor() : null;
      if (editor != null) return editor.getContentComponent();
      return aContainer;
    }
  }
}
