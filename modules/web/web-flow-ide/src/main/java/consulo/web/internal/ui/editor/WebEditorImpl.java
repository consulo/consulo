/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.internal.ui.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.codeEditor.*;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.codeEditor.impl.*;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.application.Application;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.web.internal.ui.image.WebImageElement;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.codeEditor.markup.LineMarkerPresentationContext;
import consulo.codeEditor.markup.LineMarkerPresentationProvider;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.MarkupModelListener;
import consulo.web.internal.ui.editor.gutter.GutterBand;
import consulo.web.internal.ui.editor.gutter.WebLineMarkerPresentationContext;
import consulo.web.internal.ui.editor.gutter.WebLineMarkerPresentationPainter;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.navigation.Navigatable;
import consulo.application.dumb.IndexNotReadyException;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.impl.internal.markup.AnalyzerStatus;
import consulo.language.editor.impl.internal.markup.AnalyzingType;
import consulo.language.editor.impl.internal.markup.ErrorStripeRenderer;
import consulo.language.editor.impl.internal.markup.PassWrapper;
import consulo.language.editor.impl.internal.markup.StatusItem;
import consulo.language.psi.util.EditSourceUtil;
import consulo.project.DumbService;
import consulo.ui.Point2D;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.util.lang.Pair;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.ide.impl.idea.codeInsight.navigation.CtrlMouseHandler;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import java.util.function.Supplier;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.LineStatusTrackerListener;
import consulo.versionControlSystem.internal.LineStatusTrackerManagerI;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.undoRedo.CommandProcessor;
import consulo.dataContext.DataContext;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.dataContext.DataManager;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.ui.color.ColorValue;
import consulo.web.internal.ui.WebColors;
import consulo.virtualFileSystem.VirtualFile;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.util.dataholder.Key;
import consulo.web.internal.ui.base.ComponentHolder;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.action.WebActionContextMenu;
import consulo.web.internal.ui.base.WebAwtBridgeComponent;
import org.jspecify.annotations.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebEditorImpl extends CodeEditorBase implements CaretPixelLocationProvider {
  public static class Vaadin extends ArquillEditorElement implements ComponentHolder, FromVaadinComponentWrapper {
    private consulo.ui.Component myComponent;

    public Vaadin(String text) {
      super(text);
    }

    @Override
    public consulo.ui.@Nullable Component toUIComponent() {
      return myComponent;
    }

    @Override
    public void setComponent(consulo.ui.Component component) {
      myComponent = component;
    }
  }

  private static class EditorComponent extends VaadinComponentDelegate<Vaadin> implements HasFocus {
    
    @Override
    public Vaadin createVaadinComponent() {
      Vaadin vaadin = new Vaadin("");
      vaadin.setComponent(this);
      return vaadin;
    }
  }

  private static final Key<Integer> ANNOTATION_ID = Key.create("annotation.id");

  private final EditorComponent myEditorComponent;

  private final WebEditorView myView;

  private final WebEditorGutterComponentImpl myGutterComponent;

  private final AtomicBoolean myUpdateScheduled = new AtomicBoolean();
  private final AtomicBoolean myTextAnnotationsUpdateScheduled = new AtomicBoolean();
  private boolean myPushingTextAnnotations;

  private volatile boolean myCaretFromClient;

  private volatile boolean myTextFromClient;

  private volatile @Nullable ClientEdit myClientEdit;

  /**
   * Where the caret is on screen, as the browser last reported it. Anything anchored to the caret - the completion
   * popup above all - is placed by this, because only the browser can measure where a character ended up.
   */
  private volatile @Nullable CaretPixelLocation myCaretLocation;

  /**
   * The character cell the browser measured, which every mapping between a position and a point is built on. The
   * defaults stand in only until the editor has reported - a zero cell would divide by zero the first time the
   * caret moved.
   */
  private volatile int myCharWidth = 8;
  private volatile int myLineHeight = 16;

  private final java.util.List<RangeHighlighter> myLinkHighlighters = new java.util.ArrayList<>();

  /** the browser identifies a clicked marker by its index here - a line carries more than one of them */
  private final java.util.List<GutterMark> myGutterMarks = new java.util.ArrayList<>();

  /** which lines already show a mark in place of their number, so the hovered offer is not drawn over one */
  private final java.util.Set<Integer> myLineNumberMarkLines = new java.util.HashSet<>();

  /** the line the last right click was over, or {@code -1} when it was not over the gutter */
  private int myGutterContextLine = -1;

  /** rebuilt on every inlay push - the browser answers a click with the place a run had in here */
  private final List<InlayClickTarget> myInlayClickTargets = new ArrayList<>();

  private boolean myLinkHovered;

  private @Nullable WebActionContextMenu myPopupMenu;

  /** the tracker is installed asynchronously, so it is picked up on the first push that finds it */
  private volatile @Nullable LineStatusTrackerI myLineStatusTracker;

  private final LineStatusTrackerListener myLineStatusTrackerListener = new LineStatusTrackerListener() {
    @Override
    public void onRangesChanged() {
      scheduleGutterBandsUpdate();
    }

    @Override
    public void onBecomingValid() {
      scheduleGutterBandsUpdate();
    }
  };

  @RequiredReadAction
  public WebEditorImpl(Document document, boolean viewer, @Nullable Project project, EditorKind kind) {
    super(document, viewer, project, kind);

    myGutterComponent = new WebEditorGutterComponentImpl(this);

    myView = new WebEditorView(this);
    myView.reset();

    Disposer.register(myDisposable, myView);

    // a provider hangs off a highlighter, and the daemon adds and drops those as the caret moves - the xml tag
    // tree marks the tag under the caret that way. without this the bands only ever refreshed with the whole
    // editor, which no caret move triggers
    if (project != null) {
      MarkupModel documentMarkup = DocumentMarkupModel.forDocument(myDocument, project, true);
      if (documentMarkup instanceof MarkupModelEx markupModelEx) {
        markupModelEx.addMarkupModelListener(myDisposable, new MarkupModelListener() {
          @Override
          public void afterAdded(RangeHighlighterEx highlighter) {
            scheduleIfPresentations(highlighter);
          }

          @Override
          public void afterRemoved(RangeHighlighterEx highlighter) {
            scheduleIfPresentations(highlighter);
          }

          @Override
          public void attributesChanged(RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
            if (renderersChanged) {
              scheduleIfPresentations(highlighter);
            }
          }

          private void scheduleIfPresentations(RangeHighlighterEx highlighter) {
            if (highlighter.getLineMarkerPresentationProvider() != null) {
              scheduleGutterBandsUpdate();
            }
          }
        });
      }
    }

    myEditorComponent = new EditorComponent();

    Vaadin vaadin = myEditorComponent.toVaadinComponent();
    vaadin.setSizeFull();
    vaadin.setText(myDocument.getText());
    vaadin.setReadOnly(isViewer() || !myDocument.isWritable());

    updateFont();
    updateCaretStyle();
    updateColors();

    // the awt editor learns of an inlay through a repaint of the region it covers, which here reaches a bridge
    // component and stops there - the model is the only thing left that knows. the pushes coalesce, so the hint
    // passes adding their inlays one by one still cost a single round trip
    myInlayModel.addListener(new InlayModel.Listener() {
      @Override
      public void onAdded(Inlay<?> inlay) {
        scheduleUpdate();
      }

      @Override
      public void onUpdated(Inlay<?> inlay, int changeFlags) {
        scheduleUpdate();
      }

      @Override
      public void onRemoved(Inlay<?> inlay) {
        scheduleUpdate();
      }

      @Override
      public void onBatchModeFinish(Editor editor) {
        scheduleUpdate();
      }
    }, myDisposable);

//    vaadin.addMouseDownListener(this::runMousePressedCommand);

    vaadin.addMetricsListener(event -> {
      if (event.getCharWidth() > 0 && event.getLineHeight() > 0) {
        myCharWidth = event.getCharWidth();
        myLineHeight = event.getLineHeight();
      }
    });

    vaadin.addCaretListener(event -> {
      myCaretLocation =
        new CaretPixelLocation(event.getCaretX(), event.getCaretY(), event.getCaretHeight(), event.getTextX());

      // where the caret is on screen always matters - a popup anchors to it - but only a move the user made is a
      // move. echoing back one the platform just made would look like the user left, and a lookup closes on that
      if (!event.isRectOnly()) {
        moveCaretFromClient(event.getOffset(), event.getSelectionStart(), event.getSelectionEnd());
      }
    });

    vaadin.addViewportListener(event -> ((WebScrollingModelImpl) myScrollingModel)
      .setVisibleAreaFromClient(new Rectangle(event.getX(), event.getY(), event.getWidth(), event.getHeight())));

    vaadin.addCtrlHoverListener(event -> highlightLinkAt(event.getOffset()));

    vaadin.addCtrlClickListener(event -> navigateTo(event.getOffset()));

    vaadin.addInlayClickListener(event -> performInlayClick(event.getId(), event.isControlDown()));

    vaadin.addGutterClickListener(event -> performGutterClick(event.getId()));

    vaadin.addGutterHoverListener(event -> performGutterHover(event.getLine()));

    vaadin.addGutterContextMenuListener(
      event -> performGutterContextMenu(event.getLine(), event.getMarkId(), event.getAnnotationColumn())
    );

    vaadin.addAnnotationHoverListener(
      event -> Application.get().runReadAction((Runnable)() -> performAnnotationHover(event.getLine()))
    );

    vaadin.addAnnotationClickListener(event -> performAnnotationClick(event.getLine(), event.getColumn()));

    vaadin.addGutterLineClickListener(event -> performGutterLineClick(
      event.getLine(),
      event.isAltKey(),
      event.isShiftKey(),
      event.isCtrlKey(),
      event.isMetaKey()
    ));

    vaadin.addFoldListener(event -> setFoldRegionExpandedFromClient(event.getStart(), !event.isCollapsed()));

    vaadin.addTypedListener(event -> handleTypedFromClient(event.getText()));

    vaadin.addTextChangeListener(event -> replaceTextFromClient(event.getStart(), event.getRemovedCharCount(), event.getText()));

    myCaretModel.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(CaretEvent e) {
        pushSelectionToClient();
      }
    }, myDisposable);

    // the caret and the selection move apart: shift with an arrow key grows the range and moves the caret, but
    // dragging past the anchor and back moves only the range. both are pushed the same way, and both push all of
    // it - whichever fires last leaves the browser holding the whole truth rather than half of an update
    getSelectionModel().addSelectionListener(new SelectionListener() {
      @Override
      public void selectionChanged(SelectionEvent e) {
        pushSelectionToClient();
      }
    }, myDisposable);

    // the highlighter re-lexes incrementally on its own, the view just has to be pushed again
    myDocument.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(DocumentEvent event) {
        if (myDocument.isInBulkUpdate()) {
          return;
        }

        // the browser already has its own edit on screen and echoing it back would fight the user's caret, but
        // only that one edit - the command it runs in produces more of them, the xml tag name synchronizer
        // mirroring a rename into the closing tag above all, and those have to reach the browser
        if (isEchoOfClientEdit(event)) {
          myClientEdit = null;
        }
        else {
          int start = event.getOffset();
          int end = start + event.getOldLength();
          String newFragment = event.getNewFragment().toString();

          // the caret goes with the text and after it. writing into the model moves what orion draws only as far
          // as the change forces it to, and where the caret belongs afterwards is the platform's to say - a tab
          // which indents ends up past the indent, not in front of it
          giveUI(() -> {
            vaadin.replaceText(start, end, newFragment);
            pushSelectionNow();
          });
        }

        scheduleUpdate();

        if (getGutterComponentEx().isAnnotationsShown()) {
          scheduleTextAnnotationsUpdate();
        }
      }

      @Override
      public void bulkUpdateFinished(Document document) {
        String text = document.getText();

        giveUI(() -> vaadin.setText(text));

        scheduleUpdate();
      }
    }, myDisposable);

    VirtualFile file = FileDocumentManager.getInstance().getFile(myDocument);
    if (file != null) {
      setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(file, getColorsScheme(), project));
    }

    // a reattach builds the browser side editor again, so everything that is not part of its input has to
    // be pushed once more
    vaadin.addAttachListener(attachEvent -> {
      updateFont();
      updateColors();

      update();

      // the browser is a fresh one and holds none of what the last one was sent, so the annotations have to be
      // pushed again even though they did not change
      vaadin.invalidatePushed("textAnnotations");

      Application.get().runReadAction((Runnable)this::updateTextAnnotations);
    });


    Disposer.register(myDisposable, () -> subscribeToLineStatusTracker(null));
  }

  /**
   * Hands the browser the caret and the selection together, off the ui thread it is called on.
   */
  private void pushSelectionToClient() {
    // the browser already moved its own caret when it made the edit
    if (myCaretFromClient || myTextFromClient) {
      return;
    }

    giveUI(this::pushSelectionNow);
  }

  /**
   * The same push, for callers already on the ui thread and already inside a {@link #giveUI} block.
   */
  private void pushSelectionNow() {
    Caret caret = myCaretModel.getPrimaryCaret();

    myEditorComponent.toVaadinComponent().setSelection(caret.getSelectionStart(), caret.getSelectionEnd());
  }

  /**
   * Moves the platform caret to where the browser put it, so the daemon can rerun the passes bound to the
   * caret position - identifier highlighting above all.
   * <p>
   * The selection comes with it. {@code moveToOffset} leaves whatever was selected alone, so a caret taken on its
   * own left the platform holding a range the user had long since dragged away from - and every action reading the
   * selection, delete first among them, worked on that one instead of what was on screen.
   */
  private void moveCaretFromClient(int offset, int selectionStart, int selectionEnd) {
    int length = myDocument.getTextLength();
    if (offset < 0 || offset > length || selectionStart < 0 || selectionEnd > length || selectionStart > selectionEnd) {
      return;
    }

    myCaretFromClient = true;
    try {
      CommandProcessor.getInstance().newCommand()
        .project(myProject)
        .document(myDocument)
        .run(() -> {
          Caret caret = myCaretModel.getPrimaryCaret();
          caret.moveToOffset(offset);

          if (selectionStart == selectionEnd) {
            caret.removeSelection();
          }
          else {
            caret.setSelection(selectionStart, selectionEnd);
          }
        });
    }
    finally {
      myCaretFromClient = false;
    }
  }

  /**
   * Applies an edit the user made in the browser to the platform document, so the psi, the daemon and the undo
   * stack see it. Offsets refer to the document as it was before the edit.
   */
  private void replaceTextFromClient(int start, int removedCharCount, String text) {
    Vaadin vaadin = myEditorComponent.toVaadinComponent();

    int end = start + removedCharCount;

    if (isViewer() || !myDocument.isWritable() || start < 0 || removedCharCount < 0 || end > myDocument.getTextLength()) {
      // the browser applied the edit on its own, pushing the platform text back is the only way to roll it back
      String documentText = myDocument.getText();

      giveUI(() -> vaadin.setText(documentText));
      return;
    }

    myTextFromClient = true;
    myClientEdit = new ClientEdit(start, removedCharCount, text);
    try {
      CommandProcessor.getInstance().newCommand()
        .project(myProject)
        .document(myDocument)
        .name(CodeEditorLocalize.typingInEditorCommandName())
        .inWriteAction()
        .run(() -> myDocument.replaceString(start, end, text));
    }
    finally {
      myTextFromClient = false;
      myClientEdit = null;
    }
  }

  /**
   * Runs a typed character through the platform, the way a keystroke does on the desktop.
   * <p/>
   * The browser has already been stopped from inserting it, so nothing is echoed back here - the change the typed
   * action makes travels to the browser as any other document change does. That is also what keeps the completion
   * lookup up while the user types: the lookup grows its prefix inside the guarded change the typed handler makes,
   * and only a change from outside one closes it.
   */
  @RequiredUIAccess
  private void handleTypedFromClient(String text) {
    if (isViewer() || !myDocument.isWritable() || text == null || text.length() != 1) {
      return;
    }

    // the caret is not taken from the browser here. the keystroke was stopped before the editor could act on it, so
    // the caret over there has not moved and reports the same offset for every character of a run - putting each of
    // them back at it would type the run out backwards. the caret on this side advances with each insert and is
    // pushed back to the browser by the caret listener, which is what keeps the two together
    EditorActionManager.getInstance().getTypedAction().actionPerformed(this, text.charAt(0), getDataContext());
  }

  /**
   * The edit the browser applied on its own, kept only until the document event carrying it arrives.
   */
  private record ClientEdit(int offset, int oldLength, String newText) {
  }

  /**
   * Where the caret was last seen on screen. Reported by the browser on every caret move, so it is already known by
   * the time something wants to open against it and nothing has to be asked for.
   */
  @Override
  public @Nullable CaretPixelLocation getCaretPixelLocation() {
    return myCaretLocation;
  }

  private boolean isEchoOfClientEdit(DocumentEvent event) {
    ClientEdit clientEdit = myClientEdit;

    return clientEdit != null
      && clientEdit.offset() == event.getOffset()
      && clientEdit.oldLength() == event.getOldLength()
      && clientEdit.newText().contentEquals(event.getNewFragment());
  }

  /**
   * Underlines the reference under the pointer while ctrl/cmd is held. The highlighter carries
   * {@link EditorColors#REFERENCE_HYPERLINK_COLOR}, so the existing style range push renders it.
   */
  private void highlightLinkAt(int offset) {
    boolean navigatable = Application.get().runReadAction((Supplier<Boolean>)() -> {
      clearLinkHighlighters();

      if (offset < 0 || myProject == null || offset > myDocument.getTextLength()) {
        return false;
      }

      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
      if (file == null) {
        return false;
      }

      CtrlMouseHandler.Info info = CtrlMouseHandler.getInfoAt(myProject, this, file, offset, CtrlMouseHandler.BrowseMode.Declaration);
      if (info == null || !info.isNavigatable()) {
        return false;
      }

      TextAttributes attributes = getColorsScheme().getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR);

      for (TextRange range : info.getRanges()) {
        myLinkHighlighters.add(myMarkupModel.addRangeHighlighter(
          range.getStartOffset(),
          range.getEndOffset(),
          HighlighterLayer.HYPERLINK,
          attributes,
          HighlighterTargetArea.EXACT_RANGE
        ));
      }

      return true;
    });

    // the browser cannot tell a resolvable reference from plain text, the hand cursor is switched on from here.
    // the hover fires once per character offset, so only a real change is worth a round trip
    if (myLinkHovered != navigatable) {
      myLinkHovered = navigatable;

      myEditorComponent.toVaadinComponent().setLinkHovered(navigatable);
    }
  }

  private void clearLinkHighlighters() {
    for (RangeHighlighter highlighter : myLinkHighlighters) {
      highlighter.dispose();
    }

    myLinkHighlighters.clear();
  }

  /**
   * Go to declaration, bound in the browser to ctrl/cmd click and middle click.
   */
  private void navigateTo(int offset) {
    if (myProject == null) {
      return;
    }

    clearLinkHighlighters();

    // a jump lands on a caret and selects nothing
    moveCaretFromClient(offset, offset, offset);

    PsiDocumentManager.getInstance(myProject).commitAllDocuments();

    DumbService dumbService = DumbService.getInstance(myProject);

    Navigatable navigatable = Application.get().runReadAction((Supplier<Navigatable>)() -> {
      dumbService.setAlternativeResolveEnabled(true);
      try {
        Pair<PsiElement[], GotoDeclarationHandler> found = GotoDeclarationAction.findAllTargetElementsInfo(myProject, this, offset);

        PsiElement[] elements = found.getFirst();
        // there is no web popup to disambiguate between several targets yet, the first one is taken
        if (elements == null || elements.length == 0) {
          return null;
        }

        // the raw target is not the declaration - the awt action resolves it through the same call
        PsiElement element = elements[0];
        PsiElement declaration = TargetElementUtil.getGotoDeclarationTarget(element, element.getNavigationElement());
        if (declaration == null) {
          declaration = element;
        }

        // a plain PsiElement is not navigable by itself, the descriptor is what opens the file
        return declaration instanceof Navigatable target ? target : EditSourceUtil.getDescriptor(declaration);
      }
      catch (IndexNotReadyException e) {
        return null;
      }
      finally {
        dumbService.setAlternativeResolveEnabled(false);
      }
    });

    if (navigatable == null || !navigatable.canNavigate()) {
      return;
    }

    // inside a command so that back navigation is recorded
    CommandProcessor.getInstance().newCommand()
      .project(myProject)
      .run(() -> navigatable.navigate(true));
  }

  /**
   * A click the browser reported on a run of a hint. The renderer owns what the run does - a type hint reaching
   * its class, a collapsed presentation opening - so the click is handed straight back to it.
   */
  @RequiredUIAccess
  private void performInlayClick(int id, boolean controlDown) {
    if (id < 0 || id >= myInlayClickTargets.size()) {
      return;
    }

    InlayClickTarget target = myInlayClickTargets.get(id);
    if (!target.inlay().isValid()) {
      return;
    }

    clearLinkHighlighters();

    target.inlay().getRenderer().handleClick(target.inlay(), target.contentIndex(), controlDown);
  }

  /**
   * Runs the left click action of a gutter marker, the same one the awt gutter fires from its mouse released
   * handler.
   */
  @RequiredUIAccess
  private void performGutterClick(int id) {
    if (id < 0 || id >= myGutterMarks.size()) {
      return;
    }

    if (!(myGutterMarks.get(id) instanceof GutterIconRenderer renderer)) {
      return;
    }

    AnAction action = renderer.getClickAction();
    if (action == null) {
      return;
    }

    ActionManagerEx actionManager = (ActionManagerEx)ActionManager.getInstance();

    DataContext context = getDataContext();

    AnActionEvent event = new AnActionEvent(
      null,
      context,
      ActionPlaces.EDITOR_GUTTER,
      action.getTemplatePresentation().clone(),
      actionManager,
      0
    );

    UIAccess uiAccess = UIAccess.current();

    ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false).whenCompleteAsync((enabled, throwable) -> {
      if (throwable != null || !Boolean.TRUE.equals(enabled)) {
        return;
      }

      actionManager.fireBeforeActionPerformed(action, context, event);
      actionManager.performActionDumbAware(action, event);
      actionManager.queueActionPerformedEvent(action, context, event);
    }, uiAccess);
  }

  private void performGutterLineClick(int line, boolean altKey, boolean shiftKey, boolean ctrlKey, boolean metaKey) {
    if (line < 0 || line >= myDocument.getLineCount()) {
      return;
    }

    EnumSet<ModifiedInputDetails.Modifier> modifiers = EnumSet.noneOf(ModifiedInputDetails.Modifier.class);
    if (altKey) {
      modifiers.add(ModifiedInputDetails.Modifier.ALT);
    }
    if (shiftKey) {
      modifiers.add(ModifiedInputDetails.Modifier.SHIFT);
    }
    if (ctrlKey) {
      modifiers.add(ModifiedInputDetails.Modifier.CTRL);
    }
    if (metaKey) {
      modifiers.add(ModifiedInputDetails.Modifier.META);
    }

    EditorMouseEvent event = gutterMouseEvent(line, new MouseInputDetails(
      new Point2D(0, visualLineToY(logicalToVisualPosition(new LogicalPosition(line, 0)).line)),
      new Point2D(0, 0),
      modifiers,
      MouseInputDetails.MouseButton.LEFT
    ));

    // a listener which tells a drag from a click is told the button went down first, and takes a click it was
    // never told about as the end of one
    for (EditorMouseListener listener : myMouseListeners) {
      listener.mousePressed(event);
    }

    for (EditorMouseListener listener : myMouseListeners) {
      listener.mouseClicked(event);
    }
  }

  /**
   * The pointer over the line numbers, which is what offers a breakpoint on the line it is over - the promoter of
   * the debugger listens for it and answers by putting an icon on the gutter component.
   *
   * @param line the line the pointer is over, or {@code -1} when it left the column
   */
  private void performGutterHover(int line) {
    boolean overGutter = line >= 0 && line < myDocument.getLineCount();

    int y = overGutter ? visualLineToY(logicalToVisualPosition(new LogicalPosition(line, 0)).line) : 0;

    MouseInputDetails details = new MouseInputDetails(
      new Point2D(0, y),
      new Point2D(0, 0),
      EnumSet.noneOf(ModifiedInputDetails.Modifier.class),
      MouseInputDetails.MouseButton.LEFT
    );

    // off the column the promoter is told about a move somewhere else, which is what makes it drop its icon
    EditorMouseEvent event = overGutter
      ? gutterMouseEvent(line, details)
      : new EditorMouseEvent(this, details, false, EditorMouseEventArea.EDITING_AREA);

    for (EditorMouseMotionListener listener : myMouseMotionListeners) {
      listener.mouseMoved(event);
    }
  }

  private EditorMouseEvent gutterMouseEvent(int line, MouseInputDetails details) {
    LogicalPosition logicalPosition = new LogicalPosition(line, 0);

    return new EditorMouseEvent(
      this,
      fakeEvent,
      details,
      false,
      EditorMouseEventArea.LINE_NUMBERS_AREA,
      myDocument.getLineStartOffset(line),
      logicalPosition,
      logicalToVisualPosition(logicalPosition),
      false,
      null,
      null,
      null
    );
  }

  @Override
  protected void onHighlighterChanged(
    RangeHighlighterEx highlighter,
    boolean canImpactGutterSize,
    boolean fontStyleChanged,
    boolean foregroundColorChanged
  ) {
    scheduleUpdate();
  }

  /** the traffic light is refreshed by the daemon on its own schedule, not through the markup listener */
  void scheduleAnalyzeStatusUpdate() {
    giveUI(() -> Application.get().runReadAction((Runnable)this::updateAnalyzeStatus));
  }

  /** the folding pass closes its batch off the ui thread, the browser can only be touched from it */
  void scheduleFoldRegionsUpdate() {
    giveUI(() -> Application.get().runReadAction((Runnable)this::updateFoldRegions));
  }

  /** the line status tracker recomputes its ranges on a pooled thread */
  void scheduleGutterBandsUpdate() {
    giveUI(() -> Application.get().runReadAction((Runnable)this::updateGutterBands));
  }

  void scheduleGutterHoverUpdate() {
    giveUI(this::updateHoverMark);
  }

  void scheduleTextAnnotationsUpdate() {
    // building the payload reads every line through the up to date line number provider, which touches the line
    // status tracker - and the tracker answers a change by asking every editor of its document to revalidate,
    // which lands back here. a push must not be what asks for the next one
    if (myPushingTextAnnotations) {
      return;
    }

    if (myTextAnnotationsUpdateScheduled.compareAndSet(false, true)) {
      boolean scheduled = giveUI(() -> {
        myTextAnnotationsUpdateScheduled.set(false);

        Application.get().runReadAction((Runnable)this::updateTextAnnotations);
      });

      if (!scheduled) {
        myTextAnnotationsUpdateScheduled.set(false);
      }
    }
  }

  /** the stripe settings - visibility, mark height - are pushed from wherever the platform changes them */
  void scheduleErrorStripeUpdate() {
    giveUI(() -> Application.get().runReadAction((Runnable)this::updateErrorStripeMarks));
  }

  /** the daemon adds its highlighters one by one, a single push per batch is enough */
  private void scheduleUpdate() {
    if (myUpdateScheduled.compareAndSet(false, true)) {
      boolean scheduled = giveUI(() -> {
        myUpdateScheduled.set(false);

        update();
      });

      // the flag is cleared by the runnable, so a push that was never scheduled - the editor is between a
      // detach and an attach - would latch it and every later batch of the daemon would be dropped
      if (!scheduled) {
        myUpdateScheduled.set(false);
      }
    }
  }

  /**
   * The group is only known once the file editor has configured the editor, so the popup is installed from here
   * rather than in the constructor.
   */
  @Override
  public void setContextMenuGroupId(@Nullable String groupId) {
    super.setContextMenuGroupId(groupId);

    if (groupId == null || myPopupMenu != null) {
      return;
    }

    DataManager dataManager = DataManager.getInstance();

    myPopupMenu = new WebActionContextMenu(
      myEditorComponent.toVaadinComponent(),
      groupId,
      ActionPlaces.EDITOR_POPUP,
      // the group is expanded off the ui thread, the providers have to be snapshotted before that
      () -> dataManager.createAsyncDataContext(gutterAwareDataContext(dataManager.getDataContext(myEditorComponent)))
    );
  }

  /**
   * The line a gutter action answers for is the one the pointer was over rather than the one the caret is on, and
   * {@code EditorGutterComponentImpl} publishes the same keys off its own last actionable click.
   */
  private DataContext gutterAwareDataContext(DataContext editorContext) {
    SimpleDataContext.Builder builder = SimpleDataContext.builder()
      .setParent(editorContext)
      .add(Editor.KEY, this);

    int line = myGutterContextLine;
    if (line >= 0) {
      builder.add(EditorGutter.KEY, getGutterComponentEx())
        .add(EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR, line);
    }

    return builder.build();
  }

  /**
   * The gutter carries a menu of its own, and a mark standing on the line carries one more.
   */
  private void performGutterContextMenu(int line, int markId, int annotationColumn) {
    myGutterContextLine = line;

    WebActionContextMenu popupMenu = myPopupMenu;
    if (popupMenu != null) {
      popupMenu.setOverrideGroup(line < 0 ? null : gutterPopupGroup(markId, line, annotationColumn));

      // the items are expanded off the pointer entering the target, which by the time a right click lands has
      // long since happened - so the group that just changed is expanded from here instead
      popupMenu.refresh();
    }
  }

  private @Nullable ActionGroup gutterPopupGroup(int markId, int line, int annotationColumn) {
    List<TextAnnotationGutterProvider> providers = myGutterComponent.getTextAnnotations();
    if (annotationColumn >= 0 && annotationColumn < providers.size()) {
      List<AnAction> actions = providers.get(annotationColumn).getPopupActions(line, this);
      if (!actions.isEmpty()) {
        return ActionGroup.newImmutableBuilder().addAll(actions).build();
      }
    }

    if (markId >= 0 && markId < myGutterMarks.size()
      && myGutterMarks.get(markId) instanceof GutterIconRenderer renderer) {
      ActionGroup renderetGroup = renderer.getPopupMenuActions();
      if (renderetGroup != null) {
        return renderetGroup;
      }
    }

    return CustomActionsSchemaImpl.getInstance().getCorrectedAction(IdeActions.GROUP_EDITOR_GUTTER)
      instanceof ActionGroup group ? group : null;
  }

  // due EditorMouseEvent use awt Event, we need set fake event, until migrate to own event system
  private static final MouseEvent fakeEvent = new MouseEvent(new JLabel("fake"), 0, 0, 0, 0, 0, 1, false);

  @Override
  public JComponent getComponent() {
    return WebAwtBridgeComponent.of(myEditorComponent);
  }

  @Override
  public JComponent getContentComponent() {
    return WebAwtBridgeComponent.of(myEditorComponent);
  }

  /**
   * Document and caret events arrive off the ui thread, the browser can only be touched from it. The ui is
   * the one the editor component sits in - a detached editor pushes nothing, its attach listener pushes
   * everything again anyway.
   */
  private boolean giveUI(Runnable runnable) {
    // the ui the editor component is attached to - a detached editor has none, and its attach listener pushes
    // everything again anyway
    UIAccess uiAccess = myEditorComponent.getUIAccess();
    if (uiAccess == null) {
      return false;
    }

    uiAccess.giveIfNeed(runnable);
    return true;
  }

  /**
   * The editor font of the scheme. Nothing of the scheme is readable in the browser, so the font travels the
   * same way the colors do - and the client has to relayout on it, orion caches the metrics it measured.
   */
  private void updateFont() {
    EditorColorsScheme scheme = getColorsScheme();

    String fontName = scheme.getEditorFontName();
    int fontSize = scheme.getEditorFontSize();
    // the row is the font times the spacing of the scheme, the way the awt editor sizes one - a fixed multiplier
    // made every row twice the text and a caret, which stands as tall as the row, twice the glyphs beside it
    float lineSpacing = scheme.getLineSpacing();

    giveUI(() -> myEditorComponent.toVaadinComponent().setFont(fontName, fontSize, lineSpacing));
  }

  /**
   * What the caret looks like, which the browser cannot be asked for - it draws a one pixel caret in the colour of
   * the text under it and blinks at a rate of its own, and the platform has a width and a blink period for it.
   */
  private void updateCaretStyle() {
    PersistentEditorSettings settings = PersistentEditorSettings.getInstance();

    int width = EditorUtil.getDefaultCaretWidth();
    int blinkPeriod = settings.isBlinkCaret() ? settings.getBlinkPeriod() : 0;

    giveUI(() -> myEditorComponent.toVaadinComponent().setCaretStyle(width, blinkPeriod));
  }

  private void updateColors() {
    EditorColorsScheme scheme = getColorsScheme();

    String background = WebColors.toCssColor(scheme.getDefaultBackground());
    String foreground = WebColors.toCssColor(scheme.getDefaultForeground());
    String selectionBackground = WebColors.toCssColor(scheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR));
    String selectionForeground = WebColors.toCssColor(scheme.getColor(EditorColors.SELECTION_FOREGROUND_COLOR));
    String caretRowBackground = WebColors.toCssColor(scheme.getColor(EditorColors.CARET_ROW_COLOR));

    // EditorGutterComponentImpl#getBackgroundColorValue, and the seam it draws over the gutter in paintComponent
    ColorValue gutterColor = scheme.getColor(EditorColors.EDITOR_GUTTER_BACKGROUND);
    String gutterBackground = gutterColor == null ? background : WebColors.toCssColor(gutterColor);
    String gutterSeparator = WebColors.toCssColor(scheme.getColor(EditorColors.INDENT_GUIDE_COLOR));

    // the gutter of the awt editor paints its line numbers with these two, so the ruler in the browser has to
    // be told about them as well - otherwise it keeps the colour the bundled orion stylesheet gives it
    String lineNumberColor = WebColors.toCssColor(scheme.getColor(EditorColors.LINE_NUMBERS_COLOR));
    String lineNumberCaretRowColor = WebColors.toCssColor(scheme.getColor(EditorColors.LINE_NUMBER_ON_CARET_ROW_COLOR));

    giveUI(() -> {
      ArquillEditorElement vaadin = myEditorComponent.toVaadinComponent();
      vaadin.setColors(background, foreground, selectionBackground, selectionForeground, caretRowBackground);
      vaadin.setGutterColors(gutterBackground, gutterSeparator);
      vaadin.setLineNumberColors(lineNumberColor, lineNumberCaretRowColor);
    });
  }

  public void update() {
    // the highlighter walk touches the document and the psi backed settings
    Application.get().runReadAction((Runnable)() -> {
      // the ruler needs the icons before it draws an anchor with them
      updateFoldingAnchors();

      // pushed first - the browser remaps every offset the other pushes carry once the projection changes
      updateFoldRegions();

      updateInlays();

      updateStyleRanges();
    });
  }

  /**
   * The inlays of the document, as the text they stand for. The browser puts each anchor into the view as a
   * projection, the way a fold placeholder gets there, so the offsets pushed here are the document ones and the
   * placement is spelled out rather than measured - nothing of the awt renderers survives the trip.
   * <p>
   * One entry per anchor offset. Two projections at one offset would render in the order the orion model happened
   * to splice them, and a block inlay below a line anchors exactly where a block inlay above the next line does,
   * so the ordering has to be settled here instead.
   */
  @RequiredReadAction
  private void updateInlays() {
    if (isReleased) {
      return;
    }

    int textLength = myDocument.getTextLength();
    int lineCount = myDocument.getLineCount();

    // sorted - the client keys its projections by anchor, and the bundle wants them in offset order
    Map<Integer, InlaySegments> anchors = new TreeMap<>();

    for (Inlay<?> inlay : myInlayModel.getBlockElementsInRange(0, textLength)) {
      InlayContent content = contentOf(inlay);
      if (content == null) {
        continue;
      }

      int line = myDocument.getLineNumber(inlay.getOffset());

      // a block inlay stands on a line of its own, which leaves it at the margin while the code it belongs to is
      // indented. the awt editor measures the indent and shifts the paint; here the text simply carries it
      String indent = lineIndent(line);

      if (inlay.getPlacement() == Inlay.Placement.ABOVE_LINE) {
        // the anchor is the start of the line the inlay sits above, and the break at the end of the text pushes
        // that line down - a document offset maps past the whole projection, so the line still answers for itself
        anchors.computeIfAbsent(myDocument.getLineStartOffset(line), it -> new InlaySegments()).addBlock(inlay, content, indent, false);
      }
      else if (line + 1 < lineCount) {
        anchors.computeIfAbsent(myDocument.getLineStartOffset(line + 1), it -> new InlaySegments())
          .addBlock(inlay, content, indent, false);
      }
      else {
        // below the last line there is no line start left to anchor to, so the break goes in front of the text
        anchors.computeIfAbsent(textLength, it -> new InlaySegments()).addBlock(inlay, content, indent, true);
      }
    }

    for (Inlay<?> inlay : myInlayModel.getInlineElementsInRange(0, textLength)) {
      InlayContent content = contentOf(inlay);
      if (content != null) {
        anchors.computeIfAbsent(inlay.getOffset(), it -> new InlaySegments()).addInline(inlay, content);
      }
    }

    for (Inlay<?> inlay : myInlayModel.getAfterLineEndElementsInRange(0, textLength)) {
      InlayContent content = contentOf(inlay);
      if (content != null) {
        int lineEndOffset = myDocument.getLineEndOffset(myDocument.getLineNumber(inlay.getOffset()));
        anchors.computeIfAbsent(lineEndOffset, it -> new InlaySegments()).addInline(inlay, content);
      }
    }

    boolean useEditorFontInInlays = EditorSettingsExternalizable.getInstance().isUseEditorFontInInlays();

    myInlayClickTargets.clear();

    StringBuilder inlays = new StringBuilder("[");

    for (Map.Entry<Integer, InlaySegments> entry : anchors.entrySet()) {
      if (inlays.length() > 1) {
        inlays.append(',');
      }

      inlays.append("{\"offset\":").append(entry.getKey()).append(",\"segments\":[");

      boolean first = true;
      for (InlaySegment segment : entry.getValue().flatten()) {
        // a run which is only an image projects no text, and an empty span would swallow the click of its neighbour
        if (segment.text().isEmpty() && !segment.lineBreak()) {
          continue;
        }

        if (!first) {
          inlays.append(',');
        }
        first = false;

        inlays.append("{\"text\":\"").append(escapeJson(segment.text())).append('"');

        // the break is a flag rather than a character - escapeJson turns a control character into a space, and a
        // raw one would abort the parse on the client
        if (segment.lineBreak()) {
          inlays.append(",\"br\":true");
        }

        // the indent of a block hint carries no look of its own - it stands in for the code below it and has to
        // measure like it
        if (!segment.styled()) {
          inlays.append('}');
          continue;
        }

        String style = toCssStyle(segment.attributesKey() == null ? null : getColorsScheme().getAttributes(segment.attributesKey()));

        // the awt editor measures a smaller hint against the editor font less one point, so the same one point
        // is what travels - the browser is already laying the editor font out at the size the scheme asked for
        String fontSize = segment.smallerFont()
          ? "\"fontSize\":\"" + Math.max(1, getColorsScheme().getEditorFontSize() - 1) + "px\""
          : null;

        // the box - padding, rounding, the margin holding it off the code - belongs to the stylesheet rather
        // than travelling as numbers. the scheme decides the colours, the frontend decides the shape, the same
        // split a gutter band is drawn under
        StringBuilder styleClass = new StringBuilder("arquill-inlay");
        if (segment.boxed()) {
          if (segment.first()) {
            styleClass.append(" arquill-inlay-start");
          }
          if (segment.last()) {
            styleClass.append(" arquill-inlay-end");
          }
        }

        // a hint is set in the ui font rather than the editor one unless the setting says otherwise - which is
        // what makes it read as a note about the code instead of as code. the awt editor takes the family from
        // the label font, and the browser has a ui font of its own to take instead
        if (!useEditorFontInInlays) {
          styleClass.append(" arquill-inlay-ui-font");
        }

        // a run which reaches an action is answered for by its place in the list, and the browser hands that place
        // back rather than a position - it never laid the hint out in offsets the platform knows
        if (segment.inlay() != null && segment.inlay().getRenderer().hasClickAction(segment.inlay(), segment.contentIndex())) {
          inlays.append(",\"click\":").append(myInlayClickTargets.size());

          myInlayClickTargets.add(new InlayClickTarget(segment.inlay(), segment.contentIndex()));
        }

        inlays.append(",\"style\":{\"styleClass\":\"").append(styleClass).append('"');

        if (style != null || fontSize != null) {
          inlays.append(",\"style\":{");
          if (style != null) {
            inlays.append(style, 1, style.length() - 1);
          }
          if (fontSize != null) {
            if (style != null) {
              inlays.append(',');
            }
            inlays.append(fontSize);
          }
          inlays.append('}');
        }

        inlays.append("}}");
      }

      inlays.append("]}");
    }

    inlays.append(']');

    myEditorComponent.toVaadinComponent().setInlays(inlays.toString());
  }

  /**
   * The indent a line opens with, as spaces. A tab cannot travel - the json escape turns a control character into
   * a space, and the payload the daemon messages share it with is why - so it is spent here against the tab size
   * instead, which lands the text on the column the code is on.
   */
  @RequiredReadAction
  private String lineIndent(int line) {
    int start = myDocument.getLineStartOffset(line);
    int end = myDocument.getLineEndOffset(line);

    CharSequence text = myDocument.getCharsSequence();
    int tabSize = Math.max(1, getSettings().getTabSize(myProject));

    StringBuilder indent = new StringBuilder();
    for (int offset = start; offset < end; offset++) {
      char c = text.charAt(offset);
      if (c == ' ') {
        indent.append(' ');
      }
      else if (c == '\t') {
        int spaces = tabSize - indent.length() % tabSize;
        indent.append(" ".repeat(spaces));
      }
      else {
        break;
      }
    }
    return indent.toString();
  }

  private static @Nullable InlayContent contentOf(Inlay<?> inlay) {
    if (!inlay.isValid()) {
      return null;
    }

    InlayContent content = inlay.getRenderer().getContent(inlay);
    if (content == null) {
      return null;
    }

    // the runs are kept as they came - a run which is only an image cannot be projected, but dropping it here
    // would shift every index after it, and the index is what a click is answered by
    for (InlayContentSegment segment : content.segments()) {
      if (!segment.text().isEmpty()) {
        return content;
      }
    }
    return null;
  }

  /**
   * One run of an anchor, once the placement of its inlay has been resolved into whether it ends a line.
   */
  /**
   * @param styled the run belongs to a hint rather than to the whitespace lining a block one up with the code -
   *               the indent has to stay in the editor font, a hint set in the ui font would measure it narrower
   *               and the hint would no longer sit over the declaration it belongs to
   * @param boxed  the hint is drawn as a box, which only the ones standing inside a line are. a block hint has a
   *               line to itself, and padding it would only push it off the column its code is on
   * @param first  the run opens a hint, {@code last} closes one - the edges are what get rounded and padded, so
   *               a hint of several runs still reads as the one box the awt editor paints
   */
  private record InlaySegment(
    String text,
    @Nullable TextAttributesKey attributesKey,
    boolean lineBreak,
    boolean smallerFont,
    boolean first,
    boolean last,
    boolean styled,
    boolean boxed,
    @Nullable Inlay<?> inlay,
    int contentIndex
  ) {
  }

  /**
   * A run the user can click, as the browser hands it back - the index of this record is what travels, the same
   * way a gutter mark is answered for by its place in {@link #myGutterMarks}.
   */
  private record InlayClickTarget(Inlay<?> inlay, int contentIndex) {
  }

  /**
   * The runs of one anchor. Block inlays come out ahead of the inline ones sharing the offset - they end in a
   * break, so anything after them would otherwise be carried onto their own line rather than staying on the code.
   */
  private static final class InlaySegments {
    private final List<InlaySegment> myBlock = new ArrayList<>();
    private final List<InlaySegment> myInline = new ArrayList<>();

    private void addBlock(Inlay<?> inlay, InlayContent content, String indent, boolean breakBefore) {
      boolean small = content.smallerFont();

      if (breakBefore) {
        myBlock.add(new InlaySegment("", null, true, small, false, false, false, false, null, -1));
      }

      // the indent stands outside the hint - it is the code the hint lines up with, so it stays plain and is
      // measured in the editor font like the line below it
      if (!indent.isEmpty()) {
        myBlock.add(new InlaySegment(indent, null, false, false, false, false, false, false, null, -1));
      }

      List<InlayContentSegment> segments = content.segments();
      for (int i = 0; i < segments.size(); i++) {
        InlayContentSegment segment = segments.get(i);
        boolean last = i == segments.size() - 1;
        myBlock.add(new InlaySegment(
          segment.text(),
          segment.attributesKey(),
          last && !breakBefore,
          small,
          i == 0,
          last,
          true,
          false,
          inlay,
          i
        ));
      }
    }

    private void addInline(Inlay<?> inlay, InlayContent content) {
      List<InlayContentSegment> segments = content.segments();
      for (int i = 0; i < segments.size(); i++) {
        InlayContentSegment segment = segments.get(i);
        myInline.add(new InlaySegment(
          segment.text(),
          segment.attributesKey(),
          false,
          content.smallerFont(),
          i == 0,
          i == segments.size() - 1,
          true,
          true,
          inlay,
          i
        ));
      }
    }

    private List<InlaySegment> flatten() {
      List<InlaySegment> all = new ArrayList<>(myBlock.size() + myInline.size());
      all.addAll(myBlock);
      all.addAll(myInline);
      return all;
    }
  }

  @RequiredReadAction
  private void updateStyleRanges() {
    EditorHighlighter editorHighlighter = getHighlighter();
    if (editorHighlighter == null) {
      return;
    }

    int textLength = myDocument.getTextLength();

    StringBuilder ranges = new StringBuilder("[");

    // a run that is nothing but a lexer token is pushed as classes named after the attribute keys of the
    // scheme, and the styles of those keys travel once as a stylesheet - the same keyword does not repeat
    // its resolved style through the whole file
    HighlighterIterator tokenIterator = editorHighlighter.createIterator(0);
    Set<TextAttributesKey> schemeKeys = new LinkedHashSet<>();

    // iteration state merges the lexer attributes with the markup model by layer, which is how the daemon
    // results - identifier highlighting, inspections - reach the view
    IterationState state = new IterationState(this, 0, textLength, null, false, false, false, false);
    while (!state.atEnd()) {
      int start = state.getStartOffset();
      int end = state.getEndOffset();

      String style = toCssStyle(state.getMergedAttributes());

      if (style != null) {
        while (!tokenIterator.atEnd() && tokenIterator.getEnd() <= start) {
          tokenIterator.advance();
        }

        // the run answers to the keys only when the markup added nothing over the token - what the merged
        // attributes render as is what the token renders as then. the attributes themselves cannot be
        // compared, their equals is the identity of an interned flyweight the merge does not go through
        String styleClass = null;
        if (!tokenIterator.atEnd() && tokenIterator.getStart() <= start && end <= tokenIterator.getEnd()) {
          TextAttributes tokenAttributes = tokenIterator.getTextAttributes();
          if (tokenAttributes != null && style.equals(toCssStyle(tokenAttributes))) {
            styleClass = toSchemeClasses(tokenIterator.getTextAttributesKeys(), schemeKeys);
          }
        }

        if (ranges.length() > 1) {
          ranges.append(',');
        }

        ranges.append("{\"start\":").append(start).append(",\"end\":").append(end);
        if (styleClass != null) {
          ranges.append(",\"style\":{\"styleClass\":\"").append(styleClass).append("\"}}");
        }
        else {
          ranges.append(",\"style\":{\"style\":").append(style).append("}}");
        }
      }

      state.advance();
    }

    ranges.append(']');

    Vaadin vaadin = myEditorComponent.toVaadinComponent();

    // the stylesheet has to be in the page before a range names a class of it
    vaadin.setSchemeStyles(buildSchemeCss(schemeKeys));
    vaadin.setStyleRanges(ranges.toString());

    updateTooltipRanges();

    updateGutterMarks();

    updateAnalyzeStatus();

    updateGutterBands();

    updateErrorStripeMarks();
  }

  /**
   * The marks of the error stripe, the narrow column right of the text - one per highlighter carrying an error
   * stripe color, the same set the awt {@code DesktopEditorErrorPanel} paints.
   * <p>
   * The awt panel maps an offset onto the strip itself through {@code visualLineToY}, which the web editor has
   * no implementation of - the stripe is the orion overview ruler, which squeezes the document into its own
   * height on its own. So the marks are pushed in document offsets and placed in the browser.
   */
  @RequiredReadAction
  private void updateErrorStripeMarks() {
    if (isReleased) {
      return;
    }

    WebEditorMarkupModelImpl markupModel = (WebEditorMarkupModelImpl)myMarkupModel;

    StringBuilder marks = new StringBuilder("[");

    if (markupModel.isErrorStripeVisible()) {
      // the panel merges the highlighters of the two models the same way, the document one carries the daemon
      // results and the editor one whatever was added against this editor alone
      appendErrorStripeMarks(marks, getFilteredDocumentMarkupModel().getAllHighlighters());
      appendErrorStripeMarks(marks, markupModel.getAllHighlighters());
    }

    marks.append(']');

    String json = "{\"visible\":" + markupModel.isErrorStripeVisible()
      + ",\"marks\":" + marks + "}";

    myEditorComponent.toVaadinComponent().setErrorStripeMarks(json);
  }

  @RequiredReadAction
  private void appendErrorStripeMarks(StringBuilder marks, RangeHighlighter[] highlighters) {
    EditorColorsScheme scheme = getColorsScheme();

    int textLength = myDocument.getTextLength();

    for (RangeHighlighter highlighter : highlighters) {
      if (!highlighter.isValid()) {
        continue;
      }

      ColorValue color = highlighter.getErrorStripeMarkColor(scheme);
      if (color == null) {
        continue;
      }

      int start = Math.max(0, Math.min(textLength, highlighter.getStartOffset()));
      int end = Math.max(start, Math.min(textLength, highlighter.getEndOffset()));

      if (marks.length() > 1) {
        marks.append(',');
      }

      marks.append("{\"line\":").append(myDocument.getLineNumber(start))
        .append(",\"start\":").append(start)
        .append(",\"end\":").append(end)
        .append(",\"layer\":").append(highlighter.getLayer())
        .append(",\"thin\":").append(highlighter.isThinErrorStripeMark())
        .append(",\"color\":\"").append(WebColors.toCssColor(color))
        .append("\",\"tooltip\":\"").append(escapeJson(toTooltipHtml(highlighter.getErrorStripeTooltip()))).append("\"}");
    }
  }

  /**
   * Pushes the regions the folding pass produced. The browser drives orion's own folding annotations from
   * them, which is what the folding ruler draws, and substitutes the placeholder for the collapsed text.
   */
  @RequiredReadAction
  private void updateFoldRegions() {
    if (isReleased) {
      return;
    }

    // the awt editor paints the placeholder with these, the same scheme key rather than a look of its own
    String placeholderStyle = toCssStyle(getColorsScheme().getAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES));

    StringBuilder regions = new StringBuilder("[");

    for (FoldRegion region : myFoldingModel.getAllFoldRegions()) {
      if (!region.isValid()) {
        continue;
      }

      int start = region.getStartOffset();
      int end = region.getEndOffset();

      if (regions.length() > 1) {
        regions.append(',');
      }

      regions.append("{\"start\":").append(start)
        .append(",\"end\":").append(end)
        .append(",\"collapsed\":").append(!region.isExpanded())
        .append(",\"anchor\":").append(isAnchorVisible(region))
        .append(",\"placeholder\":\"").append(escapeJson(region.getPlaceholderText())).append('"');

      // the same shape the style ranges carry, the client feeds both to the LineStyle event of the bundle
      if (placeholderStyle != null) {
        regions.append(",\"style\":{\"style\":").append(placeholderStyle).append('}');
      }

      regions.append('}');
    }

    regions.append(']');

    myEditorComponent.toVaadinComponent().setFoldRegions(regions.toString());
  }

  /**
   * Whether the folding ruler draws an anchor for the region. Mirrors
   * {@link consulo.codeEditor.impl.FoldingAnchorsOverlayStrategy} - a region which can never be opened is
   * folded without an anchor, and one which stays inside a single logical line is not marked unless it asked
   * to be. Without this every region of the pass gets a marker, which the awt ruler never shows.
   */
  @RequiredReadAction
  private boolean isAnchorVisible(FoldRegion region) {
    if (region.shouldNeverExpand()) {
      return false;
    }

    int startOffset = region.getStartOffset();
    int endOffset = region.getEndOffset();

    Document document = getDocument();
    if (document.getLineNumber(startOffset) != document.getLineNumber(endOffset)) {
      return true;
    }

    if (region.isGutterMarkEnabledForSingleLine()) {
      return true;
    }

    return getSettings().isAllowSingleLogicalLineFolding()
      && (endOffset - startOffset) > 1
      && !getSoftWrapModel().getSoftWrapsForRange(startOffset + 1, endOffset - 1).isEmpty();
  }

  /**
   * Hands the ruler the fold icons of the platform. They travel as the markup of a live image rather than a
   * url, so the tag in the ruler reloads itself when the style changes, the way every other icon of the page
   * does.
   */
  private void updateFoldingAnchors() {
    if (isReleased) {
      return;
    }

    // the three the awt gutter paints - see FoldingAnchorsOverlayStrategy and EditorGutterComponentImpl: an open
    // region is a bracket, drawn as a head on its first line and a foot on its last, and a folded one is a single
    // marker on the line its placeholder stands on
    String expanded = WebImageElement.toHtml(PlatformIconGroup.gutterFold());
    String collapsed = WebImageElement.toHtml(PlatformIconGroup.gutterUnfold());
    String expandedBottom = WebImageElement.toHtml(PlatformIconGroup.gutterFoldbottom());

    if (expanded == null || collapsed == null || expandedBottom == null) {
      return;
    }

    // the classes the ruler of the bundle puts on its own anchor - the stylesheet places the box and the hover
    // through them, and the image of the platform is what stands inside it
    myEditorComponent.toVaadinComponent().setFoldingAnchors(
      "<div class='annotationHTML expanded'>" + expanded + "</div>",
      "<div class='annotationHTML collapsed'>" + collapsed + "</div>",
      "<div class='annotationHTML expandedBottom'>" + expandedBottom + "</div>"
    );
  }

  /**
   * Takes every caret standing inside the region out of it, to the offset the region begins at, the way the awt
   * editor does when the gutter anchor of a region holding the caret is clicked.
   * <p>
   * A collapse with the caret inside it is refused by
   * {@link consulo.codeEditor.impl.CodeEditorFoldingModelBase#collapseFoldRegion}, and moving the caret back into
   * a region which did close reopens it - see the fold expansion scheduled by
   * {@link consulo.codeEditor.impl.CodeEditorCaretBase#moveToLogicalPosition}. Either way the region stays open
   * while the browser has already drawn it folded, and the push at the end of the batch takes that back: the
   * click reads as doing nothing. The caret is moved before the batch, so the collapse meets none of it.
   */
  @RequiredUIAccess
  private void moveCaretsOutOfRegion(FoldRegion region) {
    int startOffset = region.getStartOffset();
    int endOffset = region.getEndOffset();

    for (Caret caret : myCaretModel.getAllCarets()) {
      int offset = caret.getOffset();
      // the same range the folding model calls a caret inside a region - the offset it begins at is not in it
      if (offset > startOffset && offset < endOffset) {
        caret.moveToOffset(startOffset);
      }
    }
  }

  /**
   * Applies a collapse or expand the user made through the orion folding ruler. The batch end pushes the
   * regions back, so a change the platform refuses - a caret inside the region - is corrected in the browser.
   */
  @RequiredUIAccess
  private void setFoldRegionExpandedFromClient(int start, boolean expanded) {
    FoldRegion found = null;
    for (FoldRegion region : myFoldingModel.getAllFoldRegions()) {
      if (region.isValid() && region.getStartOffset() == start) {
        found = region;
        break;
      }
    }

    if (found == null) {
      return;
    }

    // the browser asked for the state it already has here, which means the two drifted apart - orion cannot
    // hold a folded region inside another folded one, so it keeps the inner one open. answering with nothing
    // leaves the click doing nothing forever, so what the platform holds is pushed again instead
    if (found.isExpanded() == expanded) {
      myEditorComponent.toVaadinComponent().invalidatePushed("foldRegions");

      updateFoldRegions();
      return;
    }

    FoldRegion region = found;

    if (!expanded) {
      moveCaretsOutOfRegion(region);
    }

    myFoldingModel.runBatchFoldingOperation(() -> region.setExpanded(expanded));

    // a region that never expands leaves the batch untouched, and then nothing pushed the state the browser
    // already applied back
    if (region.isValid() && region.isExpanded() != expanded) {
      Application.get().runReadAction((Runnable)this::updateFoldRegions);
    }
  }

  /**
   * Every line marker presentation of the document, as bands for the browser. The awt gutter paints these
   * itself; here the css draws them, so a painter turns a presentation into a {@link GutterBand} and the
   * providers - vcs, coverage, diff - never learn which frontend they built for.
   */
  @RequiredReadAction
  private void updateGutterBands() {
    if (isReleased) {
      return;
    }

    Vaadin vaadin = myEditorComponent.toVaadinComponent();

    if (myProject == null || myProject.isDisposed()) {
      vaadin.setGutterBands(List.of());
      return;
    }

    // the tracker installs itself asynchronously and publishes its highlighters when it does, so it is
    // picked up on the first pass that finds it
    subscribeToLineStatusTracker(LineStatusTrackerManagerI.getInstance(myProject).getLineStatusTracker(myDocument));

    List<GutterBand> bands = new ArrayList<>();

    MarkupModel markupModel = DocumentMarkupModel.forDocument(myDocument, myProject, false);
    if (markupModel != null) {
      for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
        collectBands(highlighter, bands);
      }
    }

    for (RangeHighlighter highlighter : getMarkupModel().getAllHighlighters()) {
      collectBands(highlighter, bands);
    }

    vaadin.setGutterBands(bands);
  }

  @RequiredReadAction
  private void collectBands(RangeHighlighter highlighter, List<GutterBand> bands) {
    LineMarkerPresentationProvider provider = highlighter.getLineMarkerPresentationProvider();
    if (provider == null || !highlighter.isValid()) {
      return;
    }

    LineMarkerPresentationContext context = new WebLineMarkerPresentationContext(this, highlighter);

    for (LineMarkerPresentation presentation : provider.buildPresentations(context)) {
      WebLineMarkerPresentationPainter painter = WebLineMarkerPresentationPainter.findPainter(presentation);
      if (painter == null) {
        continue;
      }

      GutterBand band = painter.paint(presentation, this);
      if (band != null) {
        bands.add(band);
      }
    }
  }

  private void subscribeToLineStatusTracker(@Nullable LineStatusTrackerI tracker) {
    LineStatusTrackerI subscribed = myLineStatusTracker;
    if (tracker == subscribed) {
      return;
    }

    if (subscribed != null) {
      subscribed.removeListener(myLineStatusTrackerListener);
    }

    myLineStatusTracker = tracker;

    if (tracker != null) {
      tracker.addListener(myLineStatusTrackerListener);
    }
  }

  /**
   * Error and warning counters of the current file, the same {@link AnalyzerStatus} the awt
   * {@code DesktopEditorAnalyzeStatusPanel} renders in the top right corner of the editor.
   */
  @RequiredReadAction
  private void updateAnalyzeStatus() {
    ErrorStripeRenderer renderer = ((WebEditorMarkupModelImpl)myMarkupModel).getErrorStripeRenderer();
    if (renderer == null) {
      myEditorComponent.toVaadinComponent().setAnalyzeStatus("{}");
      return;
    }

    AnalyzerStatus status = renderer.getStatus(this);

    java.util.List<StatusItem> items = status.getExpandedStatus();
    // a clean file produces no counters at all, the awt panel then shows the main icon on its own - the green
    // check, the eye while analyzing, the crossed out light when highlighting is off
    if (items.isEmpty()) {
      items = java.util.List.of(new StatusItem("", status.getIcon()));
    }

    StringBuilder json = new StringBuilder("{\"items\":[");

    boolean first = true;
    for (StatusItem item : items) {
      if (!first) {
        json.append(',');
      }
      first = false;

      json.append("{\"text\":\"").append(escapeJson(item.getText())).append('"');

      String iconHtml = toIconHtml(item.getIcon());
      if (iconHtml != null) {
        json.append(",\"iconHtml\":\"").append(escapeJson(iconHtml)).append('"');
      }

      json.append('}');
    }

    json.append("],\"tooltip\":\"").append(escapeJson(buildAnalyzeTooltip(status)))
      .append("\",\"analyzing\":").append(status.getAnalyzingType() != AnalyzingType.COMPLETE)
      .append('}');

    myEditorComponent.toVaadinComponent().setAnalyzeStatus(json.toString());
  }

  /**
   * The awt panel keeps this in the popup its widget opens - the daemon title, the progress of every running
   * pass and, below them, either the reason the analysis is not running or the severity summary. In the browser
   * the widget has no popup, so the same content is the tooltip of the panel.
   */
  private static String buildAnalyzeTooltip(AnalyzerStatus status) {
    String title = toHtmlContent(status.getTitle());
    String details = toHtmlContent(status.getDetails());
    String summary = status.getExpandedStatus().isEmpty() || status.getAnalyzingType() == AnalyzingType.EMPTY
      ? null
      : buildAnalyzeSummary(status);

    StringBuilder html = new StringBuilder();

    appendAnalyzeTooltipLine(html, title != null ? title : details != null ? details : summary);

    for (PassWrapper pass : status.getPasses()) {
      appendAnalyzeTooltipLine(html, XmlStringUtil.escapeText(pass.getPresentableName() + ": " + pass.toPercent() + "%"));
    }

    if (title != null) {
      appendAnalyzeTooltipLine(html, details != null ? details : summary);
    }

    return html.toString();
  }

  private static void appendAnalyzeTooltipLine(StringBuilder html, @Nullable String line) {
    if (line != null) {
      html.append("<div>").append(line).append("</div>");
    }
  }

  private static String buildAnalyzeSummary(AnalyzerStatus status) {
    StringBuilder summary = new StringBuilder();

    java.util.List<StatusItem> items = status.getExpandedStatus();
    for (int i = 0; i < items.size(); i++) {
      StatusItem item = items.get(i);

      if (i > 0) {
        summary.append(", ");
      }

      summary.append(item.getText());

      if (item.getType() != null) {
        summary.append(' ').append(item.getType());
      }
    }

    if (status.getAnalyzingType() != AnalyzingType.COMPLETE) {
      summary.append(' ').append(CodeEditorLocalize.iwFoundSoFarSuffix().get());
    }

    return XmlStringUtil.escapeText(summary);
  }

  /**
   * The daemon keeps the message of a highlight on the range highlighter, not in the attributes, so the tooltips
   * are collected from the markup model and pushed separately from the styles.
   */
  @RequiredReadAction
  private void updateTooltipRanges() {
    if (myProject == null) {
      return;
    }

    StringBuilder tooltips = new StringBuilder("[");

    for (RangeHighlighter highlighter : DocumentMarkupModel.forDocument(myDocument, myProject, true).getAllHighlighters()) {
      if (!(highlighter instanceof RangeHighlighterEx highlighterEx)) {
        continue;
      }

      String tooltip = toTooltipHtml(highlighterEx.getErrorStripeTooltip());
      if (tooltip == null) {
        continue;
      }

      if (tooltips.length() > 1) {
        tooltips.append(',');
      }

      tooltips.append("{\"start\":").append(highlighter.getStartOffset())
        .append(",\"end\":").append(highlighter.getEndOffset())
        .append(",\"html\":\"").append(escapeJson(tooltip)).append("\"}");
    }

    tooltips.append(']');

    myEditorComponent.toVaadinComponent().setTooltipRanges(tooltips.toString());
  }

  private static @Nullable String toTooltipHtml(@Nullable Object errorStripeTooltip) {
    String tooltip = switch (errorStripeTooltip) {
      case HighlightInfo info -> info.getToolTip().get();
      case String string -> string;
      case null, default -> null;
    };

    return toHtmlContent(tooltip);
  }

  /**
   * The platform wraps its messages in {@code <html>} for swing, which the browser would render as an unknown
   * element - and a message that carries no markup at all has to be escaped instead.
   */
  private static @Nullable String toHtmlContent(@Nullable String text) {
    if (text == null || text.isBlank()) {
      return null;
    }

    String content = XmlStringUtil.convertToHtmlContent(text);

    return content.isBlank() ? null : content;
  }

  /**
   * The line marker pass stores its results as range highlighters carrying a gutter icon renderer, in the same
   * document markup model as the highlighting, so they arrive through {@link #onHighlighterChanged}.
   */
  @RequiredReadAction
  private void updateGutterMarks() {
    if (myProject == null) {
      return;
    }

    myGutterMarks.clear();
    myLineNumberMarkLines.clear();

    StringBuilder marks = new StringBuilder("[");

    for (RangeHighlighter highlighter : DocumentMarkupModel.forDocument(myDocument, myProject, true).getAllHighlighters()) {
      GutterMark renderer = highlighter.getGutterIconRenderer();
      if (renderer == null) {
        continue;
      }

      String iconHtml = toIconHtml(renderer.getIcon());
      if (iconHtml == null) {
        continue;
      }

      int line = myDocument.getLineNumber(highlighter.getStartOffset());

      if (marks.length() > 1) {
        marks.append(',');
      }

      boolean onLineNumbers = renderer instanceof GutterIconRenderer iconRenderer
        && iconRenderer.getAlignment() == GutterIconRenderer.Alignment.LINE_NUMBERS;

      if (onLineNumbers) {
        myLineNumberMarkLines.add(line);
      }

      marks.append("{\"id\":").append(myGutterMarks.size())
        .append(",\"line\":").append(line)
        .append(",\"onLineNumbers\":").append(onLineNumbers)
        .append(",\"iconHtml\":\"").append(escapeJson(iconHtml)).append('"')
        .append(",\"tooltip\":\"").append(escapeJson(toHtmlContent(renderer.getTooltipValue().get()))).append("\"}");

      myGutterMarks.add(renderer);
    }

    marks.append(']');

    myEditorComponent.toVaadinComponent().setGutterMarks(marks.toString());

    updateHoverMark();
  }

  @RequiredReadAction
  private void updateTextAnnotations() {
    if (isReleased) {
      return;
    }

    List<TextAnnotationGutterProvider> providers = getGutterComponentEx().getTextAnnotations();

    Vaadin vaadin = myEditorComponent.toVaadinComponent();

    if (providers.isEmpty()) {
      vaadin.setTextAnnotations("null");
      return;
    }

    EditorColorsScheme scheme = getColorsScheme();
    int lineCount = Math.max(myDocument.getLineCount(), 1);

    StringBuilder json = new StringBuilder("{\"columns\":").append(providers.size()).append(",\"lines\":[");

    myPushingTextAnnotations = true;
    try {
      for (int line = 0; line < lineCount; line++) {
        if (line > 0) {
          json.append(',');
        }

        appendAnnotationCells(json, providers, scheme, line);
      }
    }
    finally {
      myPushingTextAnnotations = false;
    }

    json.append("]}");

    vaadin.setTextAnnotations(json.toString());
  }

  @RequiredReadAction
  private void appendAnnotationCells(
    StringBuilder json,
    List<TextAnnotationGutterProvider> providers,
    EditorColorsScheme scheme,
    int line
  ) {
    StringBuilder cells = new StringBuilder();
    boolean anyText = false;

    for (TextAnnotationGutterProvider provider : providers) {
      if (cells.length() > 0) {
        cells.append(',');
      }

      String text = provider.getLineText(line, this);
      if (text == null || text.isEmpty()) {
        cells.append("null");
        continue;
      }

      anyText = true;

      cells.append("{\"t\":\"").append(escapeJson(text)).append('"');

      EditorColorKey colorKey = provider.getColor(line, this);
      String color = colorKey == null ? null : WebColors.toCssColor(scheme.getColor(colorKey));
      if (color != null) {
        cells.append(",\"c\":\"").append(color).append('"');
      }

      String background = WebColors.toCssColor(provider.getBgColor(line, this));
      if (background != null) {
        cells.append(",\"g\":\"").append(background).append('"');
      }

      if (provider.getStyle(line, this) == EditorFontType.BOLD) {
        cells.append(",\"b\":true");
      }

      if (myGutterComponent.getTextAnnotationAction(provider) != null) {
        cells.append(",\"a\":true");
      }

      cells.append('}');
    }

    json.append(anyText ? "[" + cells + "]" : "null");
  }

  @RequiredReadAction
  private void performAnnotationHover(int line) {
    if (isReleased || line < 0 || line >= myDocument.getLineCount()) {
      myEditorComponent.toVaadinComponent().setAnnotationTooltip("null");
      return;
    }

    String html = null;
    for (TextAnnotationGutterProvider provider : myGutterComponent.getTextAnnotations()) {
      html = toHtmlContent(provider.getToolTipValue(line, this).get());
      if (html != null) {
        break;
      }
    }

    myEditorComponent.toVaadinComponent().setAnnotationTooltip(
      html == null ? "null" : "{\"line\":" + line + ",\"html\":\"" + escapeJson(html) + "\"}"
    );
  }

  @RequiredUIAccess
  private void performAnnotationClick(int line, int column) {
    List<TextAnnotationGutterProvider> providers = myGutterComponent.getTextAnnotations();
    if (line < 0 || column < 0 || column >= providers.size()) {
      return;
    }

    EditorGutterAction action = myGutterComponent.getTextAnnotationAction(providers.get(column));
    if (action != null) {
      action.doAction(line);
    }
  }

  void updateHoverMark() {
    JComponent gutter = getGutterComponentEx().getComponent();

    consulo.ui.image.Image icon = gutter.getClientProperty("line.number.hover.icon") instanceof consulo.ui.image.Image hovered
      ? hovered
      : null;

    Integer line = gutter.getClientProperty("active.line.number") instanceof Integer hoveredLine ? hoveredLine : null;

    String iconHtml = icon == null || line == null || myLineNumberMarkLines.contains(line) ? null : toIconHtml(icon);

    String json = iconHtml == null
      ? "null"
      : "{\"id\":-1,\"line\":" + line + ",\"onLineNumbers\":true,\"hover\":true,\"iconHtml\":\""
        + escapeJson(iconHtml) + "\",\"tooltip\":\"\"}";

    myEditorComponent.toVaadinComponent().setGutterHoverMark(json);
  }

  /**
   * The markup of the custom elements which apply the effects in the browser - the editor pushes its icons
   * inside a json payload and never gets a component to attach, so it needs the tree as a string.
   */
  private static @Nullable String toIconHtml(consulo.ui.image.@Nullable Image icon) {
    return icon == null ? null : WebImageElement.toHtml(icon);
  }

  private static String escapeJson(@Nullable String value) {
    if (value == null) {
      return "";
    }

    // a raw control character inside a json string aborts the whole JSON.parse on the client, and the daemon
    // messages do carry line breaks and tabs
    return value.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\r", " ")
      .replace("\n", " ")
      .replace("\t", " ");
  }

  private @Nullable String toCssStyle(@Nullable TextAttributes attributes) {
    if (attributes == null) {
      return null;
    }

    EditorColorsScheme scheme = getColorsScheme();

    StringBuilder style = new StringBuilder();

    ColorValue foreground = attributes.getForegroundColor();
    if (Objects.equals(foreground, scheme.getDefaultForeground())) {
      foreground = null;
    }

    ColorValue background = attributes.getBackgroundColor();
    if (Objects.equals(background, scheme.getDefaultBackground())) {
      background = null;
    }

    return toCssStyle(style, foreground, background, attributes.getFontType(), toCssTextDecoration(attributes));
  }

  /**
   * The effects of the attributes as one text-decoration. A highlighter of the daemon merges its effect into
   * the ones already on the run rather than replacing them - an error under an inspection underline carries
   * both - and only the first of them is what {@code getEffectType} answers.
   */
  private static @Nullable String toCssTextDecoration(TextAttributes attributes) {
    StringBuilder lines = new StringBuilder();
    StringBuilder style = new StringBuilder();
    StringBuilder color = new StringBuilder();

    attributes.forEachEffect((effectType, effectColor) -> {
      String decoration = toCssTextDecoration(effectType, effectColor);
      if (decoration == null) {
        return;
      }

      // css carries one line list, one style and one colour for the whole element, so several effects can
      // only be drawn together when they agree - the first one to name a style and a colour wins
      for (String part : decoration.split(" ")) {
        if (part.equals("underline") || part.equals("line-through")) {
          if (!lines.toString().contains(part)) {
            if (lines.length() > 0) {
              lines.append(' ');
            }
            lines.append(part);
          }
        }
        else if (part.startsWith("#")) {
          if (color.length() == 0) {
            color.append(part);
          }
        }
        else if (style.length() == 0) {
          style.append(part);
        }
      }
    });

    if (lines.length() == 0) {
      return null;
    }

    StringBuilder decoration = new StringBuilder(lines);
    if (style.length() > 0) {
      decoration.append(' ').append(style);
    }
    if (color.length() > 0) {
      decoration.append(' ').append(color);
    }
    return decoration.toString();
  }

  private static @Nullable String toCssStyle(
    StringBuilder style,
    @Nullable ColorValue foreground,
    @Nullable ColorValue background,
    int fontType,
    @Nullable String decoration
  ) {
    if (foreground != null) {
      style.append("\"color\":\"").append(WebColors.toCssColor(foreground)).append('"');
    }

    if (background != null) {
      if (style.length() > 0) {
        style.append(',');
      }
      style.append("\"backgroundColor\":\"").append(WebColors.toCssColor(background)).append('"');
    }

    if ((fontType & Font.BOLD) != 0) {
      if (style.length() > 0) {
        style.append(',');
      }
      style.append("\"fontWeight\":\"bold\"");
    }

    if ((fontType & Font.ITALIC) != 0) {
      if (style.length() > 0) {
        style.append(',');
      }
      style.append("\"fontStyle\":\"italic\"");
    }

    // without the effects an error reads as slightly differently coloured text, the wave is what makes it an error
    if (decoration != null) {
      if (style.length() > 0) {
        style.append(',');
      }
      style.append("\"textDecoration\":\"").append(decoration).append('"');
    }

    return style.length() == 0 ? null : "{" + style + "}";
  }

  private static @Nullable String toSchemeClasses(TextAttributesKey[] keys, Set<TextAttributesKey> collected) {
    if (keys.length == 0) {
      return null;
    }

    StringBuilder classes = new StringBuilder();
    for (TextAttributesKey key : keys) {
      collected.add(key);

      if (classes.length() > 0) {
        classes.append(' ');
      }
      classes.append(toSchemeClassName(key));
    }
    return classes.toString();
  }

  private static String toSchemeClassName(TextAttributesKey key) {
    return "arquill-a-" + key.getExternalName().replaceAll("[^A-Za-z0-9_-]", "-");
  }

  /**
   * The rules of the attribute keys the pushed ranges name. A key of the token stands later in its array the
   * higher it layers, and the rules keep that order, so the cascade resolves a token with several keys the
   * way the scheme merge does.
   */
  private String buildSchemeCss(Set<TextAttributesKey> keys) {
    EditorColorsScheme scheme = getColorsScheme();

    StringBuilder css = new StringBuilder();
    for (TextAttributesKey key : keys) {
      TextAttributes attributes = scheme.getAttributes(key);
      if (attributes == null) {
        continue;
      }

      css.append('.').append(toSchemeClassName(key)).append('{');

      ColorValue foreground = attributes.getForegroundColor();
      if (foreground != null && !Objects.equals(foreground, scheme.getDefaultForeground())) {
        css.append("color:").append(WebColors.toCssColor(foreground)).append(';');
      }

      ColorValue background = attributes.getBackgroundColor();
      if (background != null && !Objects.equals(background, scheme.getDefaultBackground())) {
        css.append("background-color:").append(WebColors.toCssColor(background)).append(';');
      }

      int fontType = attributes.getFontType();
      if ((fontType & Font.BOLD) != 0) {
        css.append("font-weight:bold;");
      }
      if ((fontType & Font.ITALIC) != 0) {
        css.append("font-style:italic;");
      }

      String decoration = toCssTextDecoration(attributes);
      if (decoration != null) {
        css.append("text-decoration:").append(decoration).append(';');
      }

      css.append("}\n");
    }
    return css.toString();
  }

  private static @Nullable String toCssTextDecoration(@Nullable EffectType effectType, @Nullable ColorValue effectColor) {
    // a scheme may name an effect and give it no colour of its own - the folded text of the default schemes
    // does - and the awt painter draws nothing at all then, the type alone is not an effect
    if (effectType == null || effectColor == null) {
      return null;
    }

    String color = " " + WebColors.toCssColor(effectColor);

    return switch (effectType) {
      case WAVE_UNDERSCORE -> "underline wavy" + color;
      case BOLD_DOTTED_LINE -> "underline dotted" + color;
      case LINE_UNDERSCORE, BOLD_LINE_UNDERSCORE -> "underline" + color;
      case STRIKEOUT -> "line-through" + color;
      // boxed effects have no text-decoration equivalent, the markup model draws them as outlines
      default -> null;
    };
  }


