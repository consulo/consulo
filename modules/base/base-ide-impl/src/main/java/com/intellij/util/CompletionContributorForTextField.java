package com.intellij.util;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import consulo.application.dumb.DumbAware;
import consulo.project.DumbService;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPlainTextFile;import consulo.annotation.access.RequiredReadAction;

/**
 * @author sergey.evdokimov
 */
public class CompletionContributorForTextField extends CompletionContributor implements DumbAware {

  @RequiredReadAction
  @Override
  public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
    PsiFile file = parameters.getOriginalFile();
    if (!(file instanceof PsiPlainTextFile)) return;

    TextFieldCompletionProvider field = file.getUserData(TextFieldCompletionProvider.COMPLETING_TEXT_FIELD_KEY);
    if (field == null) return;

    if (!(field instanceof DumbAware) && DumbService.isDumb(file.getProject())) return;
    
    String text = file.getText();
    int offset = Math.min(text.length(), parameters.getOffset());

    String prefix = field.getPrefix(text.substring(0, offset));

    CompletionResultSet activeResult;

    if (!result.getPrefixMatcher().getPrefix().equals(prefix)) {
      activeResult = result.withPrefixMatcher(prefix);
    }
    else {
      activeResult = result;
    }

    if (field.isCaseInsensitivity()) {
      activeResult = activeResult.caseInsensitive();
    }

    field.addCompletionVariants(text, offset, prefix, activeResult);
  }
}
