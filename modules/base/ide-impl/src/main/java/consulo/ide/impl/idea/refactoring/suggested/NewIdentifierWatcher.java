// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport;
import consulo.language.editor.refactoring.suggested.TextRangeUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

class NewIdentifierWatcher {
    private static class Ref {
        RangeMarker myMarker;

        Ref(RangeMarker marker) {
            myMarker = marker;
        }
    }

    private final int myMaxIdentifiers;
    private final Deque<Ref> myNewIdentifierMarkers = new ArrayDeque<>();

    NewIdentifierWatcher(int maxIdentifiers) {
        if (maxIdentifiers <= 0) {
            throw new IllegalArgumentException("maxIdentifiers must be positive");
        }
        myMaxIdentifiers = maxIdentifiers;
    }

    public @Nullable Document getLastDocument() {
        Ref first = myNewIdentifierMarkers.peekFirst();
        if (first == null || !first.myMarker.isValid()) {
            return null;
        }
        return first.myMarker.getDocument();
    }

    public List<TextRange> lastNewIdentifierRanges() {
        List<TextRange> result = new ArrayList<>(myNewIdentifierMarkers.size());
        for (Ref ref : myNewIdentifierMarkers) {
            result.add(Objects.requireNonNull(TextRangeUtils.asTextRange(ref.myMarker)));
        }
        return result;
    }

    public void documentChanged(DocumentEvent event, Language language) {
        Document document = event.getDocument();
        CharSequence chars = document.getCharsSequence();

        if (document != getLastDocument()) {
            reset();
        }

        SuggestedRefactoringSupport refactoringSupport = SuggestedRefactoringSupport.forLanguage(language);
        if (refactoringSupport == null) {
            return;
        }

        Iterator<Ref> iterator = myNewIdentifierMarkers.iterator();
        while (iterator.hasNext()) {
            Ref ref = iterator.next();
            TextRange range = TextRangeUtils.asTextRange(ref.myMarker);
            if (range == null) {
                iterator.remove();
                continue;
            }

            int start = range.getStartOffset();
            while (start > 0 && refactoringSupport.isIdentifierPart(chars.charAt(start - 1))) {
                start--;
            }

            int end = range.getEndOffset();
            while (end < document.getTextLength() && refactoringSupport.isIdentifierPart(chars.charAt(end))) {
                end++;
            }

            if (start != range.getStartOffset() || end != range.getEndOffset()) {
                ref.myMarker.dispose();
                ref.myMarker = document.createRangeMarker(start, end);
            }
        }

        if (isNewIdentifierInserted(event, refactoringSupport)) {
            if (myNewIdentifierMarkers.size() == myMaxIdentifiers) {
                RangeMarker marker = myNewIdentifierMarkers.removeFirst().myMarker;
                marker.dispose();
            }
            RangeMarker marker = document.createRangeMarker(newRange(event));
            myNewIdentifierMarkers.addLast(new Ref(marker));
        }
        else {
            TextRange newRange = newRange(event);
            for (Ref ref : myNewIdentifierMarkers) {
                if (Objects.requireNonNull(TextRangeUtils.asTextRange(ref.myMarker)).contains(newRange)) {
                    return;
                }
            }

            CharSequence newFragment = event.getNewFragment();
            for (int i = 0; i < newFragment.length(); i++) {
                if (refactoringSupport.isIdentifierStart(newFragment.charAt(i))) {
                    reset();
                    break;
                }
            }
        }
    }

    public void reset() {
        for (Ref ref : myNewIdentifierMarkers) {
            ref.myMarker.dispose();
        }
        myNewIdentifierMarkers.clear();
    }

    private static boolean isNewIdentifierInserted(DocumentEvent event, SuggestedRefactoringSupport refactoringSupport) {
        if (!isIdentifierLike(refactoringSupport, event.getNewFragment())) {
            return false;
        }
        CharSequence chars = event.getDocument().getCharsSequence();
        int start = event.getOffset();
        int end = event.getOffset() + event.getNewLength();
        if (start > 0 && refactoringSupport.isIdentifierPart(chars.charAt(start - 1))) {
            return false;
        }
        if (end < event.getDocument().getTextLength() && refactoringSupport.isIdentifierPart(chars.charAt(end))) {
            return false;
        }
        return true;
    }

    private static boolean isIdentifierLike(SuggestedRefactoringSupport refactoringSupport, CharSequence text) {
        if (text.length() == 0 || !refactoringSupport.isIdentifierStart(text.charAt(0))) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!refactoringSupport.isIdentifierPart(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static TextRange newRange(DocumentEvent event) {
        return new TextRange(event.getOffset(), event.getOffset() + event.getNewLength());
    }
}
