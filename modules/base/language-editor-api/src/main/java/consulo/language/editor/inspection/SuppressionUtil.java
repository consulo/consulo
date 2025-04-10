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
import consulo.annotation.access.RequiredWriteAction;
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

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public class SuppressionUtil {
    public static final String SUPPRESS_INSPECTIONS_TAG_NAME = "noinspection";

    /**
     * Common part of regexp for suppressing in line comments for different languages.
     * Comment start prefix isn't included, e.g. add '//' for Java/C/JS or '#' for Ruby
     */
    public static final String COMMON_SUPPRESS_REGEXP =
        "\\s*" + SUPPRESS_INSPECTIONS_TAG_NAME + "\\s+(" + LocalInspectionTool.VALID_ID_PATTERN +
            "(\\s*,\\s*" + LocalInspectionTool.VALID_ID_PATTERN + ")*)\\s*\\w*";

    public static final Pattern SUPPRESS_IN_LINE_COMMENT_PATTERN = Pattern.compile("//" + COMMON_SUPPRESS_REGEXP);
    // for Java, C, JS line comments

    public static final String ALL = "ALL";

    private SuppressionUtil() {
    }

    public static boolean isInspectionToolIdMentioned(@Nonnull String inspectionsList, String inspectionToolID) {
        Iterable<String> ids = StringUtil.tokenize(inspectionsList, "[, ]");

        for (String id : ids) {
            String trim = id.trim();
            if (trim.equals(inspectionToolID) || trim.equalsIgnoreCase(ALL)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement getStatementToolSuppressedIn(
        PsiElement place,
        String toolId,
        Class<? extends PsiElement> statementClass
    ) {
        return getStatementToolSuppressedIn(place, toolId, statementClass, SUPPRESS_IN_LINE_COMMENT_PATTERN);
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement getStatementToolSuppressedIn(
        PsiElement place,
        String toolId,
        Class<? extends PsiElement> statementClass,
        Pattern suppressInLineCommentPattern
    ) {
        PsiElement statement = PsiTreeUtil.getNonStrictParentOfType(place, statementClass);
        if (statement != null && PsiTreeUtil.skipSiblingsBackward(statement, PsiWhiteSpace.class) instanceof PsiComment comment) {
            String text = comment.getText();
            Matcher matcher = suppressInLineCommentPattern.matcher(text);
            if (matcher.matches() && isInspectionToolIdMentioned(matcher.group(1), toolId)) {
                return comment;
            }
        }
        return null;
    }

    public static boolean isSuppressedInStatement(
        PsiElement place,
        String toolId,
        Class<? extends PsiElement> statementClass
    ) {
        return place.getApplication()
            .runReadAction((Supplier<Object>)() -> getStatementToolSuppressedIn(place, toolId, statementClass)) != null;
    }

    @Nonnull
    public static PsiComment createComment(@Nonnull Project project, @Nonnull String commentText, @Nonnull Language language) {
        PsiParserFacade parserFacade = PsiParserFacade.SERVICE.getInstance(project);
        return parserFacade.createLineOrBlockCommentFromText(language, commentText);
    }

    @Nullable
    @RequiredReadAction
    public static Couple<String> getBlockPrefixSuffixPair(PsiElement comment) {
        Commenter commenter = Commenter.forLanguage(comment.getLanguage());
        if (commenter != null) {
            String prefix = commenter.getBlockCommentPrefix();
            String suffix = commenter.getBlockCommentSuffix();
            if (prefix != null || suffix != null) {
                return Couple.of(StringUtil.notNullize(prefix), StringUtil.notNullize(suffix));
            }
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    public static String getLineCommentPrefix(@Nonnull PsiElement comment) {
        Commenter commenter = Commenter.forLanguage(comment.getLanguage());
        return commenter == null ? null : commenter.getLineCommentPrefix();
    }

    @RequiredReadAction
    public static boolean isSuppressionComment(@Nonnull PsiElement comment) {
        String prefix = getLineCommentPrefix(comment);
        String commentText = comment.getText();
        if (prefix != null) {
            return commentText.startsWith(prefix + SUPPRESS_INSPECTIONS_TAG_NAME);
        }
        Couple<String> prefixSuffixPair = getBlockPrefixSuffixPair(comment);
        return prefixSuffixPair != null && commentText.startsWith(prefixSuffixPair.first + SUPPRESS_INSPECTIONS_TAG_NAME)
            && commentText.endsWith(prefixSuffixPair.second);
    }

    @RequiredWriteAction
    public static void replaceSuppressionComment(
        @Nonnull PsiElement comment,
        @Nonnull String id,
        boolean replaceOtherSuppressionIds,
        @Nonnull Language commentLanguage
    ) {
        String oldSuppressionCommentText = comment.getText();
        String lineCommentPrefix = getLineCommentPrefix(comment);
        Couple<String> blockPrefixSuffix = null;
        if (lineCommentPrefix == null) {
            blockPrefixSuffix = getBlockPrefixSuffixPair(comment);
        }
        assert blockPrefixSuffix != null && oldSuppressionCommentText.startsWith(blockPrefixSuffix.first)
            && oldSuppressionCommentText.endsWith(blockPrefixSuffix.second)
            || lineCommentPrefix != null && oldSuppressionCommentText.startsWith(lineCommentPrefix)
            : "Unexpected suppression comment " + oldSuppressionCommentText;

        // append new suppression tool id or replace
        String newText;
        if (replaceOtherSuppressionIds) {
            newText = SUPPRESS_INSPECTIONS_TAG_NAME + " " + id;
        }
        else if (lineCommentPrefix == null) {
            newText = oldSuppressionCommentText.substring(
                blockPrefixSuffix.first.length(),
                oldSuppressionCommentText.length() - blockPrefixSuffix.second.length()
            ) + "," + id;
        }
        else {
            newText = oldSuppressionCommentText.substring(lineCommentPrefix.length()) + "," + id;
        }
        comment.replace(createComment(comment.getProject(), newText, commentLanguage));
    }

    public static void createSuppression(
        @Nonnull Project project,
        @Nonnull PsiElement container,
        @Nonnull String id,
        @Nonnull Language commentLanguage
    ) {
        String text = SUPPRESS_INSPECTIONS_TAG_NAME + " " + id;
        PsiComment comment = createComment(project, text, commentLanguage);
        container.getParent().addBefore(comment, container);
    }

    public static boolean isSuppressed(@Nonnull PsiElement psiElement, String id) {
        if (id == null) {
            return false;
        }
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
        List<InspectionSuppressor> elementLanguageSuppressor = InspectionSuppressor.forLanguage(element.getLanguage());
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
