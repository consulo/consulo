package consulo.language.spellcheker.text;

import consulo.language.spellcheker.internal.TextContentImpl;
import consulo.component.util.PluginExceptionUtil;
import consulo.document.util.TextRange;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.OuterLanguageElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.spellcheker.text.TextContent.ExclusionKind;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A class encapsulating common utilities needed for extracting {@link TextContent} from PSI.
 * The common usage pattern is {@code TextContentBuilder.FromPsi. ... .build(psi, domain)}.
 */
public class TextContentBuilder {
    private static final Logger LOG = Logger.getInstance(TextContentBuilder.class);

    /**
     * A basic builder that takes the full PSI text, considering any {@link OuterLanguageElement}s as unknown fragments.
     */
    public static final TextContentBuilder FromPsi =
        new TextContentBuilder(e -> e instanceof OuterLanguageElement ? ExclusionKind.unknown : null, Collections.emptySet(), Collections.emptySet());

    private final Function<PsiElement, @Nullable ExclusionKind> myClassifier;
    private final Set<Character> myIndentChars, mySuffixChars;

    private TextContentBuilder(Function<PsiElement, @Nullable ExclusionKind> classifier,
                               Set<Character> indentChars,
                               Set<Character> suffixChars) {
        myClassifier = classifier;
        myIndentChars = indentChars;
        mySuffixChars = suffixChars;
    }

    /**
     * Exclude and {@link TextContent#markUnknown} all PSI elements satisfying the given condition.
     */
    public TextContentBuilder withUnknown(Predicate<PsiElement> unknown) {
        return appendClassifier(unknown, ExclusionKind.unknown);
    }

    /**
     * Call {@link TextContent#excludeRange} for all PSI elements satisfying the given condition.
     */
    public TextContentBuilder excluding(Predicate<PsiElement> excluded) {
        return appendClassifier(excluded, ExclusionKind.exclude);
    }

    /**
     * Exclude with {@link ExclusionKind#markup} all PSI elements satisfying the given condition.
     */
    public TextContentBuilder withMarkup(Predicate<PsiElement> markup) {
        return appendClassifier(markup, ExclusionKind.markup);
    }

    private TextContentBuilder appendClassifier(Predicate<PsiElement> predicate, ExclusionKind kind) {
        return new TextContentBuilder(e -> {
            ExclusionKind prev = myClassifier.apply(e);
            return prev != null ? prev : predicate.test(e) ? kind : prev;
        }, myIndentChars, mySuffixChars);
    }

    /**
     * For each line of the text, remove the prefix consisting of the given characters
     * (packed together in a string for compactness)
     */
    public TextContentBuilder removingIndents(String indentChars) {
        Set<Character> set = new HashSet<>(myIndentChars);
        for (int i = 0; i < indentChars.length(); i++) {
            set.add(indentChars.charAt(i));
        }
        return new TextContentBuilder(myClassifier, set, mySuffixChars);
    }

    /**
     * For each line of the text, remove the suffix consisting of the given characters
     * (packed together in a string for compactness)
     */
    public TextContentBuilder removingLineSuffixes(String suffixChars) {
        Set<Character> set = new HashSet<>(mySuffixChars);
        for (int i = 0; i < suffixChars.length(); i++) {
            set.add(suffixChars.charAt(i));
        }
        return new TextContentBuilder(myClassifier, myIndentChars, set);
    }

    /**
     * Traverses the whole sub-tree of the {@code root}, applying {@link #withUnknown}/{@link #excluding} rules,
     * {@link #removingIndents}, strips leading and trailing whitespace.
     * If the root has an {@link ElementManipulator}, only its value range is traversed.
     *
     * @return the resulting text content or {@code null} if it's empty.
     */
    public @Nullable TextContent build(PsiElement root, TextContent.TextDomain domain) {
        ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(root);
        int length = root.getTextLength();
        if (manipulator != null) {
            TextRange valueRange = manipulator.getRangeInElement(root);
            if (isWrongRange(valueRange, length)) {
                PluginExceptionUtil.logPluginError(LOG,
                    "The manipulator returned an incorrect range " + valueRange +
                        " for PSI " + root.getClass() +
                        " of length " + length,
                    null, manipulator.getClass());
                return null;
            }
            return build(root, domain, valueRange);
        }
        return build(root, domain, new TextRange(0, length));
    }

    public @Nullable TextContent build(PsiElement root, TextContent.TextDomain domain, TextRange valueRange) {
        TextRange rootRange = root.getTextRange();
        if (isWrongRange(valueRange, rootRange.getLength())) {
            LOG.error("The range " + valueRange + " is out of the PSI element, length " + rootRange.getLength());
            return null;
        }

        int rootStart = rootRange.getStartOffset();
        TextRange fileValueRange = valueRange.shiftRight(rootStart);
        CharSequence fileText = root.getContainingFile().getViewProvider().getContents();
        if (isWrongRange(fileValueRange, fileText.length())) {
            LOG.error("The range " + fileValueRange + " is out of the file, length " + fileText.length());
            return null;
        }

        TextContent content = new PsiRecursiveElementWalkingVisitor() {
            final List<TextContentImpl.TokenInfo> tokens = new ArrayList<>();
            int currentStart = fileValueRange.getStartOffset();

            @Override
            public void visitElement(PsiElement element) {
                TextRange range = element.getTextRange().intersection(fileValueRange);
                if (range == null) {
                    return;
                }

                @Nullable ExclusionKind kind = myClassifier.apply(element);
                if (kind == ExclusionKind.exclude) {
                    exclusionStarted(range);
                }
                else if (kind != null) {
                    exclusionStarted(range);
                    TextContentImpl.TokenKind tokenKind =
                        kind == ExclusionKind.markup ? TextContentImpl.TokenKind.markup : TextContentImpl.TokenKind.unknown;
                    tokens.add(new TextContentImpl.PsiToken("", root, TextRange.from(range.getStartOffset(), 0).shiftLeft(rootStart), tokenKind));
                }
                else {
                    super.visitElement(element);
                }
            }

            private void exclusionStarted(TextRange range) {
                if (range.getStartOffset() != currentStart) {
                    TextRange tokenRange = new TextRange(currentStart, range.getStartOffset());
                    String tokenText = tokenRange.subSequence(fileText).toString();
                    tokens.add(new TextContentImpl.PsiToken(tokenText, root, tokenRange.shiftLeft(rootStart), TextContentImpl.TokenKind.text));
                }
                currentStart = range.getEndOffset();
            }

            @Nullable
            TextContent walkPsiTree() {
                root.accept(this);
                exclusionStarted(TextRange.from(fileValueRange.getEndOffset(), 0));
                return tokens.isEmpty() ? null : new TextContentImpl(domain, tokens);
            }
        }.walkPsiTree();
        return content == null ? null : content.removeIndents(myIndentChars).removeLineSuffixes(mySuffixChars).trimWhitespace();
    }

    private static boolean isWrongRange(TextRange valueRange, int maxLength) {
        return valueRange.getStartOffset() < 0 || valueRange.getEndOffset() > maxLength;
    }
}
