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
import consulo.codeEditor.*;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.impl.*;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.application.Application;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.web.internal.ui.image.WebImageWithURL;
import consulo.web.internal.ui.image.WebLayeredImageImpl;
import consulo.web.internal.ui.image.WebResizeImageImpl;
import consulo.web.internal.ui.image.WebTransparentImageImpl;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;

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
import consulo.ui.ex.action.ActionManager;
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
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.LineStatusTrackerListener;
import consulo.versionControlSystem.internal.LineStatusTrackerManagerI;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.undoRedo.CommandProcessor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.DocCommandGroupId;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebEditorImpl extends CodeEditorBase {
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

  private volatile boolean myCaretFromClient;

  private volatile boolean myTextFromClient;

  private volatile @Nullable ClientEdit myClientEdit;

  private final java.util.List<RangeHighlighter> myLinkHighlighters = new java.util.ArrayList<>();

  /** the browser identifies a clicked marker by its index here - a line carries more than one of them */
  private final java.util.List<GutterMark> myGutterMarks = new java.util.ArrayList<>();

  private boolean myLinkHovered;

  private @Nullable WebActionContextMenu myPopupMenu;

  /** the tracker is installed asynchronously, so it is picked up on the first push that finds it */
  private volatile @Nullable LineStatusTrackerI myLineStatusTracker;

  private final LineStatusTrackerListener myLineStatusTrackerListener = new LineStatusTrackerListener() {
    @Override
    public void onRangesChanged() {
      scheduleChangeBandsUpdate();
    }

    @Override
    public void onBecomingValid() {
      scheduleChangeBandsUpdate();
    }
  };

  @RequiredReadAction
  public WebEditorImpl(Document document, boolean viewer, @Nullable Project project, EditorKind kind) {
    super(document, viewer, project, kind);

    myGutterComponent = new WebEditorGutterComponentImpl(this);

    myView = new WebEditorView(this);
    myView.reset();

    Disposer.register(myDisposable, myView);

    myEditorComponent = new EditorComponent();

    Vaadin vaadin = myEditorComponent.toVaadinComponent();
    vaadin.setSizeFull();
    vaadin.setText(myDocument.getText());
    vaadin.setReadOnly(isViewer() || !myDocument.isWritable());

//    vaadin.addMouseDownListener(this::runMousePressedCommand);

    vaadin.addCaretListener(event -> moveCaretFromClient(event.getOffset()));

    vaadin.addCtrlHoverListener(event -> highlightLinkAt(event.getOffset()));

    vaadin.addCtrlClickListener(event -> navigateTo(event.getOffset()));

    vaadin.addGutterClickListener(event -> performGutterClick(event.getId()));

    vaadin.addErrorStripeClickListener(event -> performErrorStripeClick(event.getOffset()));

    vaadin.addFoldListener(event -> setFoldRegionExpandedFromClient(event.getStart(), !event.isCollapsed()));

    vaadin.addTextChangeListener(event -> replaceTextFromClient(event.getStart(), event.getRemovedCharCount(), event.getText()));

    myCaretModel.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(CaretEvent e) {
        // the browser already moved its own caret when it made the edit
        if (myCaretFromClient || myTextFromClient) {
          return;
        }

        int offset = e.getCaret().getOffset();

        giveUI(() -> vaadin.setCaretOffset(offset));
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

          giveUI(() -> vaadin.replaceText(start, end, newFragment));
        }

        scheduleUpdate();
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

    vaadin.addAttachListener(attachEvent -> update());

    Disposer.register(myDisposable, () -> subscribeToLineStatusTracker(null));
  }

  /**
   * Moves the platform caret to where the browser put it, so the daemon can rerun the passes bound to the
   * caret position - identifier highlighting above all.
   */
  private void moveCaretFromClient(int offset) {
    if (offset < 0 || offset > myDocument.getTextLength()) {
      return;
    }

    myCaretFromClient = true;
    try {
      CommandProcessor.getInstance().newCommand()
        .project(myProject)
        .document(myDocument)
        .run(() -> myCaretModel.getPrimaryCaret().moveToOffset(offset));
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
   * The edit the browser applied on its own, kept only until the document event carrying it arrives.
   */
  private record ClientEdit(int offset, int oldLength, String newText) {
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

    moveCaretFromClient(offset);

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

  /**
   * Jumps to the mark the user clicked in the error stripe, the counterpart of
   * {@code DesktopEditorMarkupModelImpl.doClick}.
   */
  @RequiredUIAccess
  private void performErrorStripeClick(int offset) {
    if (offset < 0 || offset > myDocument.getTextLength()) {
      return;
    }

    CommandProcessor.getInstance().newCommand()
      .project(myProject)
      .document(myDocument)
      .name(CodeEditorLocalize.moveCaretCommandName())
      .groupId(DocCommandGroupId.noneGroupId(myDocument))
      .run(() -> {
        myCaretModel.removeSecondaryCarets();
        myCaretModel.moveToOffset(offset);
        mySelectionModel.removeSelection();
      });

    // the awt panel scrolls the caret to the center afterwards, the web scrolling model is a stub - the caret
    // push is what reveals the line, orion scrolls to the offset it is given
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
  void scheduleChangeBandsUpdate() {
    giveUI(() -> Application.get().runReadAction((Runnable)this::updateChangeBands));
  }

  /** the stripe settings - visibility, mark height - are pushed from wherever the platform changes them */
  void scheduleErrorStripeUpdate() {
    giveUI(() -> Application.get().runReadAction((Runnable)this::updateErrorStripeMarks));
  }

  /** the daemon adds its highlighters one by one, a single push per batch is enough */
  private void scheduleUpdate() {
    if (myUpdateScheduled.compareAndSet(false, true)) {
      giveUI(() -> {
        myUpdateScheduled.set(false);

        update();
      });
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
      () -> dataManager.createAsyncDataContext(dataManager.getDataContext(myEditorComponent))
    );
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

  /** document and caret events arrive off the ui thread, the browser can only be touched from it */
  private static void giveUI(Runnable runnable) {
    UIAccess uiAccess = Application.get().getLastUIAccess();
    if (uiAccess != null) {
      uiAccess.giveIfNeed(runnable);
    }
  }

  public void update() {
    // the highlighter walk touches the document and the psi backed settings
    Application.get().runReadAction((Runnable)() -> {
      // pushed first - the browser remaps every offset the other pushes carry once the projection changes
      updateFoldRegions();

      updateStyleRanges();
    });
  }

  @RequiredReadAction
  private void updateStyleRanges() {
    if (getHighlighter() == null) {
      return;
    }

    int textLength = myDocument.getTextLength();

    StringBuilder ranges = new StringBuilder("[");

    // iteration state merges the lexer attributes with the markup model by layer, which is how the daemon
    // results - identifier highlighting, inspections - reach the view
    IterationState state = new IterationState(this, 0, textLength, null, false, false, false, false);
    while (!state.atEnd()) {
      String style = toCssStyle(state.getMergedAttributes());

      if (style != null) {
        if (ranges.length() > 1) {
          ranges.append(',');
        }

        ranges.append("{\"start\":").append(state.getStartOffset())
          .append(",\"end\":").append(state.getEndOffset())
          .append(",\"style\":{\"style\":").append(style).append("}}");
      }

      state.advance();
    }

    ranges.append(']');

    myEditorComponent.toVaadinComponent().setStyleRanges(ranges.toString());

    updateTooltipRanges();

    updateGutterMarks();

    updateAnalyzeStatus();

    updateChangeBands();

    updateErrorStripeMarks();
  }

  /**
   * The marks of the error stripe, the narrow column right of the text - one per highlighter carrying an error
   * stripe color, the same set the awt {@code DesktopEditorErrorPanel} paints.
   * <p>
   * The awt panel maps an offset onto the strip itself through {@code visualLineToY}, which the web editor has
   * no implementation of - the layout lives in the browser. So the marks are pushed in document lines and the
   * scaling onto the strip height is done on the client, where the line height and the strip height are known.
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
      + ",\"minMarkHeight\":" + markupModel.getMinMarkHeight()
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

      marks.append("{\"offset\":").append(start)
        .append(",\"line1\":").append(myDocument.getLineNumber(start))
        .append(",\"line2\":").append(myDocument.getLineNumber(end))
        .append(",\"layer\":").append(highlighter.getLayer())
        .append(",\"thin\":").append(highlighter.isThinErrorStripeMark())
        .append(",\"color\":\"").append(toCssColor(color))
        .append("\",\"tooltip\":\"").append(escapeJson(toTooltipHtml(highlighter.getErrorStripeTooltip()))).append("\"}");
    }
  }

  /**
   * Pushes the regions the folding pass produced. The browser drives orion's own folding annotations from
   * them, which is what owns the projection hiding the lines.
   */
  @RequiredReadAction
  private void updateFoldRegions() {
    if (isReleased) {
      return;
    }

    StringBuilder regions = new StringBuilder("[");

    for (FoldRegion region : myFoldingModel.getAllFoldRegions()) {
      if (!region.isValid()) {
        continue;
      }

      int start = region.getStartOffset();
      int end = region.getEndOffset();

      // orion folds whole lines - it hides from the start of the line after the region to the end of the
      // line the region ends on - so a region contained in a single line has nothing it could hide
      if (myDocument.getLineNumber(start) >= myDocument.getLineNumber(end)) {
        continue;
      }

      if (regions.length() > 1) {
        regions.append(',');
      }

      regions.append("{\"start\":").append(start)
        .append(",\"end\":").append(end)
        .append(",\"collapsed\":").append(!region.isExpanded())
        .append('}');
    }

    regions.append(']');

    myEditorComponent.toVaadinComponent().setFoldRegions(regions.toString());
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

    if (found == null || found.isExpanded() == expanded) {
      return;
    }

    FoldRegion region = found;

    myFoldingModel.runBatchFoldingOperation(() -> region.setExpanded(expanded));

    // a region that never expands leaves the batch untouched, and then nothing pushed the state the browser
    // already applied back
    if (region.isValid() && region.isExpanded() != expanded) {
      Application.get().runReadAction((Runnable)this::updateFoldRegions);
    }
  }

  /**
   * The vcs changed line ranges of the current file, the counterpart of the change bars the awt gutter paints
   * in its right free painters area.
   */
  @RequiredReadAction
  private void updateChangeBands() {
    if (isReleased) {
      return;
    }

    Vaadin vaadin = myEditorComponent.toVaadinComponent();

    if (myProject == null || myProject.isDisposed()) {
      vaadin.setChangeBands("[]");
      return;
    }

    LineStatusTrackerI tracker = LineStatusTrackerManagerI.getInstance(myProject).getLineStatusTracker(myDocument);

    subscribeToLineStatusTracker(tracker);

    java.util.List<VcsRange> ranges = tracker == null ? null : tracker.getRanges();
    if (ranges == null) {
      vaadin.setChangeBands("[]");
      return;
    }

    EditorColorsScheme scheme = getColorsScheme();

    StringBuilder bands = new StringBuilder("[");

    for (VcsRange range : ranges) {
      EditorColorKey colorKey = switch (range.getType()) {
        case VcsRange.INSERTED -> EditorColors.ADDED_LINES_COLOR;
        case VcsRange.DELETED -> EditorColors.DELETED_LINES_COLOR;
        case VcsRange.MODIFIED -> EditorColors.MODIFIED_LINES_COLOR;
        default -> null;
      };

      ColorValue color = colorKey == null ? null : scheme.getColor(colorKey);
      if (color == null) {
        continue;
      }

      if (bands.length() > 1) {
        bands.append(',');
      }

      // line2 is exclusive, and equal to line1 for a deletion - there are no lines left to cover, the
      // marker sits on the boundary between the two surviving ones
      bands.append("{\"line1\":").append(range.getLine1())
        .append(",\"line2\":").append(range.getLine2())
        .append(",\"color\":\"").append(toCssColor(color)).append("\"}");
    }

    bands.append(']');

    vaadin.setChangeBands(bands.toString());
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

      String iconUrls = toIconUrlsJson(item.getIcon());
      if (iconUrls != null) {
        json.append(",\"iconUrls\":").append(iconUrls);
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

    StringBuilder marks = new StringBuilder("[");

    for (RangeHighlighter highlighter : DocumentMarkupModel.forDocument(myDocument, myProject, true).getAllHighlighters()) {
      GutterMark renderer = highlighter.getGutterIconRenderer();
      if (renderer == null) {
        continue;
      }

      String iconUrls = toIconUrlsJson(renderer.getIcon());
      if (iconUrls == null) {
        continue;
      }

      int line = myDocument.getLineNumber(highlighter.getStartOffset());

      if (marks.length() > 1) {
        marks.append(',');
      }

      marks.append("{\"id\":").append(myGutterMarks.size())
        .append(",\"line\":").append(line)
        .append(",\"iconUrls\":").append(iconUrls)
        .append(",\"tooltip\":\"").append(escapeJson(toHtmlContent(renderer.getTooltipValue().get()))).append("\"}");

      myGutterMarks.add(renderer);
    }

    marks.append(']');

    myEditorComponent.toVaadinComponent().setGutterMarks(marks.toString());
  }

  /**
   * The browser can only show an image it has a url for, and the platform builds most markers by layering,
   * resizing and fading images together - run/debug line markers among them. Every leaf is emitted separately
   * and the client stacks them, which is what the awt gutter paints on a single graphics anyway.
   *
   * @return json array of urls, or null when nothing in the image resolves to one
   */
  private static @Nullable String toIconUrlsJson(consulo.ui.image.@Nullable Image icon) {
    java.util.List<String> urls = new java.util.ArrayList<>();
    collectIconUrls(icon, urls);

    if (urls.isEmpty()) {
      return null;
    }

    StringBuilder json = new StringBuilder("[");
    for (String url : urls) {
      if (json.length() > 1) {
        json.append(',');
      }
      json.append('"').append(escapeJson(url)).append('"');
    }
    return json.append(']').toString();
  }

  private static void collectIconUrls(consulo.ui.image.@Nullable Image icon, java.util.List<String> urls) {
    switch (icon) {
      case null -> {
      }
      case WebImageWithURL withURL -> urls.add(withURL.getImageURL());
      case WebLayeredImageImpl layered -> {
        for (consulo.ui.image.Image layer : layered.getImages()) {
          collectIconUrls(layer, urls);
        }
      }
      // the browser scales the stacked images to the marker box, and a faded layer is close enough at gutter size
      case WebResizeImageImpl resized -> collectIconUrls(resized.getOriginal(), urls);
      case WebTransparentImageImpl transparent -> collectIconUrls(transparent.getOriginal(), urls);
      default -> {
      }
    }
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

    return toCssStyle(style, foreground, background, attributes.getFontType(), attributes.getEffectType(), attributes.getEffectColor());
  }

  private static @Nullable String toCssStyle(
    StringBuilder style,
    @Nullable ColorValue foreground,
    @Nullable ColorValue background,
    int fontType,
    @Nullable EffectType effectType,
    @Nullable ColorValue effectColor
  ) {
    if (foreground != null) {
      style.append("\"color\":\"").append(toCssColor(foreground)).append('"');
    }

    if (background != null) {
      if (style.length() > 0) {
        style.append(',');
      }
      style.append("\"backgroundColor\":\"").append(toCssColor(background)).append('"');
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
    String decoration = toCssTextDecoration(effectType, effectColor);
    if (decoration != null) {
      if (style.length() > 0) {
        style.append(',');
      }
      style.append("\"textDecoration\":\"").append(decoration).append('"');
    }

    return style.length() == 0 ? null : "{" + style + "}";
  }

  private static @Nullable String toCssTextDecoration(@Nullable EffectType effectType, @Nullable ColorValue effectColor) {
    if (effectType == null) {
      return null;
    }

    String color = effectColor == null ? "" : " " + toCssColor(effectColor);

    return switch (effectType) {
      case WAVE_UNDERSCORE -> "underline wavy" + color;
      case BOLD_DOTTED_LINE -> "underline dotted" + color;
      case LINE_UNDERSCORE, BOLD_LINE_UNDERSCORE -> "underline" + color;
      case STRIKEOUT -> "line-through" + color;
      // boxed effects have no text-decoration equivalent, the markup model draws them as outlines
      default -> null;
    };
  }

  private static String toCssColor(ColorValue colorValue) {
    RGBColor color = colorValue.toRGB();
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
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
    myView.reset();
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
    return 0;
  }

  @Override
  public int logicalPositionToOffset(LogicalPosition pos) {
    return myView.logicalPositionToOffset(pos);
  }

  @Override
  public int visualLineToY(int visualLine) {
    return 0;
  }

  @Override
  public boolean isShowing() {
    return myEditorComponent.isVisible();
  }

  
  @Override
  public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) {
    return new VisualPosition(logicalPos.line, logicalPos.column, logicalPos.visualPositionLeansRight);
  }

  
  @Override
  public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) {
    return new LogicalPosition(visiblePos.getLine(), visiblePos.getColumn(), visiblePos.leansRight);
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

  
  public java.awt.Point visualPositionToXY(VisualPosition visible) {
    // todo fake return
    return new Point(1, 1);
  }
}
