// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.psi.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public abstract sealed class StringEntry permits StringEntry.Known, StringEntry.Unknown {
    private StringEntry() {
    }

    public abstract @Nullable PsiElement getSourcePsi();

    /**
     * A range in the {@link #getSourcePsi()} that corresponds to the content of this segment
     */
    public abstract TextRange getRange();

    public static final class Known extends StringEntry {
        private final String myValue;
        private final @Nullable PsiElement mySourcePsi;
        private final TextRange myRange;

        public Known(String value, @Nullable PsiElement sourcePsi, TextRange range) {
            myValue = value;
            mySourcePsi = sourcePsi;
            myRange = range;
        }

        public String getValue() {
            return myValue;
        }

        @Override
        public @Nullable PsiElement getSourcePsi() {
            return mySourcePsi;
        }

        @Override
        public TextRange getRange() {
            return myRange;
        }

        @Override
        public String toString() {
            return "StringEntry.Known('" + myValue + "' at " + myRange + " in " + mySourcePsi + ")";
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Known known
                && myValue.equals(known.myValue)
                && Objects.equals(mySourcePsi, known.mySourcePsi)
                && myRange.equals(known.myRange);
        }

        @Override
        public int hashCode() {
            int result = myValue.hashCode();
            result = 31 * result + (mySourcePsi != null ? mySourcePsi.hashCode() : 0);
            result = 31 * result + myRange.hashCode();
            return result;
        }
    }

    public static final class Unknown extends StringEntry {
        private final @Nullable PsiElement mySourcePsi;
        private final TextRange myRange;
        private final @Nullable Iterable<PartiallyKnownString> myPossibleValues;

        public Unknown(@Nullable PsiElement sourcePsi, TextRange range) {
            this(sourcePsi, range, null);
        }

        public Unknown(@Nullable PsiElement sourcePsi, TextRange range, @Nullable Iterable<PartiallyKnownString> possibleValues) {
            mySourcePsi = sourcePsi;
            myRange = range;
            myPossibleValues = possibleValues;
        }

        @Override
        public @Nullable PsiElement getSourcePsi() {
            return mySourcePsi;
        }

        @Override
        public TextRange getRange() {
            return myRange;
        }

        public @Nullable Iterable<PartiallyKnownString> getPossibleValues() {
            return myPossibleValues;
        }

        @Override
        public String toString() {
            return "StringEntry.Unknown(at " + myRange + " in " + mySourcePsi + ")";
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Unknown unknown
                && Objects.equals(mySourcePsi, unknown.mySourcePsi)
                && myRange.equals(unknown.myRange);
        }

        @Override
        public int hashCode() {
            int result = mySourcePsi != null ? mySourcePsi.hashCode() : 0;
            result = 31 * result + myRange.hashCode();
            return result;
        }
    }

    @RequiredReadAction
    public @Nullable PsiElement getHost() {
        PsiElement sourcePsi = getSourcePsi();
        if (isSuitableHostClass(sourcePsi)) {
            return sourcePsi;
        }
        PsiElement parent = sourcePsi != null ? sourcePsi.getParent() : null;
        return isSuitableHostClass(parent) ? parent : null;
    }

    @RequiredReadAction
    public @Nullable Pair<PsiElement, TextRange> getRangeAlignedToHost() {
        PsiElement sourcePsi = getSourcePsi();
        if (sourcePsi == null) {
            return null;
        }
        if (isSuitableHostClass(sourcePsi)) {
            return Pair.create(sourcePsi, getRange());
        }
        PsiElement parent = sourcePsi.getParent();
        if (parent instanceof PsiLanguageInjectionHost) {
            return Pair.create(parent, getRange().shiftRight(sourcePsi.getStartOffsetInParent()));
        }
        return null;
    }

    private static boolean isSuitableHostClass(@Nullable PsiElement element) {
        return element instanceof ContributedReferenceHost || element instanceof PsiLanguageInjectionHost;
    }
}