//  private Map<String, String> convertToCssProperties(TextAttributes textAttributes) {
//    Map<String, String> map = new HashMap<>();
//    ColorValue foregroundColor = textAttributes.getForegroundColor();
//    if (foregroundColor != null) {
//      RGBColorShared rgb = Mappers.map(foregroundColor.toRGB());
//      map.put("color", rgb.toString());
//    }
//
//    ColorValue backgroundColor = textAttributes.getBackgroundColor();
//    if (backgroundColor != null) {
//      RGBColorShared rgb = Mappers.map(backgroundColor.toRGB());
//      map.put("backgroundColor", rgb.toString());
//    }
//
//    int fontType = textAttributes.getFontType();
//    if (BitUtil.isSet(fontType, Font.BOLD)) {
//      map.put("fontWeight", "bold");
//    }
//
//    if (BitUtil.isSet(fontType, Font.ITALIC)) {
//      map.put("fontStyle", "italic");
//    }
//
//    return map;
//  }

//  private void runMousePressedCommand(final MouseDownEvent e) {
//    //myLastMousePressedLocation = xyToLogicalPosition(e.getPoint());
//    //myCaretStateBeforeLastPress = isToggleCaretEvent(e) ? myCaretModel.getCaretsAndSelections() : Collections.emptyList();
//    //myCurrentDragIsSubstantial = false;
//    //myDragStarted = false;
//    //clearDnDContext();
//
//    boolean forceProcessing = false;
//    //myMousePressedEvent = e;
//    MouseInputDetails.MouseButton mouseButton = MouseInputDetails.MouseButton.values()[e.getButton()];
//    Point2D position = new Point2D(e.getX(), e.getY());
//    EditorMouseEvent event =
//      new EditorMouseEvent(this,
//                           new MouseInputDetails(position,
//                                                 Point2D.OUT_OF_RANGE,
//                                                 EnumSet.noneOf(MouseInputDetails.Modifier.class),
//                                                 mouseButton),
//                           mouseButton == MouseInputDetails.MouseButton.RIGHT,
//                           EditorMouseEventArea.EDITING_AREA);
//
//    myExpectedCaretOffset = e.getTextOffset();
//    try {
//      for (EditorMouseListener mouseListener : myMouseListeners) {
//        boolean wasConsumed = event.isConsumed();
//        mouseListener.mousePressed(event);
//        //noinspection deprecation
//        if (!wasConsumed && event.isConsumed() && mouseListener instanceof consulo.ide.impl.idea.util.EditorPopupHandler) {
//          // compatibility with legacy code, this logic should be removed along with EditorPopupHandler
//          forceProcessing = true;
//        }
//        if (isReleased) return;
//      }
//    }
//    finally {
//      myExpectedCaretOffset = -1;
//    }
//
//    //if (event.getArea() == EditorMouseEventArea.LINE_MARKERS_AREA || event.getArea() == EditorMouseEventArea.FOLDING_OUTLINE_AREA && !isInsideGutterWhitespaceArea(e)) {
//    //  myDragOnGutterSelectionStartLine = EditorUtil.yPositionToLogicalLine(DesktopEditorImpl.this, e);
//    //}
//
//    if (event.isConsumed() && !forceProcessing) return;
//
//    if (myCommandProcessor != null) {
//      Runnable runnable = () -> {
//        if (processMousePressed(e) && myProject != null && !myProject.isDefault()) {
//          IdeDocumentHistory.getInstance(myProject).includeCurrentCommandAsNavigation();
//        }
//      };
//      myCommandProcessor.executeCommand(myProject,
//                                        runnable,
//                                        "",
//                                        DocCommandGroupId.noneGroupId(getDocument()),
//                                        UndoConfirmationPolicy.DEFAULT,
//                                        getDocument());
//    }
//    else {
//      processMousePressed(e);
//    }
//
//    invokePopupIfNeeded(event);
//  }

