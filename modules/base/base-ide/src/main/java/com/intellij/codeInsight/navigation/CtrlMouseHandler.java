// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.codeInsight.navigation.actions.GotoTypeDeclarationAction;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Comparing;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.codeInsight.TargetElementUtil;
import consulo.logging.Logger;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.concurrency.CancellablePromise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

@Singleton
public final class CtrlMouseHandler {
  private static final Logger LOG = Logger.getInstance(CtrlMouseHandler.class);

  private final Project myProject;

  private HighlightersSet myHighlighter;
  @JdkConstants.InputEventMask
  private int myStoredModifiers;
  private TooltipProvider myTooltipProvider;
  @Nullable
  private Point myPrevMouseLocation;
  private LightweightHint myHint;

  public enum BrowseMode {
    None,
    Declaration,
    TypeDeclaration,
    Implementation
  }

  private final KeyListener myEditorKeyListener = new KeyAdapter() {
    @Override
    public void keyPressed(final KeyEvent e) {
      handleKey(e);
    }

    @Override
    public void keyReleased(final KeyEvent e) {
      handleKey(e);
    }

    private void handleKey(final KeyEvent e) {
      int modifiers = e.getModifiers();
      if (modifiers == myStoredModifiers) {
        return;
      }

      BrowseMode browseMode = getBrowseMode(modifiers);

      if (browseMode == BrowseMode.None) {
        disposeHighlighter();
        cancelPreviousTooltip();
      }
      else {
        TooltipProvider tooltipProvider = myTooltipProvider;
        if (tooltipProvider != null) {
          if (browseMode != tooltipProvider.getBrowseMode()) {
            disposeHighlighter();
          }
          myStoredModifiers = modifiers;
          cancelPreviousTooltip();
          myTooltipProvider = new TooltipProvider(tooltipProvider);
          myTooltipProvider.execute(browseMode);
        }
      }
    }
  };

  private final VisibleAreaListener myVisibleAreaListener = __ -> {
    disposeHighlighter();
    cancelPreviousTooltip();
  };

  private final EditorMouseListener myEditorMouseAdapter = new EditorMouseListener() {
    @Override
    public void mouseReleased(@Nonnull EditorMouseEvent e) {
      disposeHighlighter();
      cancelPreviousTooltip();
    }
  };

  private final EditorMouseMotionListener myEditorMouseMotionListener = new EditorMouseMotionListener() {
    @Override
    public void mouseMoved(@Nonnull final EditorMouseEvent e) {
      if (e.isConsumed() || !myProject.isInitialized() || myProject.isDisposed()) {
        return;
      }
      MouseEvent mouseEvent = e.getMouseEvent();

      Point prevLocation = myPrevMouseLocation;
      myPrevMouseLocation = mouseEvent.getLocationOnScreen();
      if (isMouseOverTooltip(mouseEvent.getLocationOnScreen()) || ScreenUtil.isMovementTowards(prevLocation, mouseEvent.getLocationOnScreen(), getHintBounds())) {
        return;
      }
      cancelPreviousTooltip();

      myStoredModifiers = mouseEvent.getModifiers();
      BrowseMode browseMode = getBrowseMode(myStoredModifiers);

      if (browseMode == BrowseMode.None || e.getArea() != EditorMouseEventArea.EDITING_AREA) {
        disposeHighlighter();
        return;
      }

      Editor editor = e.getEditor();
      if (!(editor instanceof EditorEx) || editor.getProject() != null && editor.getProject() != myProject) return;
      Point point = new Point(mouseEvent.getPoint());
      if (!EditorUtil.isPointOverText(editor, point)) {
        disposeHighlighter();
        return;
      }
      myTooltipProvider = new TooltipProvider((EditorEx)editor, editor.xyToLogicalPosition(point));
      myTooltipProvider.execute(browseMode);
    }
  };

