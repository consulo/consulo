package consulo.language.editor.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionManager;
import consulo.document.Document;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.OrderedSet;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ignatov
 */
public abstract class SmartEnterProcessorWithFixers extends SmartEnterProcessor {
  protected static final Logger LOG = Logger.getInstance(SmartEnterProcessorWithFixers.class);
  protected static final int MAX_ATTEMPTS = 20;
  protected static final Key<Long> SMART_ENTER_TIMESTAMP = Key.create("smartEnterOriginalTimestamp");

  protected int myFirstErrorOffset = Integer.MAX_VALUE;

  private final List<Fixer<? extends SmartEnterProcessorWithFixers>> myFixers = new ArrayList<Fixer<? extends SmartEnterProcessorWithFixers>>();
  private final List<FixEnterProcessor> myEnterProcessors = new ArrayList<FixEnterProcessor>();

  protected static void plainEnter(@Nonnull Editor editor) {
    getEnterHandler().execute(editor, ((EditorEx)editor).getDataContext());
  }

  protected static EditorActionHandler getEnterHandler() {
    return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_START_NEW_LINE);
  }

  protected static boolean isModified(@Nonnull Editor editor) {
    Long timestamp = editor.getUserData(SMART_ENTER_TIMESTAMP);
    assert timestamp != null;
    return editor.getDocument().getModificationStamp() != timestamp.longValue();
  }

  public boolean doNotStepInto(PsiElement element) {
    return false;
  }

  @Override
  public boolean process(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile psiFile) {
    Document document = editor.getDocument();
    String textForRollback = document.getText();
    try {
      editor.putUserData(SMART_ENTER_TIMESTAMP, editor.getDocument().getModificationStamp());
      myFirstErrorOffset = Integer.MAX_VALUE;
      process(project, editor, psiFile, 0);
    }
    catch (TooManyAttemptsException e) {
      document.replaceString(0, document.getTextLength(), textForRollback);
    }
    finally {
      editor.putUserData(SMART_ENTER_TIMESTAMP, null);
    }
    return true;
  }

  protected void process(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, int attempt)
    throws TooManyAttemptsException {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.complete.statement");
    if (attempt > MAX_ATTEMPTS) throw new TooManyAttemptsException();

    try {
      commit(editor);
      if (myFirstErrorOffset != Integer.MAX_VALUE) {
        editor.getCaretModel().moveToOffset(myFirstErrorOffset);
      }

      myFirstErrorOffset = Integer.MAX_VALUE;

      PsiElement atCaret = getStatementAtCaret(editor, file);
      if (atCaret == null) {
        return;
      }

      OrderedSet<PsiElement> queue = new OrderedSet<PsiElement>();
      collectAllElements(atCaret, queue, true);
      queue.add(atCaret);

      for (PsiElement psiElement : queue) {
        for (Fixer fixer : myFixers) {
          fixer.apply(editor, this, psiElement);
          if (LookupManager.getInstance(project).getActiveLookup() != null) {
            return;
          }
          if (isUncommited(project) || !psiElement.isValid()) {
            moveCaretInsideBracesIfAny(editor, file);
            process(project, editor, file, attempt + 1);
            return;
          }
        }
      }

      doEnter(atCaret, file, editor);
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  protected void collectAllElements(@Nonnull PsiElement element, @Nonnull OrderedSet<PsiElement> result, boolean recursive) {
    result.add(0, element);
    if (doNotStepInto(element)) {
      if (!recursive) return;
      recursive = false;
    }

    collectAdditionalElements(element, result);

    for (PsiElement child : element.getChildren()) {
      collectAllElements(child, result, recursive);
    }
  }

  protected void doEnter(@Nonnull PsiElement atCaret, @Nonnull PsiFile file, @Nonnull Editor editor) throws IncorrectOperationException {
    if (myFirstErrorOffset != Integer.MAX_VALUE) {
      editor.getCaretModel().moveToOffset(myFirstErrorOffset);
      reformat(atCaret);
      return;
    }

    reformat(atCaret);
    commit(editor);

    for (FixEnterProcessor enterProcessor : myEnterProcessors) {
      if (enterProcessor.doEnter(atCaret, file, editor, isModified(editor))) {
        return;
      }
    }

    if (!isModified(editor)) {
      plainEnter(editor);
    }
    else {
      if (myFirstErrorOffset == Integer.MAX_VALUE) {
        editor.getCaretModel().moveToOffset(atCaret.getTextRange().getEndOffset());
      }
      else {
        editor.getCaretModel().moveToOffset(myFirstErrorOffset);
      }
    }
  }

  protected void addEnterProcessors(FixEnterProcessor... processors) {
    ContainerUtil.addAllNotNull(myEnterProcessors, processors);
  }

  protected void addFixers(Fixer<? extends SmartEnterProcessorWithFixers>... fixers) {
    ContainerUtil.addAllNotNull(myFixers, fixers);
  }

  protected void collectAdditionalElements(@Nonnull PsiElement element, @Nonnull List<PsiElement> result) {
  }

  protected void moveCaretInsideBracesIfAny(@Nonnull Editor editor, @Nonnull PsiFile file) throws IncorrectOperationException {
  }

  public static class TooManyAttemptsException extends Exception {
  }

  public abstract static class Fixer<P extends SmartEnterProcessorWithFixers> {
    abstract public void apply(@Nonnull Editor editor, @Nonnull P processor, @Nonnull PsiElement element) throws IncorrectOperationException;
  }

  public abstract static class FixEnterProcessor {
    abstract public boolean doEnter(PsiElement atCaret, PsiFile file, @Nonnull Editor editor, boolean modified);
    
    protected void plainEnter(@Nonnull Editor editor) {
      SmartEnterProcessorWithFixers.plainEnter(editor);
    }
  }

  @Override
  public void commit(@Nonnull Editor editor) { // pull up
    super.commit(editor);
  }
}