//  private boolean processMousePressed(MouseDownEvent e) {
//    CodeEditorCaretBase primaryCaret = getCaretModel().getPrimaryCaret();
//
//    primaryCaret.moveToOffset(e.getTextOffset());
//    return true;
//  }

  @Override
  protected void bulkUpdateFinished() {
    myView.reset();

    super.bulkUpdateFinished();
  }

  
  @Override
  public consulo.ui.Component getUIComponent() {
    return myEditorComponent;
  }

  
  @Override
  public Component getContentUIComponent() {
    return myEditorComponent;
  }

  @Override
  protected CodeEditorSelectionModelBase createSelectionModel() {
    return new WebSelectionModelImpl(this);
  }

  @Override
  protected MarkupModelImpl createMarkupModel() {
    return new WebEditorMarkupModelImpl(this);
  }

  @Override
  protected CodeEditorFoldingModelBase createFoldingModel() {
    return new WebFoldingModelImpl(this);
  }

  @Override
  protected CodeEditorCaretModelBase createCaretModel() {
    return new WebCaretModelImpl(this);
  }

  @Override
  protected CodeEditorScrollingModelBase createScrollingModel() {
    return new WebScrollingModelImpl(this);
  }

  @Override
  protected CodeEditorInlayModelBase createInlayModel() {
    return new WebInlayModelImpl(this);
  }

  @Override
  protected CodeEditorSoftWrapModelBase createSoftWrapModel() {
    return new WebSoftWrapModelImpl(this);
  }

  
  @Override
  protected DataContext getComponentContext() {
    return DataManager.getInstance().getDataContext(getUIComponent());
  }

  @Override
  protected void stopDumb() {

  }

  @Override
  public void release() {
    assertIsDispatchThread();
    if (isReleased) {
      throwDisposalError("Double release of editor:");
    }
    myTraceableDisposable.kill(null);

    isReleased = true;
    //mySizeAdjustmentStrategy.cancelAllRequests();
    //cancelAutoResetForMouseSelectionState();

    myFoldingModel.dispose();
    mySoftWrapModel.release();
    myMarkupModel.dispose();

    myScrollingModel.dispose();
    //myGutterComponent.dispose();
    //myMousePressedEvent = null;
    //myMouseMovedEvent = null;
    Disposer.dispose(myCaretModel);
    Disposer.dispose(mySoftWrapModel);
    Disposer.dispose(myView);
    //clearCaretThread();

    myFocusListeners.clear();
    myMouseListeners.clear();
    myMouseMotionListeners.clear();

    //myEditorComponent.removeMouseListener(myMouseListener);
    //myGutterComponent.removeMouseListener(myMouseListener);
    //myEditorComponent.removeMouseMotionListener(myMouseMotionListener);
    //myGutterComponent.removeMouseMotionListener(myMouseMotionListener);

    //CodeStyleSettingsManager.removeListener(myProject, this);

    Disposer.dispose(myDisposable);
    //myVerticalScrollBar.setUI(null); // clear error panel's cached image
  }

  @Override
  public int offsetToVisualLine(int offset, boolean beforeSoftWrap) {
    return 0;
  }

  @Override
  public int visualLineStartOffset(int visualLine) {
    return 0;
  }

  @Override
  public void startDumb() {

  }

  
  @Override
  public EditorGutterComponentEx getGutterComponentEx() {
    return myGutterComponent;
  }

  @Override
  public void setVerticalScrollbarOrientation(@MagicConstant(intValues = {VERTICAL_SCROLLBAR_LEFT, VERTICAL_SCROLLBAR_RIGHT}) int type) {

  }

  @Override
  public int getVerticalScrollbarOrientation() {
    return 0;
  }

  @Override
  public void setVerticalScrollbarVisible(boolean b) {

  }

  @Override
  public void setHorizontalScrollbarVisible(boolean b) {

  }

  @Override
  public void repaint(int startOffset, int endOffset, boolean invalidateTextLayout) {

  }

  @Override
  public void reinitSettings() {
    // the delegate keeps the global scheme it was created against, a scheme switch reaches the editor only
    // through this
    if (myScheme instanceof MyColorSchemeDelegate schemeDelegate) {
      schemeDelegate.updateGlobalScheme();
    }

    // the lexer highlighter caches the attributes of every token type against the scheme it was handed
    EditorHighlighter highlighter = getHighlighter();
    if (highlighter != null) {
      highlighter.setColorScheme(myScheme);
    }

    myView.reset();

    updateFont();
    updateColors();

    // the pushed ranges carry attributes resolved from the previous scheme
    update();
  }

  @Override
  public int getMaxWidthInRange(int startOffset, int endOffset) {
    return 0;
  }

  @Override
  public boolean setCaretVisible(boolean b) {
    return false;
  }

  @Override
  public boolean setCaretEnabled(boolean enabled) {
    return false;
  }

  @Override
  public void setFontSize(int fontSize) {

  }

  @Override
  public boolean isEmbeddedIntoDialogWrapper() {
    return false;
  }

  @Override
  public void setEmbeddedIntoDialogWrapper(boolean b) {

  }

  @Override
  public TextDrawingCallback getTextDrawingCallback() {
    return null;
  }

  @Override
  public int getPrefixTextWidthInPixels() {
    return 0;
  }

  @Override
  public void setCustomCursor(Object requestor, @Nullable Cursor cursor) {

  }

  @Override
  public int getLineHeight() {
    return myLineHeight;
  }

  @Override
  public int logicalPositionToOffset(LogicalPosition pos) {
    return myView.logicalPositionToOffset(pos);
  }

  @Override
  public int visualLineToY(int visualLine) {
    return visualLine * myLineHeight;
  }

  @Override
  public boolean isShowing() {
    return myEditorComponent.isVisible();
  }
  
  @Override
  public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) {
    return new VisualPosition(logicalToVisualLine(logicalPos.line), logicalPos.column, logicalPos.visualPositionLeansRight);
  }


  @Override
  public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) {
    return new LogicalPosition(visualToLogicalLine(visiblePos.getLine()), visiblePos.getColumn(), visiblePos.leansRight);
  }

  /**
   * What a collapsed region takes out of the document stands between a line and the row it is drawn at, so the two
   * only agree while nothing is folded.
   */
  private int logicalToVisualLine(int logicalLine) {
    int lineCount = myDocument.getLineCount();
    if (logicalLine <= 0 || lineCount == 0) {
      return Math.max(0, logicalLine);
    }

    int offset = myDocument.getLineStartOffset(Math.min(logicalLine, lineCount - 1));

    return Math.max(0, logicalLine - myFoldingModel.getFoldedLinesCountBefore(offset));
  }

  private int visualToLogicalLine(int visualLine) {
    if (visualLine <= 0) {
      return Math.max(0, visualLine);
    }

    int logicalLine = visualLine;

    // the regions come in the order of the document, so what each of them hides moves the line further down and
    // the one after it is measured against where the line has got to
    for (FoldRegion region : myFoldingModel.getAllFoldRegions()) {
      if (region.isExpanded() || !region.isValid()) {
        continue;
      }

      int startLine = myDocument.getLineNumber(region.getStartOffset());
      if (startLine >= logicalLine) {
        break;
      }

      logicalLine += Math.max(0, myDocument.getLineNumber(region.getEndOffset()) - startLine);
    }

    return Math.min(logicalLine, Math.max(0, myDocument.getLineCount() - 1));
  }

  
  @Override
  public LogicalPosition offsetToLogicalPosition(int offset) {
    return myView.offsetToLogicalPosition(offset);
  }

  
  @Override
  public VisualPosition offsetToVisualPosition(int offset) {
    LogicalPosition position = myView.offsetToLogicalPosition(offset);
    return logicalToVisualPosition(position);
  }

  
  @Override
  public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) {
    // todo impl
    return offsetToVisualPosition(offset);
  }

  
  @Override
  public EditorGutter getGutter() {
    return getGutterComponentEx();
  }

  @Override
  public boolean hasHeaderComponent() {
    return false;
  }

  @Override
  public @Nullable JComponent getHeaderComponent() {
    return null;
  }

  
  public LogicalPosition xyToLogicalPosition(java.awt.Point p) {
    // todo fake return
    return new LogicalPosition(0, 0);
  }

  
  @Override
  public java.awt.Point visualPositionToXY(VisualPosition visible) {
    return new Point(visible.column * myCharWidth, visualLineToY(visible.line));
  }

  /**
   * The inverse of {@link #visualPositionToXY}. Without it moving the caret up or down threw - the shared caret
   * code keeps the column it started from as an x and asks for it back, and an editor which cannot answer stops
   * the arrow keys dead.
   */
  @Override
  public VisualPosition xyToVisualPosition(Point p) {
    return new VisualPosition(Math.max(0, p.y / myLineHeight), Math.max(0, (p.x + myCharWidth / 2) / myCharWidth));
  }

  /**
   * The same two steps the awt editor takes. Left unimplemented it threw "Unsupported platform" from the interface
   * default, and brace highlighting asks for it on every caret move to decide whether the brace it matched has
   * scrolled out of sight - so the failure arrived once per keystroke rather than once per hint.
   */
  @Override
  public Point logicalPositionToXY(LogicalPosition pos) {
    return visualPositionToXY(logicalToVisualPosition(pos));
  }
}
