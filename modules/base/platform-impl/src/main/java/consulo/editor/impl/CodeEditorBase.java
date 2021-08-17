// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.editor.impl;

import com.intellij.application.options.EditorFontsConstants;
import com.intellij.diagnostic.Dumpable;
import com.intellij.ide.CopyProvider;
import com.intellij.ide.CutProvider;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.colors.*;
import com.intellij.openapi.editor.colors.impl.DelegateColorScheme;
import com.intellij.openapi.editor.colors.impl.FontPreferencesImpl;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.impl.*;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.disposer.TraceableDisposable;
import consulo.disposer.util.DisposerUtil;
import consulo.editor.internal.EditorInternal;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.*;
import java.util.function.IntFunction;

/**
 * Common part from desktop CodeEditor implementation
 */
public abstract class CodeEditorBase extends UserDataHolderBase implements EditorInternal, HighlighterClient, Dumpable, Queryable {
  protected class MyColorSchemeDelegate extends DelegateColorScheme {
    private final FontPreferencesImpl myFontPreferences = new FontPreferencesImpl();
    private final FontPreferencesImpl myConsoleFontPreferences = new FontPreferencesImpl();
    private final Map<TextAttributesKey, TextAttributes> myOwnAttributes = new HashMap<>();
    private final Map<EditorColorKey, ColorValue> myOwnColors = new HashMap<>();
    private final EditorColorsScheme myCustomGlobalScheme;
    private Map<EditorFontType, Font> myFontsMap;
    private int myMaxFontSize = EditorFontsConstants.getMaxEditorFontSize();
    private int myFontSize = -1;
    private int myConsoleFontSize = -1;
    private String myFaceName;
    private Float myLineSpacing;

    private MyColorSchemeDelegate(@Nullable EditorColorsScheme globalScheme) {
      super(globalScheme == null ? EditorColorsManager.getInstance().getGlobalScheme() : globalScheme);
      myCustomGlobalScheme = globalScheme;
      updateGlobalScheme();
    }

    private void reinitFonts() {
      EditorColorsScheme delegate = getDelegate();
      String editorFontName = getEditorFontName();
      int editorFontSize = getEditorFontSize();
      updatePreferences(myFontPreferences, editorFontName, editorFontSize, delegate == null ? null : delegate.getFontPreferences());
      String consoleFontName = getConsoleFontName();
      int consoleFontSize = getConsoleFontSize();
      updatePreferences(myConsoleFontPreferences, consoleFontName, consoleFontSize, delegate == null ? null : delegate.getConsoleFontPreferences());

      myFontsMap = new EnumMap<>(EditorFontType.class);
      myFontsMap.put(EditorFontType.PLAIN, new Font(editorFontName, Font.PLAIN, editorFontSize));
      myFontsMap.put(EditorFontType.BOLD, new Font(editorFontName, Font.BOLD, editorFontSize));
      myFontsMap.put(EditorFontType.ITALIC, new Font(editorFontName, Font.ITALIC, editorFontSize));
      myFontsMap.put(EditorFontType.BOLD_ITALIC, new Font(editorFontName, Font.BOLD | Font.ITALIC, editorFontSize));
      myFontsMap.put(EditorFontType.CONSOLE_PLAIN, new Font(consoleFontName, Font.PLAIN, consoleFontSize));
      myFontsMap.put(EditorFontType.CONSOLE_BOLD, new Font(consoleFontName, Font.BOLD, consoleFontSize));
      myFontsMap.put(EditorFontType.CONSOLE_ITALIC, new Font(consoleFontName, Font.ITALIC, consoleFontSize));
      myFontsMap.put(EditorFontType.CONSOLE_BOLD_ITALIC, new Font(consoleFontName, Font.BOLD | Font.ITALIC, consoleFontSize));
    }

    private void updatePreferences(@Nonnull FontPreferencesImpl preferences, @Nonnull String fontName, int fontSize, @Nullable FontPreferences delegatePreferences) {
      preferences.clear();
      preferences.register(fontName, fontSize);
      if (delegatePreferences != null) {
        boolean first = true; //skip delegate's primary font
        for (String font : delegatePreferences.getRealFontFamilies()) {
          if (!first) {
            preferences.register(font, fontSize);
          }
          first = false;
        }
      }
      preferences.setUseLigatures(delegatePreferences != null && delegatePreferences.useLigatures());
    }

    private void reinitFontsAndSettings() {
      reinitFonts();
      reinitSettings();
    }

