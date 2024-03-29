/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.language.editor.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public class SuppressionUtil {
  @NonNls
  public static final String SUPPRESS_INSPECTIONS_TAG_NAME = "noinspection";

  /**
   * Common part of regexp for suppressing in line comments for different languages.
   * Comment start prefix isn't included, e.g. add '//' for Java/C/JS or '#' for Ruby
   */
  @NonNls
  public static final String COMMON_SUPPRESS_REGEXP =
          "\\s*" + SUPPRESS_INSPECTIONS_TAG_NAME + "\\s+(" + LocalInspectionTool.VALID_ID_PATTERN + "(\\s*,\\s*" + LocalInspectionTool.VALID_ID_PATTERN + ")*)\\s*\\w*";

  @NonNls
  public static final Pattern SUPPRESS_IN_LINE_COMMENT_PATTERN = Pattern.compile("//" + COMMON_SUPPRESS_REGEXP);  // for Java, C, JS line comments

  @NonNls
  public static final String ALL = "ALL";

  private SuppressionUtil() {
  }

  public static boolean isInspectionToolIdMentioned(@Nonnull String inspectionsList, String inspectionToolID) {
    Iterable<String> ids = StringUtil.tokenize(inspectionsList, "[, ]");

    for (@NonNls String id : ids) {
      @NonNls String trim = id.trim();
      if (trim.equals(inspectionToolID) || trim.equalsIgnoreCase(ALL)) return true;
    }
    return false;
  }

  @Nullable
  public static PsiElement getStatementToolSuppressedIn(final PsiElement place, final String toolId, final Class<? extends PsiElement> statementClass) {
    return getStatementToolSuppressedIn(place, toolId, statementClass, SUPPRESS_IN_LINE_COMMENT_PATTERN);
  }

  @Nullable
  public static PsiElement getStatementToolSuppressedIn(final PsiElement place, final String toolId, final Class<? extends PsiElement> statementClass, final Pattern suppressInLineCommentPattern) {
    PsiElement statement = PsiTreeUtil.getNonStrictParentOfType(place, statementClass);
    if (statement != null) {
      PsiElement prev = PsiTreeUtil.skipSiblingsBackward(statement, PsiWhiteSpace.class);
      if (prev instanceof PsiComment) {
        String text = prev.getText();
        Matcher matcher = suppressInLineCommentPattern.matcher(text);
        if (matcher.matches() && isInspectionToolIdMentioned(matcher.group(1), toolId)) {
          return prev;
        }
      }
    }
    return null;
  }

  public static boolean isSuppressedInStatement(final PsiElement place, final String toolId, final Class<? extends PsiElement> statementClass) {
    return ApplicationManager.getApplication().runReadAction(new Supplier<Object>() {
      @Override
      public PsiElement get() {
        return getStatementToolSuppressedIn(place, toolId, statementClass);
      }
    }) != null;
  }

  @Nonnull
  public static PsiComment createComment(@Nonnull Project project, @Nonnull String commentText, @Nonnull Language language) {
    final PsiParserFacade parserFacade = PsiParserFacade.SERVICE.getInstance(project);
    return parserFacade.createLineOrBlockCommentFromText(language, commentText);
  }

  @Nullable
  public static Couple<String> getBlockPrefixSuffixPair(PsiElement comment) {
    final Commenter commenter = Commenter.forLanguage(comment.getLanguage());
    if (commenter != null) {
      final String prefix = commenter.getBlockCommentPrefix();
      final String suffix = commenter.getBlockCommentSuffix();
      if (prefix != null || suffix != null) {
        return Couple.of(StringUtil.notNullize(prefix), StringUtil.notNullize(suffix));
      }
    }
    return null;
  }

  @Nullable
  public static String getLineCommentPrefix(@Nonnull final PsiElement comment) {
    final Commenter commenter = Commenter.forLanguage(comment.getLanguage());
    return commenter == null ? null : commenter.getLineCommentPrefix();
  }

  public static boolean isSuppressionComment(@Nonnull PsiElement comment) {
    final String prefix = getLineCommentPrefix(comment);
    final String commentText = comment.getText();
    if (prefix != null) {
      return commentText.startsWith(prefix + SUPPRESS_INSPECTIONS_TAG_NAME);
    }
    final Couple<String> prefixSuffixPair = getBlockPrefixSuffixPair(comment);
    return prefixSuffixPair != null && commentText.startsWith(prefixSuffixPair.first + SUPPRESS_INSPECTIONS_TAG_NAME) && commentText.endsWith(prefixSuffixPair.second);
  }

  public static void replaceSuppressionComment(@Nonnull PsiElement comment, @Nonnull String id, boolean replaceOtherSuppressionIds, @Nonnull Language commentLanguage) {
    final String oldSuppressionCommentText = comment.getText();
    final String lineCommentPrefix = getLineCommentPrefix(comment);
    Couple<String> blockPrefixSuffix = null;
    if (lineCommentPrefix == null) {
      blockPrefixSuffix = getBlockPrefixSuffixPair(comment);
    }
    assert blockPrefixSuffix != null && oldSuppressionCommentText.startsWith(blockPrefixSuffix.first) && oldSuppressionCommentText.endsWith(blockPrefixSuffix.second) ||
           lineCommentPrefix != null && oldSuppressionCommentText.startsWith(lineCommentPrefix) : "Unexpected suppression comment " + oldSuppressionCommentText;

    // append new suppression tool id or replace
    final String newText;
    if (replaceOtherSuppressionIds) {
      newText = SUPPRESS_INSPECTIONS_TAG_NAME + " " + id;
    }
    else if (lineCommentPrefix == null) {
      newText = oldSuppressionCommentText.substring(blockPrefixSuffix.first.length(), oldSuppressionCommentText.length() - blockPrefixSuffix.second.length()) + "," + id;
    }
    else {
      newText = oldSuppressionCommentText.substring(lineCommentPrefix.length()) + "," + id;
    }
    comment.replace(createComment(comment.getProject(), newText, commentLanguage));
  }

  public static void createSuppression(@Nonnull Project project, @Nonnull PsiElement container, @Nonnull String id, @Nonnull Language commentLanguage) {
    final String text = SUPPRESS_INSPECTIONS_TAG_NAME + " " + id;
    PsiComment comment = createComment(project, text, commentLanguage);
    container.getParent().addBefore(comment, container);
  }

  public static boolean isSuppressed(@Nonnull PsiElement psiElement, String id) {
    if (id == null) return false;
    for (InspectionExtensionsFactory factory : InspectionExtensionsFactory.EP_NAME.getExtensionList()) {
      if (!factory.isToCheckMember(psiElement, id)) {
        return true;
      }
    }
    return false;
  }

  public static boolean inspectionResultSuppressed(@Nonnull PsiElement place, @Nonnull LocalInspectionTool tool) {
    return tool.isSuppressedFor(place);
  }

  @RequiredReadAction
  @Nonnull
  public static Set<InspectionSuppressor> getSuppressors(@Nonnull PsiElement element) {
    PsiUtilCore.ensureValid(element);
    PsiFile file = element.getContainingFile();
    if (file == null) {
      return Collections.emptySet();
    }
    FileViewProvider viewProvider = file.getViewProvider();
    final List<InspectionSuppressor> elementLanguageSuppressor = InspectionSuppressor.forLanguage(element.getLanguage());
    if (viewProvider instanceof TemplateLanguageFileViewProvider) {
      Set<InspectionSuppressor> suppressors = new LinkedHashSet<>();
      ContainerUtil.addAllNotNull(suppressors, InspectionSuppressor.forLanguage(viewProvider.getBaseLanguage()));
      for (Language language : viewProvider.getLanguages()) {
        ContainerUtil.addAllNotNull(suppressors, InspectionSuppressor.forLanguage(language));
      }
      ContainerUtil.addAllNotNull(suppressors, elementLanguageSuppressor);
      return suppressors;
    }
    if (!element.getLanguage().isKindOf(viewProvider.getBaseLanguage())) {
      Set<InspectionSuppressor> suppressors = new LinkedHashSet<>();
      ContainerUtil.addAllNotNull(suppressors, InspectionSuppressor.forLanguage(viewProvider.getBaseLanguage()));
      ContainerUtil.addAllNotNull(suppressors, elementLanguageSuppressor);
      return suppressors;
    }
    int size = elementLanguageSuppressor.size();
    switch (size) {
      case 0:
        return Collections.emptySet();
      case 1:
        return Collections.singleton(elementLanguageSuppressor.get(0));
      default:
        return new HashSet<>(elementLanguageSuppressor);
    }
  }
}
