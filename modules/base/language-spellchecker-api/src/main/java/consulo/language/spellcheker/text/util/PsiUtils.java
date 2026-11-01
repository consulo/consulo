// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.spellcheker.text.util;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.spellcheker.text.TextExtractionFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class PsiUtils {

    private PsiUtils() {
    }

    public static boolean hasType(ASTNode node, IElementType... tokens) {
        return hasType(node, Set.copyOf(Arrays.asList(tokens)));
    }

    public static boolean hasType(ASTNode node, Set<IElementType> tokens) {
        return tokens.contains(node.getElementType());
    }

    public static Stream<PsiElement> parents(PsiElement element) {
        return Stream.iterate(element, Objects::nonNull, PsiElement::getParent);
    }

    /**
     * Get all siblings of {@code element} that are accepted by {@code checkSibling}
     * which are separated by whitespace containing at most one line break
     */
    public static List<PsiElement> getNotSoDistantSimilarSiblings(PsiElement element, Predicate<PsiElement> checkSibling) {
        return getNotSoDistantSimilarSiblings(element, TokenSet.EMPTY, checkSibling);
    }

    /**
     * Get all siblings of {@code element} that are accepted by {@code checkSibling}
     * which are separated by whitespace ({@link PsiWhiteSpace} or anything in {@code whitespaceTokens}) containing at most one line break
     */
    public static List<PsiElement> getNotSoDistantSimilarSiblings(PsiElement element,
                                                                  TokenSet whitespaceTokens,
                                                                  Predicate<PsiElement> checkSibling) {
        return PsiUtilCore.getNotSoDistantSimilarSiblings(
            element,
            whitespaceTokens,
            checkSibling,
            element.getApplication().getInstance(TextExtractionFilter.class)::isSuppressionComment
        );
    }

}
