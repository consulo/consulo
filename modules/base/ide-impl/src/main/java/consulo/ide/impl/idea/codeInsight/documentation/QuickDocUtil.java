// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.documentation;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.progress.SensitiveProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.codeInsight.navigation.DocPreviewUtil;
import consulo.ide.impl.idea.openapi.editor.EditorMouseHoverPopupManager;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.util.lang.ObjectUtil;
import consulo.ui.ex.awt.util.SingleAlarm;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiQualifiedNamedElement;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.ref.Ref;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static consulo.application.impl.internal.progress.ProgressIndicatorUtils.runInReadActionWithWriteActionPriority;

/**
 * @author gregsh
 */
public class QuickDocUtil {

  public static void updateQuickDoc(@Nonnull final Project project, @Nonnull final PsiElement element, @Nullable final String documentation) {
    if (StringUtil.isEmpty(documentation)) return;
    // modal dialogs with fragment editors fix: can't guess proper modality state here
    UIUtil.invokeLaterIfNeeded(() -> {
      DocumentationComponent component = getActiveDocComponent(project);
      if (component != null) {
        component.replaceText(documentation, element);
      }
    });
  }

  @Nullable
  public static DocumentationComponent getActiveDocComponent(@Nonnull Project project) {
    DocumentationManager documentationManager = DocumentationManager.getInstance(project);
    DocumentationComponent component;
    JBPopup hint = documentationManager.getDocInfoHint();
    if (hint != null) {
      component = (DocumentationComponent)((AbstractPopup)hint).getComponent();
    }
    else if (documentationManager.hasActiveDockedDocWindow()) {
      ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION);
      Content selectedContent = toolWindow == null ? null : toolWindow.getContentManager().getSelectedContent();
      component = selectedContent == null ? null : (DocumentationComponent)selectedContent.getComponent();
    }
    else {
      component = EditorMouseHoverPopupManager.getInstance().getDocumentationComponent();
    }
    return component;
  }


  /**
   * Repeatedly tries to run given task in read action without blocking write actions (for this to work effectively the action should invoke
   * {@link ProgressManager#checkCanceled()} or {@link ProgressIndicator#checkCanceled()} often enough).
   *
   * @param action              task to run
   * @param timeout             timeout in milliseconds
   * @param pauseBetweenRetries pause between retries in milliseconds
   * @param progressIndicator   optional progress indicator, which can be used to cancel the action externally
   * @return {@code true} if the action succeeded to run without interruptions, {@code false} otherwise
   */
  public static boolean runInReadActionWithWriteActionPriorityWithRetries(@Nonnull final Runnable action, long timeout, long pauseBetweenRetries, @Nullable ProgressIndicator progressIndicator) {
    boolean result;
    long deadline = System.currentTimeMillis() + timeout;
    while (!(result = runInReadActionWithWriteActionPriority(action, progressIndicator == null ? null : new SensitiveProgressWrapper(progressIndicator))) &&
           (progressIndicator == null || !progressIndicator.isCanceled()) &&
           System.currentTimeMillis() < deadline) {
      try {
        TimeUnit.MILLISECONDS.sleep(pauseBetweenRetries);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return result;
  }

  /**
   * Same as {@link #runInReadActionWithWriteActionPriorityWithRetries(Runnable, long, long, ProgressIndicator)} using current thread's
   * progress indicator ({@link ProgressManager#getProgressIndicator()}).
   */
  public static boolean runInReadActionWithWriteActionPriorityWithRetries(@Nonnull final Runnable action, long timeout, long pauseBetweenRetries) {
    return runInReadActionWithWriteActionPriorityWithRetries(action, timeout, pauseBetweenRetries, ProgressIndicatorProvider.getGlobalProgressIndicator());
  }

  @Contract("_, _, _, null -> null")
  public static String inferLinkFromFullDocumentation(@Nonnull DocumentationProvider provider, PsiElement element, PsiElement originalElement, @Nullable String navigationInfo) {
    if (navigationInfo != null) {
      String fqn = element instanceof PsiQualifiedNamedElement ? ((PsiQualifiedNamedElement)element).getQualifiedName() : null;
      String fullText = provider.generateDoc(element, originalElement);
      return HintUtil.prepareHintText(DocPreviewUtil.buildPreview(navigationInfo, fqn, fullText), HintUtil.getInformationHint());
    }
    return null;
  }

  public static final Object CUT_AT_CMD = ObjectUtil.sentinel("CUT_AT_CMD");

  public static void updateQuickDocAsync(@Nonnull PsiElement element, @Nonnull CharSequence prefix, @Nonnull Consumer<Consumer<Object>> provider) {
    Project project = element.getProject();
    StringBuilder sb = new StringBuilder(prefix);
    ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();
    Disposable alarmDisposable = Disposable.newDisposable();
    Disposer.register(project, alarmDisposable);
    AtomicBoolean stop = new AtomicBoolean(false);
    Ref<Object> cutAt = Ref.create(null);
    SingleAlarm alarm = new SingleAlarm(() -> {
      DocumentationComponent component = getActiveDocComponent(project);
      if (component == null) {
        stop.set(true);
        Disposer.dispose(alarmDisposable);
        return;
      }
      Object s = queue.poll();
      while (s != null) {
        if (s == CUT_AT_CMD || cutAt.get() == CUT_AT_CMD) {
          cutAt.set(s);
          s = "";
        }
        else if (!cutAt.isNull()) {
          int idx = StringUtil.indexOf(sb, cutAt.get().toString());
          if (idx >= 0) sb.setLength(idx);
          cutAt.set(null);
        }
        sb.append(s);
        s = queue.poll();
      }
      if (stop.get()) {
        Disposer.dispose(alarmDisposable);
      }
      String newText = sb.toString() + "<br><br><br>";
      String prevText = component.getText();
      if (!Comparing.equal(newText, prevText)) {
        component.replaceText(newText, element);
      }
    }, 100, alarmDisposable);
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        provider.accept(str -> {
          ProgressManager.checkCanceled();
          if (stop.get()) throw new ProcessCanceledException();
          queue.add(str);
          alarm.cancelAndRequest();
        });
      }
      finally {
        if (stop.compareAndSet(false, true)) {
          alarm.cancelAndRequest();
        }
      }
    });
  }
}
