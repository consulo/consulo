// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.psi.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class PartiallyKnownStringUtil {
    private PartiallyKnownStringUtil() {
    }

    @RequiredReadAction
    public static Attachment[] mkAttachments(PsiElement host) {
        AttachmentFactory factory = AttachmentFactory.get();
        return new Attachment[]{
            factory.create("host.txt", getHostText(host)),
            factory.create(getFileName(host), getFileText(host))
        };
    }

    @RequiredReadAction
    private static String getHostText(PsiElement host) {
        try {
            String text = host.getText();
            return text != null ? text : "<null>";
        }
        catch (Throwable e) {
            return ExceptionUtil.getThrowableText(e);
        }
    }

    @RequiredReadAction
    private static String getFileName(PsiElement host) {
        String name = null;
        try {
            PsiFile file = host.getContainingFile();
            VirtualFile virtualFile = file != null ? file.getVirtualFile() : null;
            name = virtualFile != null ? virtualFile.getName() : null;
        }
        catch (Throwable ignored) {
        }
        return name != null ? name : "file.txt";
    }

    @RequiredReadAction
    private static String getFileText(PsiElement host) {
        try {
            PsiFile file = host.getContainingFile();
            String text = file != null ? file.getText() : null;
            return text != null ? text : "<null>";
        }
        catch (Throwable e) {
            return ExceptionUtil.getThrowableText(e);
        }
    }

    public static Stream<TextRange> splitToTextRanges(CharSequence charSequence, String pattern) {
        return splitToTextRanges(charSequence, pattern, (sequence, p) -> SplitEscaper.ACCEPT_ALL);
    }

    public static Stream<TextRange> splitToTextRanges(
        CharSequence charSequence,
        String pattern,
        BiFunction<CharSequence, String, SplitEscaper> escaperFactory
    ) {
        SplitEscaper escaper = escaperFactory.apply(charSequence, pattern);
        Iterator<TextRange> iterator = new Iterator<>() {
            private int myLastMatch = 0;
            private int myLastSplit = 0;
            private boolean myFinished = false;
            private @Nullable TextRange myNext;

            @Override
            public boolean hasNext() {
                if (myNext == null && !myFinished) {
                    myNext = computeNext();
                }
                return myNext != null;
            }

            @Override
            public TextRange next() {
                TextRange result = myNext;
                if (result == null) {
                    if (myFinished) {
                        throw new NoSuchElementException();
                    }
                    result = computeNext();
                }
                myNext = null;
                return result;
            }

            private TextRange computeNext() {
                while (true) {
                    int start = StringUtil.indexOf(charSequence, pattern, myLastMatch);
                    if (start == -1) {
                        myFinished = true;
                        return new TextRange(myLastSplit, charSequence.length());
                    }
                    myLastMatch = start + pattern.length();
                    if (escaper.filter(myLastSplit, start)) {
                        TextRange range = new TextRange(myLastSplit, start);
                        myLastSplit = myLastMatch;
                        return range;
                    }
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }
}