    @Override
    public TextAttributes getAttributes(TextAttributesKey key) {
      if (myOwnAttributes.containsKey(key)) return myOwnAttributes.get(key);
      return getDelegate().getAttributes(key);
    }

    @Override
    public void setAttributes(@Nonnull TextAttributesKey key, TextAttributes attributes) {
      myOwnAttributes.put(key, attributes);
    }

    @Nullable
    @Override
    public ColorValue getColor(EditorColorKey key) {
      if (myOwnColors.containsKey(key)) {
        return myOwnColors.get(key);
      }
      return getDelegate().getColor(key);
    }

    @Override
    public void setColor(EditorColorKey key, ColorValue color) {
      myOwnColors.put(key, color);

      // These two are here because those attributes are cached and I do not whant the clients to call editor's reinit
      // settings in this case.
      myCaretModel.reinitSettings();
      mySelectionModel.reinitSettings();
    }

    @Override
    public int getEditorFontSize() {
      if (myFontSize == -1) {
        return getDelegate().getEditorFontSize();
      }
      return myFontSize;
    }

    @Override
    public void setEditorFontSize(int fontSize) {
      if (fontSize < MIN_FONT_SIZE) fontSize = MIN_FONT_SIZE;
      if (fontSize > myMaxFontSize) fontSize = myMaxFontSize;
      if (fontSize == myFontSize) return;
      myFontSize = fontSize;
      reinitFontsAndSettings();
    }

    @Nonnull
    @Override
    public FontPreferences getFontPreferences() {
      return myFontPreferences.getEffectiveFontFamilies().isEmpty() ? getDelegate().getFontPreferences() : myFontPreferences;
    }

    @Override
    public void setFontPreferences(@Nonnull FontPreferences preferences) {
      if (Objects.equals(preferences, myFontPreferences)) return;
      preferences.copyTo(myFontPreferences);
      reinitFontsAndSettings();
    }

    @Nonnull
    @Override
    public FontPreferences getConsoleFontPreferences() {
      return myConsoleFontPreferences.getEffectiveFontFamilies().isEmpty() ? getDelegate().getConsoleFontPreferences() : myConsoleFontPreferences;
    }

    @Override
    public void setConsoleFontPreferences(@Nonnull FontPreferences preferences) {
      if (Objects.equals(preferences, myConsoleFontPreferences)) return;
      preferences.copyTo(myConsoleFontPreferences);
      reinitFontsAndSettings();
    }

    @Override
    public String getEditorFontName() {
      if (myFaceName == null) {
        return getDelegate().getEditorFontName();
      }
      return myFaceName;
    }

    @Override
    public void setEditorFontName(String fontName) {
      if (Objects.equals(fontName, myFaceName)) return;
      myFaceName = fontName;
      reinitFontsAndSettings();
    }

    @Nonnull
    @Override
    public Font getFont(EditorFontType key) {
      if (myFontsMap != null) {
        Font font = myFontsMap.get(key);
        if (font != null) return font;
      }
      return getDelegate().getFont(key);
    }

    @Override
    public void setFont(EditorFontType key, Font font) {
      if (myFontsMap == null) {
        reinitFontsAndSettings();
      }
      myFontsMap.put(key, font);
      reinitSettings();
    }

    @Override
    @Nullable
    public EditorColorsScheme clone() {
      return null;
    }

    public void updateGlobalScheme() {
      setDelegate(myCustomGlobalScheme == null ? EditorColorsManager.getInstance().getGlobalScheme() : myCustomGlobalScheme);
    }

    @Override
    public void setDelegate(@Nonnull EditorColorsScheme delegate) {
      super.setDelegate(delegate);
      int globalFontSize = getDelegate().getEditorFontSize();
      myMaxFontSize = Math.max(EditorFontsConstants.getMaxEditorFontSize(), globalFontSize);
      reinitFonts();
    }

    @Override
    public void setConsoleFontSize(int fontSize) {
      myConsoleFontSize = fontSize;
      reinitFontsAndSettings();
    }

    @Override
    public int getConsoleFontSize() {
      return myConsoleFontSize == -1 ? super.getConsoleFontSize() : myConsoleFontSize;
    }

    @Override
    public float getLineSpacing() {
      return myLineSpacing == null ? super.getLineSpacing() : myLineSpacing;
    }

    @Override
    public void setLineSpacing(float lineSpacing) {
      myLineSpacing = EditorFontsConstants.checkAndFixEditorLineSpacing(lineSpacing);
      reinitSettings();
    }
  }

