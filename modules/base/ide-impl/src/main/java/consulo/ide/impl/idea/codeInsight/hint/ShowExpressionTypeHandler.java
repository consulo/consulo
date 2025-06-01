// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.hint;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.language.editor.refactoring.introduce.IntroduceTargetChooser;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.Language;
import consulo.language.editor.ExpressionTypeProvider;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.util.collection.JBIterable;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;

public class ShowExpressionTypeHandler implements CodeInsightActionHandler {
  private final boolean myRequestFocus;

  public ShowExpressionTypeHandler(boolean requestFocus) {
    myRequestFocus = requestFocus;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull PsiFile file) {
    UIAccess.assertIsUIThread();

    Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
    final Set<ExpressionTypeProvider> handlers = getHandlers(project, language, file.getViewProvider().getBaseLanguage());
    if (handlers.isEmpty()) return;

    Map<PsiElement, ExpressionTypeProvider> map = getExpressions(file, editor, handlers);
    Consumer<PsiElement> callback = new Consumer<PsiElement>() {
      @Override
      public void accept(@Nonnull PsiElement expression) {
        ExpressionTypeProvider provider = Objects.requireNonNull(map.get(expression));
        //noinspection unchecked
        final String informationHint = provider.getInformationHint(expression);
        TextRange range = expression.getTextRange();
        editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
        displayHint(new DisplayedTypeInfo(expression, provider, editor), informationHint);
      }
    };
    if (map.isEmpty()) {
      ApplicationManager.getApplication().invokeLater(() -> {
        String errorHint = Objects.requireNonNull(ContainerUtil.getFirstItem(handlers)).getErrorHint();
        HintManager.getInstance().showErrorHint(editor, errorHint);
      });
    }
    else if (map.size() == 1) {
      Map.Entry<PsiElement, ExpressionTypeProvider> entry = map.entrySet().iterator().next();
      PsiElement expression = entry.getKey();
      ExpressionTypeProvider provider = entry.getValue();
      DisplayedTypeInfo typeInfo = new DisplayedTypeInfo(expression, provider, editor);
      if (typeInfo.isRepeating() && provider.hasAdvancedInformation()) {
        //noinspection unchecked
        String informationHint = provider.getAdvancedInformationHint(expression);
        displayHint(typeInfo, informationHint);
      }
      else {
        callback.accept(expression);
      }
    }
    else {
      IntroduceTargetChooser.showChooser(editor, new ArrayList<>(map.keySet()), callback, PsiElement::getText);
    }
  }

  private void displayHint(@Nonnull DisplayedTypeInfo typeInfo, String informationHint) {
    ApplicationManager.getApplication().invokeLater(() -> {
      HintManager.getInstance().setRequestFocusForNextHint(myRequestFocus);
      typeInfo.showHint(informationHint);
    });
  }

  @Nonnull
  @RequiredReadAction
  public Map<PsiElement, ExpressionTypeProvider> getExpressions(@Nonnull PsiFile file, @Nonnull Editor editor) {
    Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
    Set<ExpressionTypeProvider> handlers = getHandlers(file.getProject(), language, file.getViewProvider().getBaseLanguage());
    return getExpressions(file, editor, handlers);
  }

  @Nonnull
  @RequiredReadAction
  private static Map<PsiElement, ExpressionTypeProvider> getExpressions(@Nonnull PsiFile file, @Nonnull Editor editor, @Nonnull Set<? extends ExpressionTypeProvider> handlers) {
    if (handlers.isEmpty()) return Collections.emptyMap();
    boolean exactRange = false;
    TextRange range = EditorUtil.getSelectionInAnyMode(editor);
    final Map<PsiElement, ExpressionTypeProvider> map = new LinkedHashMap<>();
    int offset = !range.isEmpty() ? range.getStartOffset() : TargetElementUtil.adjustOffset(file, editor.getDocument(), range.getStartOffset());
    for (int i = 0; i < 3 && map.isEmpty() && offset >= i; i++) {
      PsiElement elementAt = file.findElementAt(offset - i);
      if (elementAt == null) continue;
      for (ExpressionTypeProvider handler : handlers) {
        for (PsiElement element : ((ExpressionTypeProvider<? extends PsiElement>)handler).getExpressionsAt(elementAt)) {
          TextRange r = element.getTextRange();
          if (exactRange && !r.equals(range) || !r.contains(range)) continue;
          if (!exactRange) exactRange = r.equals(range);
          map.put(element, handler);
        }
      }
    }
    return map;
  }

  @Nonnull
  public static Set<ExpressionTypeProvider> getHandlers(final Project project, Language... languages) {
    DumbService dumbService = DumbService.getInstance(project);
    return JBIterable.of(languages).flatten(language -> dumbService.filterByDumbAwareness(ExpressionTypeProvider.forLanguage(language))).addAllTo(new LinkedHashSet<>());
  }

  static final class DisplayedTypeInfo {
    private static volatile DisplayedTypeInfo ourCurrentInstance;
    final
    @Nonnull
    PsiElement myElement;
    final
    @Nonnull
    ExpressionTypeProvider<?> myProvider;
    final
    @Nonnull
    Editor myEditor;

    DisplayedTypeInfo(@Nonnull PsiElement element, @Nonnull ExpressionTypeProvider<?> provider, @Nonnull Editor editor) {
      myElement = element;
      myProvider = provider;
      myEditor = editor;
    }

    @Override
    @SuppressWarnings("EqualsHashCode")
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DisplayedTypeInfo info = (DisplayedTypeInfo)o;
      return Objects.equals(myElement, info.myElement) && Objects.equals(myProvider, info.myProvider) && Objects.equals(myEditor, info.myEditor);
    }

    /**
     * @return true if the same hint (i.e. on the same PsiElement, with the same provider, in the same editor) is displayed currently.
     */
    boolean isRepeating() {
      return this.equals(ourCurrentInstance);
    }

    void showHint(String informationHint) {
      JComponent label = HintUtil.createInformationLabel(informationHint);
      setInstance(this);
      AccessibleContextUtil.setName(label, "Expression type hint");
      HintManagerImpl hintManager = (HintManagerImpl)HintManager.getInstance();
      LightweightHintImpl hint = new LightweightHintImpl(label);
      hint.addHintListener(e -> ApplicationManager.getApplication().invokeLater(() -> setInstance(null)));
      Point p = hintManager.getHintPosition(hint, myEditor, HintManager.ABOVE);
      int flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING;
      hintManager.showEditorHint(hint, myEditor, p, flags, 0, false);
    }

    private static void setInstance(DisplayedTypeInfo typeInfo) {
      ourCurrentInstance = typeInfo;
    }
  }
}

