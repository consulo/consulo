// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.hint;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.Language;
import consulo.ide.impl.idea.lang.parameterInfo.LanguageParameterInfo;
import consulo.ide.impl.idea.lang.parameterInfo.ParameterInfoHandler;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.editor.EditorActivityManager;
import consulo.language.editor.hint.HintManager;
import consulo.project.DumbService;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.Pair;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.ide.impl.idea.ui.LightweightHint;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static consulo.ide.impl.idea.codeInsight.hint.ParameterInfoTaskRunnerUtil.runTask;

public class ShowParameterInfoHandler implements CodeInsightActionHandler {
  private static final ParameterInfoHandler[] EMPTY_HANDLERS = new ParameterInfoHandler[0];
  private final boolean myRequestFocus;

  public ShowParameterInfoHandler() {
    this(false);
  }

  public ShowParameterInfoHandler(boolean requestFocus) {
    myRequestFocus = requestFocus;
  }

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    invoke(project, editor, file, -1, null, myRequestFocus);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  /**
   * @deprecated use {@link #invoke(Project, Editor, PsiFile, int, PsiElement, boolean)} instead
   */
  @Deprecated
  public static void invoke(final Project project, final Editor editor, PsiFile file, int lbraceOffset, PsiElement highlightedElement) {
    invoke(project, editor, file, lbraceOffset, highlightedElement, false);
  }

  public static void invoke(final Project project, final Editor editor, PsiFile file, int lbraceOffset, PsiElement highlightedElement, boolean requestFocus) {
    invoke(project, editor, file, lbraceOffset, highlightedElement, requestFocus, false, CodeInsightBundle.message("parameter.info.progress.title"),
           e -> DumbService.getInstance(project).showDumbModeNotification(CodeInsightBundle.message("parameter.info.indexing.mode.not.supported")));
  }

  /**
   * @param progressTitle null means no loading panel should be shown
   */
  public static void invoke(final Project project,
                            final Editor editor,
                            PsiFile file,
                            int lbraceOffset,
                            Object highlightedElement,
                            boolean requestFocus,
                            boolean singleParameterHint,
                            @Nullable String progressTitle,
                            Consumer<IndexNotReadyException> indexNotReadyExceptionConsumer) {
    final DumbService dumbService = DumbService.getInstance(project);

    final int initialOffset = editor.getCaretModel().getOffset();

    Lookup lookup = LookupManager.getInstance(project).getActiveLookup();
    LookupElement lookupElement = lookup != null ? lookup.getCurrentItem() : null;

    runTask(project, ReadAction.nonBlocking(() -> {
      final int offset = editor.getCaretModel().getOffset();
      final int fileLength = file.getTextLength();
      if (fileLength == 0) return null;

      // file.findElementAt(file.getTextLength()) returns null but we may need to show parameter info at EOF offset (for example in SQL)
      final int offsetForLangDetection = offset > 0 && offset == fileLength ? offset - 1 : offset;
      final Language language = PsiUtilCore.getLanguageAtOffset(file, offsetForLangDetection);

      final ShowParameterInfoContext context = new ShowParameterInfoContext(editor, project, file, offset, lbraceOffset, requestFocus, singleParameterHint);

      context.setHighlightedElement(highlightedElement);
      context.setRequestFocus(requestFocus);

      final ParameterInfoHandler<PsiElement, Object>[] handlers = ObjectUtils.notNull(getHandlers(project, language, file.getViewProvider().getBaseLanguage()), EMPTY_HANDLERS);

      if (lookup != null) {
        if (lookupElement != null) {
          for (ParameterInfoHandler<PsiElement, Object> handler : handlers) {
            if (handler.couldShowInLookup()) {
              final Object[] items = handler.getParametersForLookup(lookupElement, context);
              if (items != null && items.length > 0) {
                return (Runnable)() -> {
                  showLookupEditorHint(items, editor, handler, requestFocus);
                };
              }
              return null;
            }
          }
        }
        return null;
      }

      return dumbService.computeWithAlternativeResolveEnabled(() -> {
        try {
          for (int i = 0; i < handlers.length; i++) {
            ParameterInfoHandler<PsiElement, Object> handler = handlers[i];
            PsiElement element = handler.findElementForParameterInfo(context);
            if (element != null) {
              return (Runnable)() -> {
                if (element.isValid()) {
                  handler.showParameterInfo(element, context);
                }
              };
            }
          }
        }
        catch (IndexNotReadyException e) {
          indexNotReadyExceptionConsumer.accept(e);
        }
        return null;
      });
    }).withDocumentsCommitted(project).expireWhen(() -> editor.getCaretModel().getOffset() != initialOffset).coalesceBy(ShowParameterInfoHandler.class, editor), continuation -> {
      if (continuation != null) {
        continuation.run();
      }
    }, progressTitle, editor);
  }

  private static void showLookupEditorHint(Object[] descriptors, final Editor editor, ParameterInfoHandler handler, boolean requestFocus) {
    ParameterInfoComponent component = new ParameterInfoComponent(descriptors, editor, handler, requestFocus, false);
    component.update(false);

    final LightweightHint hint = new LightweightHint(component);
    hint.setSelectingHint(true);
    final HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
    final Pair<Point, Short> pos = ParameterInfoController.chooseBestHintPosition(editor, null, hint, HintManager.DEFAULT, true);
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!EditorActivityManager.getInstance().isVisible(editor)) return;
      hintManager.showEditorHint(hint, editor, pos.getFirst(), HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE | HintManager.UPDATE_BY_SCROLLING, 0, false, pos.getSecond());
    });
  }

  @Nullable
  public static ParameterInfoHandler[] getHandlers(Project project, final Language... languages) {
    Set<ParameterInfoHandler> handlers = new LinkedHashSet<>();
    DumbService dumbService = DumbService.getInstance(project);
    for (final Language language : languages) {
      handlers.addAll(dumbService.filterByDumbAwareness(LanguageParameterInfo.INSTANCE.allForLanguage(language)));
    }
    if (handlers.isEmpty()) return null;
    return handlers.toArray(new ParameterInfoHandler[0]);
  }
}