  private class DefaultPopupHandler extends ContextMenuPopupHandler.ById {
    @Nullable
    @Override
    public String getActionGroupId(@Nonnull EditorMouseEvent event) {
      String contextMenuGroupId = myContextMenuGroupId;
      Inlay inlay = myInlayModel.getElementAt(event.getMouseEvent().getPoint());
      if (inlay != null) {
        String inlayContextMenuGroupId = inlay.getRenderer().getContextMenuGroupId(inlay);
        if (inlayContextMenuGroupId != null) contextMenuGroupId = inlayContextMenuGroupId;
      }
      return contextMenuGroupId;
    }
  }

  private class MyEditable implements CutProvider, CopyProvider, PasteProvider, DeleteProvider, DumbAware {
    @Override
    public void performCopy(@Nonnull DataContext dataContext) {
      executeAction(IdeActions.ACTION_EDITOR_COPY, dataContext);
    }

    @Override
    public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
      return true;
    }

    @Override
    public boolean isCopyVisible(@Nonnull DataContext dataContext) {
      return getSelectionModel().hasSelection(true);
    }

    @Override
    public void performCut(@Nonnull DataContext dataContext) {
      executeAction(IdeActions.ACTION_EDITOR_CUT, dataContext);
    }

    @Override
    public boolean isCutEnabled(@Nonnull DataContext dataContext) {
      return !isViewer();
    }

    @Override
    public boolean isCutVisible(@Nonnull DataContext dataContext) {
      return isCutEnabled(dataContext) && getSelectionModel().hasSelection(true);
    }

    @Override
    public void performPaste(@Nonnull DataContext dataContext) {
      executeAction(IdeActions.ACTION_EDITOR_PASTE, dataContext);
    }

    @Override
    public boolean isPastePossible(@Nonnull DataContext dataContext) {
      // Copy of isPasteEnabled. See interface method javadoc.
      return !isViewer();
    }

    @Override
    public boolean isPasteEnabled(@Nonnull DataContext dataContext) {
      return !isViewer();
    }

    @Override
    public void deleteElement(@Nonnull DataContext dataContext) {
      executeAction(IdeActions.ACTION_EDITOR_DELETE, dataContext);
    }

    @Override
    public boolean canDeleteElement(@Nonnull DataContext dataContext) {
      return !isViewer();
    }