  @Inject
  public CtrlMouseHandler(@Nonnull Project project) {
    myProject = project;

    if(project.isDefault()) {
      // fixme [vistall] hack for javac bug with return inside constructor and lambda parameter
      // [203,6] error: variable __ might not have been initialized
      //return;
    }
    else {
      StartupManager.getInstance(project).registerPostStartupActivity((DumbAwareRunnable)() -> {
        EditorEventMulticaster eventMulticaster = EditorFactory.getInstance().getEventMulticaster();
        eventMulticaster.addEditorMouseListener(myEditorMouseAdapter, project);
        eventMulticaster.addEditorMouseMotionListener(myEditorMouseMotionListener, project);
        eventMulticaster.addCaretListener(new CaretListener() {
          @Override
          public void caretPositionChanged(@Nonnull CaretEvent e) {
            if (myHint != null) {
              DocumentationManager.getInstance(myProject).updateToolwindowContext();
            }
          }
        }, project);
      });
      project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
        @Override
        public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
          disposeHighlighter();
          cancelPreviousTooltip();
        }
      });
    }
  }

  private void cancelPreviousTooltip() {
    if (myTooltipProvider != null) {
      myTooltipProvider.dispose();
      myTooltipProvider = null;
    }
  }

  private boolean isMouseOverTooltip(@Nonnull Point mouseLocationOnScreen) {
    Rectangle bounds = getHintBounds();
    return bounds != null && bounds.contains(mouseLocationOnScreen);
  }

  @Nullable
  private Rectangle getHintBounds() {
    LightweightHint hint = myHint;
    if (hint == null) {
      return null;
    }
    JComponent hintComponent = hint.getComponent();
    if (!hintComponent.isShowing()) {
      return null;
    }
    return new Rectangle(hintComponent.getLocationOnScreen(), hintComponent.getSize());
  }

  @Nonnull
  private static BrowseMode getBrowseMode(@JdkConstants.InputEventMask int modifiers) {
    if (modifiers != 0) {
      final Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();
      if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_DECLARATION)) return BrowseMode.Declaration;
      if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_TYPE_DECLARATION)) return BrowseMode.TypeDeclaration;
      if (KeymapUtil.matchActionMouseShortcutsModifiers(activeKeymap, modifiers, IdeActions.ACTION_GOTO_IMPLEMENTATION)) return BrowseMode.Implementation;
    }
    return BrowseMode.None;
  }

  @Nullable
  @TestOnly
  public static String getInfo(PsiElement element, PsiElement atPointer) {
    return generateInfo(element, atPointer, true).text;
  }

  @Nullable
  @TestOnly
  public static String getInfo(@Nonnull Editor editor, BrowseMode browseMode) {
    Project project = editor.getProject();
    if (project == null) return null;
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;
    Info info = getInfoAt(project, editor, file, editor.getCaretModel().getOffset(), browseMode);
    return info == null ? null : info.getInfo().text;
  }

  @Nonnull
  private static DocInfo generateInfo(PsiElement element, PsiElement atPointer, boolean fallbackToBasicInfo) {
    final DocumentationProvider documentationProvider = DocumentationManager.getProviderFromElement(element, atPointer);
    String result = documentationProvider.getQuickNavigateInfo(element, atPointer);
    if (result == null && fallbackToBasicInfo) {
      result = doGenerateInfo(element);
    }
    return result == null ? DocInfo.EMPTY : new DocInfo(result, documentationProvider);
  }

  @Nullable
  private static String doGenerateInfo(@Nonnull PsiElement element) {
    if (element instanceof PsiFile) {
      final VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (virtualFile != null) {
        return virtualFile.getPresentableUrl();
      }
    }

    String info = getQuickNavigateInfo(element);
    if (info != null) {
      return info;
    }

    if (element instanceof NavigationItem) {
      final ItemPresentation presentation = ((NavigationItem)element).getPresentation();
      if (presentation != null) {
        return presentation.getPresentableText();
      }
    }

    return null;
  }

  @Nullable
  private static String getQuickNavigateInfo(PsiElement element) {
    final String name = ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE);
    if (StringUtil.isEmpty(name)) return null;
    final String typeName = ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE);
    final PsiFile file = element.getContainingFile();
    final StringBuilder sb = new StringBuilder();
    if (StringUtil.isNotEmpty(typeName)) sb.append(typeName).append(" ");
    sb.append("\"").append(name).append("\"");
    if (file != null && file.isPhysical()) {
      sb.append(" [").append(file.getName()).append("]");
    }
    return sb.toString();
  }

  public abstract static class Info {
    @Nonnull
    final PsiElement myElementAtPointer;
    @Nonnull
    private final List<TextRange> myRanges;

    public Info(@Nonnull PsiElement elementAtPointer, @Nonnull List<TextRange> ranges) {
      myElementAtPointer = elementAtPointer;
      myRanges = ranges;
    }

    public Info(@Nonnull PsiElement elementAtPointer) {
      this(elementAtPointer, getReferenceRanges(elementAtPointer));
    }

    @Nonnull
    private static List<TextRange> getReferenceRanges(@Nonnull PsiElement elementAtPointer) {
      if (!elementAtPointer.isPhysical()) return Collections.emptyList();
      int textOffset = elementAtPointer.getTextOffset();
      final TextRange range = elementAtPointer.getTextRange();
      if (range == null) {
        throw new AssertionError("Null range for " + elementAtPointer + " of " + elementAtPointer.getClass());
      }
      if (textOffset < range.getStartOffset() || textOffset < 0) {
        LOG.error("Invalid text offset " + textOffset + " of element " + elementAtPointer + " of " + elementAtPointer.getClass());
        textOffset = range.getStartOffset();
      }
      return Collections.singletonList(new TextRange(textOffset, range.getEndOffset()));
    }

    boolean isSimilarTo(@Nonnull Info that) {
      return Comparing.equal(myElementAtPointer, that.myElementAtPointer) && myRanges.equals(that.myRanges);
    }

    @Nonnull
    public List<TextRange> getRanges() {
      return myRanges;
    }

    @Nonnull
    public abstract DocInfo getInfo();

    public abstract boolean isValid(@Nonnull Document document);

    public abstract boolean isNavigatable();

    boolean rangesAreCorrect(@Nonnull Document document) {
      final TextRange docRange = new TextRange(0, document.getTextLength());
      for (TextRange range : getRanges()) {
        if (!docRange.contains(range)) return false;
      }

      return true;
    }
  }

  private static void showDumbModeNotification(@Nonnull Project project) {
    DumbService.getInstance(project).showDumbModeNotification("Element information is not available during index update");
  }

  private static class InfoSingle extends Info {
    @Nonnull
    private final PsiElement myTargetElement;

    InfoSingle(@Nonnull PsiElement elementAtPointer, @Nonnull PsiElement targetElement) {
      super(elementAtPointer);
      myTargetElement = targetElement;
    }

    InfoSingle(@Nonnull PsiReference ref, @Nonnull final PsiElement targetElement) {
      super(ref.getElement(), ReferenceRange.getAbsoluteRanges(ref));
      myTargetElement = targetElement;
    }

    @Override
    @Nonnull
    public DocInfo getInfo() {
      return areElementsValid() ? generateInfo(myTargetElement, myElementAtPointer, isNavigatable()) : DocInfo.EMPTY;
    }

    private boolean areElementsValid() {
      return myTargetElement.isValid() && myElementAtPointer.isValid();
    }

    @Override
    public boolean isValid(@Nonnull Document document) {
      return areElementsValid() && rangesAreCorrect(document);
    }

    @Override
    public boolean isNavigatable() {
      return myTargetElement != myElementAtPointer && myTargetElement != myElementAtPointer.getParent();
    }
  }

  private static class InfoMultiple extends Info {
    InfoMultiple(@Nonnull final PsiElement elementAtPointer) {
      super(elementAtPointer);
    }

    InfoMultiple(@Nonnull final PsiElement elementAtPointer, @Nonnull PsiReference ref) {
      super(elementAtPointer, ReferenceRange.getAbsoluteRanges(ref));
    }

    @Override
    @Nonnull
    public DocInfo getInfo() {
      return new DocInfo(CodeInsightBundle.message("multiple.implementations.tooltip"), null);
    }

    @Override
    public boolean isValid(@Nonnull Document document) {
      return rangesAreCorrect(document);
    }

    @Override
    public boolean isNavigatable() {
      return true;
    }
  }

  @Nullable
  private Info getInfoAt(@Nonnull final Editor editor, @Nonnull PsiFile file, int offset, @Nonnull BrowseMode browseMode) {
    return getInfoAt(myProject, editor, file, offset, browseMode);
  }

  @Nullable
  @RequiredReadAction
  public static Info getInfoAt(@Nonnull Project project, @Nonnull final Editor editor, @Nonnull PsiFile file, int offset, @Nonnull BrowseMode browseMode) {
    PsiElement targetElement = null;

    if (browseMode == BrowseMode.TypeDeclaration) {
      try {
        targetElement = GotoTypeDeclarationAction.findSymbolType(editor, offset);
      }
      catch (IndexNotReadyException e) {
        showDumbModeNotification(project);
      }
    }
    else if (browseMode == BrowseMode.Declaration) {
      final PsiReference ref = TargetElementUtil.findReference(editor, offset);
      final List<PsiElement> resolvedElements = ref == null ? Collections.emptyList() : resolve(ref);
      final PsiElement resolvedElement = resolvedElements.size() == 1 ? resolvedElements.get(0) : null;

      final PsiElement[] targetElements = GotoDeclarationAction.findTargetElementsNoVS(project, editor, offset, false);
      final PsiElement elementAtPointer = file.findElementAt(TargetElementUtil.adjustOffset(file, editor.getDocument(), offset));

      if (targetElements != null) {
        if (targetElements.length == 0) {
          return null;
        }
        else if (targetElements.length == 1) {
          if (targetElements[0] != resolvedElement && elementAtPointer != null && targetElements[0].isPhysical()) {
            return ref != null ? new InfoSingle(ref, targetElements[0]) : new InfoSingle(elementAtPointer, targetElements[0]);
          }
        }
        else {
          return elementAtPointer != null ? new InfoMultiple(elementAtPointer) : null;
        }
      }

      if (resolvedElements.size() == 1) {
        return new InfoSingle(ref, resolvedElements.get(0));
      }
      if (resolvedElements.size() > 1) {
        return elementAtPointer != null ? new InfoMultiple(elementAtPointer, ref) : null;
      }
    }
    else if (browseMode == BrowseMode.Implementation) {
      final PsiElement element = TargetElementUtil.findTargetElement(editor, ImplementationSearcher.getFlags(), offset);
      PsiElement[] targetElements = new ImplementationSearcher() {
        @Override
        @Nonnull
        protected PsiElement[] searchDefinitions(final PsiElement element, Editor editor) {
          final List<PsiElement> found = new ArrayList<>(2);
          DefinitionsScopedSearch.search(element, getSearchScope(element, editor)).forEach(psiElement -> {
            found.add(psiElement);
            return found.size() != 2;
          });
          return PsiUtilCore.toPsiElementArray(found);
        }
      }.searchImplementations(editor, element, offset);
      if (targetElements == null) {
        return null;
      }
      if (targetElements.length > 1) {
        PsiElement elementAtPointer = file.findElementAt(offset);
        if (elementAtPointer != null) {
          return new InfoMultiple(elementAtPointer);
        }
        return null;
      }
      if (targetElements.length == 1) {
        Navigatable descriptor = EditSourceUtil.getDescriptor(targetElements[0]);
        if (descriptor == null || !descriptor.canNavigate()) {
          return null;
        }
        targetElement = targetElements[0];
      }
    }

    if (targetElement != null && targetElement.isPhysical()) {
      PsiElement elementAtPointer = file.findElementAt(offset);
      if (elementAtPointer != null) {
        return new InfoSingle(elementAtPointer, targetElement);
      }
    }

    final PsiElement element = GotoDeclarationAction.findElementToShowUsagesOf(editor, offset);
    if (element != null) {
      PsiElement identifier = ((PsiNameIdentifierOwner)element).getNameIdentifier();
      if (identifier != null && identifier.isValid()) {
        DocInfo baseDocInfo = generateInfo(element, element, false);

        if(baseDocInfo != DocInfo.EMPTY && !StringUtil.isEmptyOrSpaces(baseDocInfo.text)) {
          return new Info(identifier) {
            @Nonnull
            @Override
            public DocInfo getInfo() {
              StringBuilder builder = new StringBuilder("<small>Show usages of </small><br>");
              builder.append(baseDocInfo.text);
              return new DocInfo(builder.toString(), null);
            }

            @Override
            public boolean isValid(@Nonnull Document document) {
              return true;
            }

            @Override
            public boolean isNavigatable() {
              return true;
            }
          };
        }
        else {
          return new Info(identifier) {
            @Nonnull
            @Override
            public DocInfo getInfo() {
              String name = UsageViewUtil.getType(element) + " '" + UsageViewUtil.getShortName(element) + "'";
              return new DocInfo("Show usages of " + name, null);
            }

            @Override
            public boolean isValid(@Nonnull Document document) {
              return element.isValid();
            }

            @Override
            public boolean isNavigatable() {
              return true;
            }
          };
        }
      }
    }
    return null;
  }

  @Nonnull
  @RequiredReadAction
  private static List<PsiElement> resolve(@Nonnull PsiReference ref) {
    // IDEA-56727 try resolve first as in GotoDeclarationAction
    PsiElement resolvedElement = ref.resolve();

    if (resolvedElement == null && ref instanceof PsiPolyVariantReference) {
      List<PsiElement> result = new ArrayList<>();
      final ResolveResult[] psiElements = ((PsiPolyVariantReference)ref).multiResolve(false);
      for (ResolveResult resolveResult : psiElements) {
        if (resolveResult.getElement() != null) {
          result.add(resolveResult.getElement());
        }
      }
      return result;
    }
    return resolvedElement == null ? Collections.emptyList() : Collections.singletonList(resolvedElement);
  }

  private void disposeHighlighter() {
    HighlightersSet highlighter = myHighlighter;
    if (highlighter != null) {
      myHighlighter = null;
      highlighter.uninstall();
      HintManager.getInstance().hideAllHints();
    }
  }

  private void updateText(@Nonnull String updatedText, @Nonnull Consumer<? super String> newTextConsumer, @Nonnull LightweightHint hint, @Nonnull Editor editor) {
    UIUtil.invokeLaterIfNeeded(() -> {
      // There is a possible case that quick doc control width is changed, e.g. it contained text
      // like 'public final class String implements java.io.Serializable, java.lang.Comparable<java.lang.String>' and
      // new text replaces fully-qualified class names by hyperlinks with short name.
      // That's why we might need to update the control size. We assume that the hint component is located at the
      // layered pane, so, the algorithm is to find an ancestor layered pane and apply new size for the target component.
      JComponent component = hint.getComponent();
      Dimension oldSize = component.getPreferredSize();
      newTextConsumer.consume(updatedText);


      Dimension newSize = component.getPreferredSize();
      if (newSize.width == oldSize.width) {
        return;
      }
      component.setPreferredSize(new Dimension(newSize.width, newSize.height));

      // We're assuming here that there are two possible hint representation modes: popup and layered pane.
      if (hint.isRealPopup()) {

        TooltipProvider tooltipProvider = myTooltipProvider;
        if (tooltipProvider != null) {
          // There is a possible case that 'raw' control was rather wide but the 'rich' one is narrower. That's why we try to
          // re-show the hint here. Benefits: there is a possible case that we'll be able to show nice layered pane-based balloon;
          // the popup will be re-positioned according to the new width.
          hint.hide();
          tooltipProvider.showHint(new LightweightHint(component), editor);
        }
        else {
          component.setPreferredSize(new Dimension(newSize.width, oldSize.height));
          hint.pack();
        }
        return;
      }

      Container topLevelLayeredPaneChild = null;
      boolean adjustBounds = false;
      for (Container current = component.getParent(); current != null; current = current.getParent()) {
        if (current instanceof JLayeredPane) {
          adjustBounds = true;
          break;
        }
        else {
          topLevelLayeredPaneChild = current;
        }
      }

      if (adjustBounds && topLevelLayeredPaneChild != null) {
        Rectangle bounds = topLevelLayeredPaneChild.getBounds();
        topLevelLayeredPaneChild.setBounds(bounds.x, bounds.y, bounds.width + newSize.width - oldSize.width, bounds.height);
      }
    });
  }


  private final class TooltipProvider {
    @Nonnull
    private final EditorEx myHostEditor;
    private final int myHostOffset;
    private BrowseMode myBrowseMode;
    private boolean myDisposed;
    private CancellablePromise<?> myExecutionProgress;

    TooltipProvider(@Nonnull EditorEx hostEditor, @Nonnull LogicalPosition hostPos) {
      myHostEditor = hostEditor;
      myHostOffset = hostEditor.logicalPositionToOffset(hostPos);
    }

    @SuppressWarnings("CopyConstructorMissesField")
    TooltipProvider(@Nonnull TooltipProvider source) {
      myHostEditor = source.myHostEditor;
      myHostOffset = source.myHostOffset;
    }

    void dispose() {
      myDisposed = true;
      if (myExecutionProgress != null) {
        myExecutionProgress.cancel();
      }
    }

    BrowseMode getBrowseMode() {
      return myBrowseMode;
    }

    void execute(@Nonnull BrowseMode browseMode) {
      myBrowseMode = browseMode;

      if (PsiDocumentManager.getInstance(myProject).getPsiFile(myHostEditor.getDocument()) == null) return;

      int selStart = myHostEditor.getSelectionModel().getSelectionStart();
      int selEnd = myHostEditor.getSelectionModel().getSelectionEnd();

      if (myHostOffset >= selStart && myHostOffset < selEnd) {
        disposeHighlighter();
        return;
      }

      myExecutionProgress = ReadAction.nonBlocking(() -> doExecute()).withDocumentsCommitted(myProject).expireWhen(() -> isTaskOutdated(myHostEditor))
              .finishOnUiThread(ModalityState.defaultModalityState(), Runnable::run).submit(AppExecutorUtil.getAppExecutorService());
    }

    private Runnable createDisposalContinuation() {
      return CtrlMouseHandler.this::disposeHighlighter;
    }

    @Nonnull
    private Runnable doExecute() {
      EditorEx editor = getPossiblyInjectedEditor();
      int offset = getOffset(editor);

      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
      if (file == null) return createDisposalContinuation();

      final Info info;
      final DocInfo docInfo;
      try {
        info = getInfoAt(editor, file, offset, myBrowseMode);
        if (info == null) return createDisposalContinuation();
        docInfo = info.getInfo();
      }
      catch (IndexNotReadyException e) {
        showDumbModeNotification(myProject);
        return createDisposalContinuation();
      }

      LOG.debug("Obtained info about element under cursor");
      return () -> addHighlighterAndShowHint(info, docInfo, editor);
    }

    @Nonnull
    private EditorEx getPossiblyInjectedEditor() {
      final Document document = myHostEditor.getDocument();
      if (PsiDocumentManager.getInstance(myProject).isCommitted(document)) {
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
        return (EditorEx)InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(myHostEditor, psiFile, myHostOffset);
      }
      return myHostEditor;
    }

    private boolean isTaskOutdated(@Nonnull Editor editor) {
      return myDisposed || myProject.isDisposed() || editor.isDisposed() || !ApplicationManager.getApplication().isUnitTestMode() && !editor.getComponent().isShowing();
    }

    private int getOffset(@Nonnull Editor editor) {
      return editor instanceof EditorWindow ? ((EditorWindow)editor).getDocument().hostToInjected(myHostOffset) : myHostOffset;
    }

    private void addHighlighterAndShowHint(@Nonnull Info info, @Nonnull DocInfo docInfo, @Nonnull EditorEx editor) {
      if (myDisposed || editor.isDisposed()) return;
      if (myHighlighter != null) {
        if (!info.isSimilarTo(myHighlighter.getStoredInfo())) {
          disposeHighlighter();
        }
        else {
          // highlighter already set
          if (info.isNavigatable()) {
            editor.setCustomCursor(CtrlMouseHandler.class, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }
          return;
        }
      }

      if (!info.isValid(editor.getDocument()) || !info.isNavigatable() && docInfo.text == null) {
        return;
      }

      boolean highlighterOnly = EditorSettingsExternalizable.getInstance().isShowQuickDocOnMouseOverElement() && DocumentationManager.getInstance(myProject).getDocInfoHint() != null;

      myHighlighter = installHighlighterSet(info, editor, highlighterOnly);

      if (highlighterOnly || docInfo.text == null) return;

      HyperlinkListener hyperlinkListener = docInfo.docProvider == null ? null : new QuickDocHyperlinkListener(docInfo.docProvider, info.myElementAtPointer);
      Ref<Consumer<? super String>> newTextConsumerRef = new Ref<>();
      JComponent component = HintUtil.createInformationLabel(docInfo.text, hyperlinkListener, null, newTextConsumerRef);
      component.setBorder(JBUI.Borders.empty(6, 6, 5, 6));

      final LightweightHint hint = new LightweightHint(wrapInScrollPaneIfNeeded(component, editor));

      myHint = hint;
      hint.addHintListener(__ -> myHint = null);

      showHint(hint, editor);

      Consumer<? super String> newTextConsumer = newTextConsumerRef.get();
      if (newTextConsumer != null) {
        updateOnPsiChanges(hint, info, newTextConsumer, docInfo.text, editor);
      }
    }

    @Nonnull
    private JComponent wrapInScrollPaneIfNeeded(@Nonnull JComponent component, @Nonnull Editor editor) {
      if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
        Dimension preferredSize = component.getPreferredSize();
        Dimension maxSize = getMaxPopupSize(editor);
        if (preferredSize.width > maxSize.width || preferredSize.height > maxSize.height) {
          // We expect documentation providers to exercise good judgement in limiting the displayed information,
          // but in any case, we don't want the hint to cover the whole screen, so we also implement certain limiting here.
          JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(component, true);
          scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width, maxSize.width), Math.min(preferredSize.height, maxSize.height)));
          return scrollPane;
        }
      }
      return component;
    }

    @Nonnull
    private Dimension getMaxPopupSize(@Nonnull Editor editor) {
      Rectangle rectangle = ScreenUtil.getScreenRectangle(editor.getContentComponent());
      return new Dimension((int)(0.9 * Math.max(640, rectangle.width)), (int)(0.33 * Math.max(480, rectangle.height)));
    }

    private void updateOnPsiChanges(@Nonnull LightweightHint hint, @Nonnull Info info, @Nonnull Consumer<? super String> textConsumer, @Nonnull String oldText, @Nonnull Editor editor) {
      if (!hint.isVisible()) return;
      Disposable hintDisposable = Disposable.newDisposable("CtrlMouseHandler.TooltipProvider.updateOnPsiChanges");
      hint.addHintListener(__ -> Disposer.dispose(hintDisposable));
      myProject.getMessageBus().connect(hintDisposable).subscribe(PsiModificationTracker.TOPIC, () -> ReadAction.nonBlocking(() -> {
        try {
          DocInfo newDocInfo = info.getInfo();
          return (Runnable)() -> {
            if (newDocInfo.text != null && !oldText.equals(newDocInfo.text)) {
              updateText(newDocInfo.text, textConsumer, hint, editor);
            }
          };
        }
        catch (IndexNotReadyException e) {
          showDumbModeNotification(myProject);
          return createDisposalContinuation();
        }
      }).finishOnUiThread(ModalityState.defaultModalityState(), Runnable::run).withDocumentsCommitted(myProject).expireWith(hintDisposable).expireWhen(() -> !info.isValid(editor.getDocument()))
              .coalesceBy(hint).submit(AppExecutorUtil.getAppExecutorService()));
    }

    public void showHint(@Nonnull LightweightHint hint, @Nonnull Editor editor) {
      if (ApplicationManager.getApplication().isUnitTestMode() || editor.isDisposed()) return;
      final HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
      short constraint = HintManager.ABOVE;
      LogicalPosition position = editor.offsetToLogicalPosition(getOffset(editor));
      Point p = HintManagerImpl.getHintPosition(hint, editor, position, constraint);
      if (p.y - hint.getComponent().getPreferredSize().height < 0) {
        constraint = HintManager.UNDER;
        p = HintManagerImpl.getHintPosition(hint, editor, position, constraint);
      }
      hintManager.showEditorHint(hint, editor, p, HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0, false,
                                 HintManagerImpl.createHintHint(editor, p, hint, constraint).setContentActive(false));
    }
  }

  @Nonnull
  private HighlightersSet installHighlighterSet(@Nonnull Info info, @Nonnull EditorEx editor, boolean highlighterOnly) {
    editor.getContentComponent().addKeyListener(myEditorKeyListener);
    editor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
    if (info.isNavigatable()) {
      editor.setCustomCursor(CtrlMouseHandler.class, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    List<RangeHighlighter> highlighters = new ArrayList<>();

    if (!highlighterOnly || info.isNavigatable()) {
      TextAttributes attributes = info.isNavigatable()
                                  ? EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR)
                                  : new TextAttributes(null, HintUtil.getInformationColor(), null, null, Font.PLAIN);
      for (TextRange range : info.getRanges()) {
        TextAttributes attr = NavigationUtil.patchAttributesColor(attributes, range, editor);
        final RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), HighlighterLayer.HYPERLINK, attr, HighlighterTargetArea.EXACT_RANGE);
        highlighters.add(highlighter);
      }
    }

    return new HighlightersSet(highlighters, editor, info);
  }

  @TestOnly
  public boolean isCalculationInProgress() {
    TooltipProvider provider = myTooltipProvider;
    if (provider == null) return false;
    Future<?> progress = provider.myExecutionProgress;
    if (progress == null) return false;
    return !progress.isDone();
  }

  private final class HighlightersSet {
    @Nonnull
    private final List<? extends RangeHighlighter> myHighlighters;
    @Nonnull
    private final EditorEx myHighlighterView;
    @Nonnull
    private final Info myStoredInfo;

    private HighlightersSet(@Nonnull List<? extends RangeHighlighter> highlighters, @Nonnull EditorEx highlighterView, @Nonnull Info storedInfo) {
      myHighlighters = highlighters;
      myHighlighterView = highlighterView;
      myStoredInfo = storedInfo;
    }

    public void uninstall() {
      for (RangeHighlighter highlighter : myHighlighters) {
        highlighter.dispose();
      }

      myHighlighterView.setCustomCursor(CtrlMouseHandler.class, null);
      myHighlighterView.getContentComponent().removeKeyListener(myEditorKeyListener);
      myHighlighterView.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
    }

    @Nonnull
    Info getStoredInfo() {
      return myStoredInfo;
    }
  }

  public static final class DocInfo {
    public static final DocInfo EMPTY = new DocInfo(null, null);

    @Nullable
    public final String text;
    @Nullable
    final DocumentationProvider docProvider;

    DocInfo(@Nullable String text, @Nullable DocumentationProvider provider) {
      this.text = text;
      docProvider = provider;
    }
  }

  private final class QuickDocHyperlinkListener implements HyperlinkListener {
    @Nonnull
    private final DocumentationProvider myProvider;
    @Nonnull
    private final PsiElement myContext;

    QuickDocHyperlinkListener(@Nonnull DocumentationProvider provider, @Nonnull PsiElement context) {
      myProvider = provider;
      myContext = context;
    }

    @Override
    public void hyperlinkUpdate(@Nonnull HyperlinkEvent e) {
      if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
        return;
      }

      String description = e.getDescription();
      if (StringUtil.isEmpty(description) || !description.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) {
        return;
      }

      String elementName = e.getDescription().substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());

      DumbService.getInstance(myProject).withAlternativeResolveEnabled(() -> {
        PsiElement targetElement = myProvider.getDocumentationElementForLink(PsiManager.getInstance(myProject), elementName, myContext);
        if (targetElement != null) {
          LightweightHint hint = myHint;
          if (hint != null) {
            hint.hide(true);
          }
          DocumentationManager.getInstance(myProject).showJavaDocInfo(targetElement, myContext, null);
        }
      });
    }
  }
}