    private void executeAction(@Nonnull String actionId, @Nonnull DataContext dataContext) {
      EditorAction action = (EditorAction)ActionManager.getInstance().getAction(actionId);
      if (action != null) {
        action.actionPerformed(CodeEditorBase.this, dataContext);
      }
    }
  }

  private class EditorDocumentAdapter implements PrioritizedDocumentListener {
    @Override
    public void beforeDocumentChange(@Nonnull DocumentEvent e) {
      beforeChangedUpdate(e);
    }

    @Override
    public void documentChanged(@Nonnull DocumentEvent e) {
      changedUpdate(e);
    }

    @Override
    public void bulkUpdateStarting(@Nonnull Document document) {
      bulkUpdateStarted();
    }

    @Override
    public void bulkUpdateFinished(@Nonnull Document document) {
      CodeEditorBase.this.bulkUpdateFinished();
    }

    @Override
    public int getPriority() {
      return EditorDocumentPriorities.EDITOR_DOCUMENT_ADAPTER;
    }
  }

  private static final Logger LOG = Logger.getInstance(CodeEditorBase.class);
  public static final Key<Boolean> DISABLE_CARET_POSITION_KEEPING = Key.create("editor.disable.caret.position.keeping");

  protected static final int MIN_FONT_SIZE = 8;

  protected volatile EditorHighlighter myHighlighter; // updated in EDT, but can be accessed from other threads (under read action)
  protected Disposable myHighlighterDisposable = Disposable.newDisposable();
  protected boolean isReleased;
  protected final Disposable myDisposable = Disposable.newDisposable();
  @Nonnull
  protected final DocumentEx myDocument;
  @Nullable
  protected final Project myProject;
  protected boolean myIsViewer;
  protected final EditorKind myKind;
  protected boolean myIsRendererMode;

  protected boolean myIsInsertMode = true;
  protected boolean myIsOneLineMode;
  protected boolean myIsColumnMode;

  protected final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

  protected final List<FocusChangeListener> myFocusListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  protected final List<EditorPopupHandler> myPopupHandlers = new ArrayList<>();

  @Nonnull
  protected final List<EditorMouseListener> myMouseListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  @Nonnull
  protected final List<EditorMouseMotionListener> myMouseMotionListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  @Nonnull
  protected EditorColorsScheme myScheme;

  protected VirtualFile myVirtualFile;

  @Nonnull
  protected final SettingsImpl mySettings;

  protected CodeEditorCaretModelBase<CodeEditorCaretBase> myCaretModel;
  protected CodeEditorFoldingModelBase myFoldingModel;
  protected CodeEditorSelectionModelBase mySelectionModel;
  protected CodeEditorScrollingModelBase myScrollingModel;
  protected CodeEditorInlayModelBase myInlayModel;
  protected CodeEditorSoftWrapModelBase mySoftWrapModel;
  protected MarkupModelImpl myMarkupModel;

  @Nonnull
  protected IndentsModel myIndentsModel;

  protected String myContextMenuGroupId = IdeActions.GROUP_BASIC_EDITOR_POPUP;

  protected final TraceableDisposable myTraceableDisposable = TraceableDisposable.newTraceDisposable(true);

  protected Condition<RangeHighlighter> myHighlightingFilter;

  @Nonnull
  protected final MarkupModelListener myMarkupModelListener;

  @Nonnull
  protected final EditorFilteringMarkupModelEx myDocumentMarkupModel;

  protected final List<IntFunction<Collection<LineExtensionInfo>>> myLineExtensionPainters = new SmartList<>();

  @Nullable
  protected CharSequence myPlaceholderText;
  @Nullable
  protected TextAttributes myPlaceholderAttributes;
  protected boolean myShowPlaceholderWhenFocused;

  protected boolean myStickySelection;
  protected int myStickySelectionStart;
  protected boolean myScrollToCaret = true;
  protected boolean myPurePaintingMode;
  protected volatile int myExpectedCaretOffset = -1;

  protected final CommandProcessor myCommandProcessor;

  protected boolean myRestoreScrollingPosition;
  protected boolean myDocumentChangeInProgress;

  protected final EditorScrollingPositionKeeper myScrollingPositionKeeper;

  private MyEditable myEditable;

  public CodeEditorBase(@Nonnull Document document, boolean viewer, @Nullable Project project, @Nonnull EditorKind kind) {
    assertIsDispatchThread();

    myIsViewer = viewer;
    myKind = kind;
    myDocument = (DocumentEx)document;
    myProject = project;
    myScheme = createBoundColorSchemeDelegate(null);

    myCommandProcessor = CommandProcessor.getInstance();
    
    mySettings = new SettingsImpl(this, project, kind);

    MarkupModelEx documentMarkup = (MarkupModelEx)DocumentMarkupModel.forDocument(myDocument, myProject, true);

    myDocumentMarkupModel = new EditorFilteringMarkupModelEx(this, documentMarkup);


    myPopupHandlers.add(new DefaultPopupHandler());

    if (!mySettings.isUseSoftWraps() && shouldSoftWrapsBeForced()) {
      mySettings.setUseSoftWrapsQuiet();
      putUserData(FORCED_SOFT_WRAPS, Boolean.TRUE);
    }

    mySelectionModel = createSelectionModel();
    myMarkupModel = createMarkupModel();
    myFoldingModel = createFoldingModel();
    myCaretModel = createCaretModel();
    myScrollingModel = createScrollingModel();
    myInlayModel = createInlayModel();
    Disposer.register(myCaretModel, myInlayModel);
    mySoftWrapModel = createSoftWrapModel();

    myMarkupModelListener = new MarkupModelListener() {
      @Override
      public void afterAdded(@Nonnull RangeHighlighterEx highlighter) {
        onHighlighterChanged(highlighter, canImpactGutterSize(highlighter), EditorUtil.attributesImpactFontStyleOrColor(highlighter.getTextAttributes()), false);
      }

      @Override
      public void beforeRemoved(@Nonnull RangeHighlighterEx highlighter) {
        onHighlighterChanged(highlighter, canImpactGutterSize(highlighter), EditorUtil.attributesImpactFontStyleOrColor(highlighter.getTextAttributes()), true);
      }

      @Override
      public void attributesChanged(@Nonnull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
        onHighlighterChanged(highlighter, renderersChanged, fontStyleOrColorChanged, false);
      }
    };

    getFilteredDocumentMarkupModel().addMarkupModelListener(myCaretModel, myMarkupModelListener);
    getMarkupModel().addMarkupModelListener(myCaretModel, myMarkupModelListener);

    myDocument.addDocumentListener(myFoldingModel, myCaretModel);
    myDocument.addDocumentListener(myCaretModel, myCaretModel);

    myDocument.addDocumentListener(new EditorDocumentAdapter(), myCaretModel);
    myDocument.addDocumentListener(mySoftWrapModel, myCaretModel);

    myFoldingModel.addListener(mySoftWrapModel, myCaretModel);

    myInlayModel.addListener(myFoldingModel, myCaretModel);
    myInlayModel.addListener(myCaretModel, myCaretModel);

    myIndentsModel = new IndentsModelImpl(this);

    myScrollingPositionKeeper = new EditorScrollingPositionKeeper(this);
    Disposer.register(myDisposable, myScrollingPositionKeeper);
  }

  protected abstract CodeEditorSelectionModelBase createSelectionModel();

  protected abstract MarkupModelImpl createMarkupModel();

  protected abstract CodeEditorFoldingModelBase createFoldingModel();

  protected abstract CodeEditorCaretModelBase createCaretModel();

  protected abstract CodeEditorScrollingModelBase createScrollingModel();

  protected abstract CodeEditorInlayModelBase createInlayModel();

  protected abstract CodeEditorSoftWrapModelBase createSoftWrapModel();

  @Nonnull
  protected abstract DataContext getComponentContext();

  protected abstract void stopDumb();

  protected boolean canImpactGutterSize(@Nonnull RangeHighlighterEx highlighter) {
    return false;
  }

  protected void onHighlighterChanged(@Nonnull RangeHighlighterEx highlighter, boolean canImpactGutterSize, boolean fontStyleOrColorChanged, boolean remove) {
  }

  protected void bulkUpdateStarted() {
    myScrollingPositionKeeper.savePosition();

    myCaretModel.onBulkDocumentUpdateStarted();
    mySoftWrapModel.onBulkDocumentUpdateStarted();
    myFoldingModel.onBulkDocumentUpdateStarted();
  }

  protected void bulkUpdateFinished() {
    myFoldingModel.onBulkDocumentUpdateFinished();
    mySoftWrapModel.onBulkDocumentUpdateFinished();
    myCaretModel.onBulkDocumentUpdateFinished();

    //setMouseSelectionState(MOUSE_SELECTION_STATE_NONE);

    validateSize();

    //updateGutterSize();
    //repaintToScreenBottom(0);
    //updateCaretCursor();

    if (!Boolean.TRUE.equals(getUserData(DISABLE_CARET_POSITION_KEEPING))) {
      myScrollingPositionKeeper.restorePosition(true);
    }
  }

  protected void changedUpdate(DocumentEvent e) {
    myDocumentChangeInProgress = false;
    if (myDocument.isInBulkUpdate()) return;

    if (myRestoreScrollingPosition && !Boolean.TRUE.equals(getUserData(DISABLE_CARET_POSITION_KEEPING))) {
      myScrollingPositionKeeper.restorePosition(true);
    }
  }

  protected void invokePopupIfNeeded(EditorMouseEvent event) {
    if (event.getArea() == EditorMouseEventArea.EDITING_AREA && event.isPopupTrigger() && !event.isConsumed()) {
      for (int i = myPopupHandlers.size() - 1; i >= 0; i--) {
        if (myPopupHandlers.get(i).handlePopup(event)) break;
      }
    }
  }

  private void beforeChangedUpdate(DocumentEvent e) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    myDocumentChangeInProgress = true;
    if (isStickySelection()) {
      setStickySelection(false);
    }
    if (myDocument.isInBulkUpdate()) {
      // Assuming that the job is done at bulk listener callback methods.
      return;
    }

    myRestoreScrollingPosition = getCaretModel().getOffset() < e.getOffset() || getCaretModel().getOffset() > e.getOffset() + e.getOldLength();
    if (myRestoreScrollingPosition) {
      myScrollingPositionKeeper.savePosition();
    }
  }

  @Nonnull
  @Override
  public CodeEditorScrollingModelBase getScrollingModel() {
    return myScrollingModel;
  }

  @Nonnull
  @Override
  public CodeEditorFoldingModelBase getFoldingModel() {
    return myFoldingModel;
  }

  @Nonnull
  @Override
  public CodeEditorSelectionModelBase getSelectionModel() {
    return mySelectionModel;
  }

  @Nonnull
  @Override
  public CodeEditorInlayModelBase getInlayModel() {
    return myInlayModel;
  }

  @Nonnull
  @Override
  public CodeEditorSoftWrapModelBase getSoftWrapModel() {
    return mySoftWrapModel;
  }

  @Nonnull
  @Override
  public MarkupModelEx getMarkupModel() {
    return myMarkupModel;
  }

  @Nonnull
  @Override
  public CodeEditorCaretModelBase<? extends CodeEditorCaretBase> getCaretModel() {
    return myCaretModel;
  }

  private MyEditable getViewer() {
    if (myEditable == null) {
      myEditable = new MyEditable();
    }
    return myEditable;
  }

  @Override
  public void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes) {
    mySoftWrapModel.recalculate();
  }

  @Nonnull
  @Override
  public IndentsModel getIndentsModel() {
    return myIndentsModel;
  }

  @Override
  public boolean isViewer() {
    return myIsViewer || myIsRendererMode;
  }

  @Override
  public boolean isRendererMode() {
    return myIsRendererMode;
  }

  @Override
  public void setRendererMode(boolean isRendererMode) {
    myIsRendererMode = isRendererMode;
  }

  public boolean shouldSoftWrapsBeForced() {
    if (myProject != null && PsiDocumentManager.getInstance(myProject).isDocumentBlockedByPsi(myDocument)) {
      // Disable checking for files in intermediate states - e.g. for files during refactoring.
      return false;
    }
    int lineWidthLimit = Registry.intValue("editor.soft.wrap.force.limit");
    for (int i = 0; i < myDocument.getLineCount(); i++) {
      if (myDocument.getLineEndOffset(i) - myDocument.getLineStartOffset(i) > lineWidthLimit) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void registerLineExtensionPainter(IntFunction<Collection<LineExtensionInfo>> lineExtensionPainter) {
    myLineExtensionPainters.add(lineExtensionPainter);
  }

  public boolean processLineExtensions(int line, Processor<? super LineExtensionInfo> processor) {
    for (IntFunction<Collection<LineExtensionInfo>> painter : myLineExtensionPainters) {
      for (LineExtensionInfo extension : painter.apply(line)) {
        if (!processor.process(extension)) {
          return false;
        }
      }
    }
    if (myProject != null && myVirtualFile != null) {
      for (EditorLinePainter painter : EditorLinePainter.EP_NAME.getExtensionList()) {
        Collection<LineExtensionInfo> extensions = painter.getLineExtensions(myProject, myVirtualFile, line);
        if (extensions != null) {
          for (LineExtensionInfo extension : extensions) {
            if (!processor.process(extension)) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  @Override
  public int getExpectedCaretOffset() {
    int expectedCaretOffset = myExpectedCaretOffset;
    return expectedCaretOffset == -1 ? getCaretModel().getOffset() : expectedCaretOffset;
  }

  @Override
  public boolean isPurePaintingMode() {
    return myPurePaintingMode;
  }

  @Override
  public void setPurePaintingMode(boolean enabled) {
    myPurePaintingMode = enabled;
  }

  @Override
  public CopyProvider getCopyProvider() {
    return getViewer();
  }

  @Override
  public CutProvider getCutProvider() {
    return getViewer();
  }

  @Override
  public PasteProvider getPasteProvider() {
    return getViewer();
  }

  @Override
  public DeleteProvider getDeleteProvider() {
    return getViewer();
  }

  @Override
  public boolean isScrollToCaret() {
    return myScrollToCaret;
  }

  public void setScrollToCaret(boolean scrollToCaret) {
    myScrollToCaret = scrollToCaret;
  }

  @Override
  @Nonnull
  public Disposable getDisposable() {
    return myDisposable;
  }

  @Override
  public void addEditorMouseListener(@Nonnull EditorMouseListener listener) {
    myMouseListeners.add(listener);
  }

  @Override
  public void removeEditorMouseListener(@Nonnull EditorMouseListener listener) {
    boolean success = myMouseListeners.remove(listener);
    LOG.assertTrue(success || isReleased);
  }

  @Override
  public void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener) {
    myMouseMotionListeners.add(listener);
  }

  @Override
  public void removeEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener) {
    boolean success = myMouseMotionListeners.remove(listener);
    LOG.assertTrue(success || isReleased);
  }

  @Override
  public boolean isStickySelection() {
    return myStickySelection;
  }

  @Override
  public void setStickySelection(boolean enable) {
    myStickySelection = enable;
    if (enable) {
      myStickySelectionStart = getCaretModel().getOffset();
    }
    else {
      mySelectionModel.removeSelection();
    }
  }

  @Override
  public void setPlaceholder(@Nullable CharSequence text) {
    myPlaceholderText = text;
  }

  @Override
  public void setPlaceholderAttributes(@Nullable TextAttributes attributes) {
    myPlaceholderAttributes = attributes;
  }

  @Nullable
  public TextAttributes getPlaceholderAttributes() {
    return myPlaceholderAttributes;
  }

  public CharSequence getPlaceholder() {
    return myPlaceholderText;
  }

  @Override
  public void setShowPlaceholderWhenFocused(boolean show) {
    myShowPlaceholderWhenFocused = show;
  }

  public boolean getShowPlaceholderWhenFocused() {
    return myShowPlaceholderWhenFocused;
  }

  @Override
  public boolean isDisposed() {
    return isReleased;
  }

  @Override
  public void stopDumbLater() {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    ApplicationManager.getApplication().invokeLater(this::stopDumb, ModalityState.current(), __ -> isDisposed());
  }

  @Override
  @Nonnull
  public MarkupModelEx getFilteredDocumentMarkupModel() {
    return myDocumentMarkupModel;
  }

  @Override
  public void installPopupHandler(@Nonnull EditorPopupHandler popupHandler) {
    myPopupHandlers.add(popupHandler);
  }

  @Override
  public void uninstallPopupHandler(@Nonnull EditorPopupHandler popupHandler) {
    myPopupHandlers.remove(popupHandler);
  }

  @Override
  public void setHighlightingFilter(@Nullable Condition<RangeHighlighter> filter) {
    if (myHighlightingFilter == filter) return;
    Condition<RangeHighlighter> oldFilter = myHighlightingFilter;
    myHighlightingFilter = filter;

    for (RangeHighlighter highlighter : myDocumentMarkupModel.getDelegate().getAllHighlighters()) {
      boolean oldAvailable = oldFilter == null || oldFilter.value(highlighter);
      boolean newAvailable = filter == null || filter.value(highlighter);
      if (oldAvailable != newAvailable) {
        myMarkupModelListener.attributesChanged((RangeHighlighterEx)highlighter, true, EditorUtil.attributesImpactFontStyleOrColor(highlighter.getTextAttributes()));
      }
    }
  }

  @Override
  public boolean isHighlighterAvailable(@Nonnull RangeHighlighter highlighter) {
    return myHighlightingFilter == null || myHighlightingFilter.value(highlighter);
  }

  @Override
  public void setContextMenuGroupId(@Nullable String groupId) {
    myContextMenuGroupId = groupId;
  }

  @Nullable
  @Override
  public String getContextMenuGroupId() {
    return myContextMenuGroupId;
  }

  @Override
  public void setInsertMode(boolean mode) {
    assertIsDispatchThread();
    boolean oldValue = myIsInsertMode;
    myIsInsertMode = mode;
    myPropertyChangeSupport.firePropertyChange(PROP_INSERT_MODE, oldValue, mode);
  }

  @Override
  @Nullable
  public Project getProject() {
    return myProject;
  }

  @Override
  public boolean isOneLineMode() {
    return myIsOneLineMode;
  }

  @Override
  public void setOneLineMode(boolean isOneLineMode) {
    myIsOneLineMode = isOneLineMode;
    getScrollPane().setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
    reinitSettings();
  }

  protected void fireFocusLost(@Nonnull FocusEvent event) {
    for (FocusChangeListener listener : myFocusListeners) {
      listener.focusLost(this, event);
    }
  }

  protected void fireFocusGained(@Nonnull FocusEvent event) {
    for (FocusChangeListener listener : myFocusListeners) {
      listener.focusGained(this, event);
    }
  }

  @Override
  public void addFocusListener(@Nonnull FocusChangeListener listener) {
    myFocusListeners.add(listener);
  }

  @Override
  public void addFocusListener(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable) {
    DisposerUtil.add(listener, myFocusListeners, parentDisposable);
  }

  @Override
  public boolean isInsertMode() {
    return myIsInsertMode;
  }

  @Override
  public void setColumnMode(boolean mode) {
    assertIsDispatchThread();
    boolean oldValue = myIsColumnMode;
    myIsColumnMode = mode;
    myPropertyChangeSupport.firePropertyChange(PROP_COLUMN_MODE, oldValue, mode);
  }

  @Override
  public boolean isColumnMode() {
    return myIsColumnMode;
  }

  /**
   * To be called when editor was not disposed while it should
   */
  @Override
  public void throwEditorNotDisposedError(@Nonnull final String msg) {
    myTraceableDisposable.throwObjectNotDisposedError(msg);
  }

  @Override
  public void putInfo(@Nonnull Map<String, String> info) {
    final VisualPosition visual = getCaretModel().getVisualPosition();
    info.put("caret", visual.getLine() + ":" + visual.getColumn());
  }

  /**
   * In case of "editor not disposed error" use {@link #throwEditorNotDisposedError(String)}
   */
  public void throwDisposalError(@Nonnull String msg) {
    myTraceableDisposable.throwDisposalError(msg);
  }

  @Override
  @Nonnull
  public String dumpState() {
    return "allow caret inside tab: " +
           mySettings.isCaretInsideTabs() +
           ", allow caret after line end: " +
           mySettings.isVirtualSpace() +
           ", soft wraps: " +
           (mySoftWrapModel.isSoftWrappingEnabled() ? "on" : "off") +
           ", caret model: " +
           getCaretModel().dumpState() +
           ", soft wraps data: " +
           getSoftWrapModel().dumpState() +
           "\n\nfolding data: " +
           getFoldingModel().dumpState() +
           "\ninlay model: " +
           getInlayModel().dumpState() +
           (myDocument instanceof DocumentImpl ? "\n\ndocument info: " + ((DocumentImpl)myDocument).dumpState() : "") +
           "\nfont preferences: " +
           myScheme.getFontPreferences() +
           "\npure painting mode: " +
           myPurePaintingMode;
  }

  @Nonnull
  @Override
  public DataContext getDataContext() {
    return getProjectAwareDataContext(getComponentContext());
  }

  @Nonnull
  private DataContext getProjectAwareDataContext(@Nonnull final DataContext original) {
    if (original.getData(CommonDataKeys.PROJECT) == myProject) return original;

    return new DataContext() {
      @Nullable
      @Override
      @SuppressWarnings("unchecked")
      public <T> T getData(@Nonnull Key<T> dataId) {
        if (CommonDataKeys.PROJECT == dataId) {
          return (T)myProject;
        }
        return original.getData(dataId);
      }
    };
  }

  @Nonnull
  @Override
  public EditorKind getEditorKind() {
    return myKind;
  }

  @Override
  @Nonnull
  public EditorSettings getSettings() {
    return mySettings;
  }

  @Override
  public void setFile(VirtualFile vFile) {
    myVirtualFile = vFile;
    reinitSettings();
  }

  @Override
  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    myPropertyChangeSupport.addPropertyChangeListener(listener);
  }

  @Override
  public void addPropertyChangeListener(@Nonnull final PropertyChangeListener listener, @Nonnull Disposable parentDisposable) {
    addPropertyChangeListener(listener);
    Disposer.register(parentDisposable, () -> removePropertyChangeListener(listener));
  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    myPropertyChangeSupport.removePropertyChangeListener(listener);
  }

  @Override
  public void setColorsScheme(@Nonnull EditorColorsScheme scheme) {
    assertIsDispatchThread();
    myScheme = scheme;
    reinitSettings();
  }

  @Override
  @Nonnull
  public EditorColorsScheme getColorsScheme() {
    return myScheme;
  }

  @Override
  public int getFontSize() {
    return myScheme.getEditorFontSize();
  }

  @Override
  public void setViewer(boolean isViewer) {
    myIsViewer = isViewer;
  }

  @Nonnull
  @Override
  public EditorColorsScheme createBoundColorSchemeDelegate(@Nullable final EditorColorsScheme customGlobalScheme) {
    return new MyColorSchemeDelegate(customGlobalScheme);
  }

  @Override
  public void setHighlighter(@Nonnull final EditorHighlighter highlighter) {
    if (isReleased) return; // do not set highlighter to the released editor
    assertIsDispatchThread();
    final Document document = getDocument();
    Disposer.dispose(myHighlighterDisposable);

    document.addDocumentListener(highlighter);
    myHighlighterDisposable = () -> document.removeDocumentListener(highlighter);
    Disposer.register(myDisposable, myHighlighterDisposable);
    highlighter.setEditor(this);
    highlighter.setText(document.getImmutableCharSequence());
    if (!(highlighter instanceof EmptyEditorHighlighter)) {
      EditorHighlighterCache.rememberEditorHighlighterForCachesOptimization(document, highlighter);
    }
    myHighlighter = highlighter;
  }

  @Override
  public void repaint(int startOffset, int endOffset) {
    repaint(startOffset, endOffset, true);
  }

  @Override
  @Nonnull
  public DocumentEx getDocument() {
    return myDocument;
  }

  @Nonnull
  @Override
  public EditorHighlighter getHighlighter() {
    assertReadAccess();
    return myHighlighter;
  }

  @RequiredUIAccess
  public static void assertIsDispatchThread() {
    UIAccess.assertIsUIThread();
  }

  @RequiredReadAction
  public static void assertReadAccess() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
  }
}
