// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.comment;

import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.change.patch.PatchLine;

import java.util.ArrayList;
import java.util.List;

public final class CodeReviewSuggestedChange {
    private final String myCommentBody;
    private final PatchHunk myPatchHunk;
    private final String myFilePath;
    private final int myStartLineIndex;
    private final int myEndLineIndex;
    private final CodeReviewSuggestionSyntax mySyntax;

    public CodeReviewSuggestedChange(String commentBody,
                                     PatchHunk patchHunk,
                                     String filePath,
                                     int startLineIndex,
                                     int endLineIndex,
                                     CodeReviewSuggestionSyntax syntax) {
        myCommentBody = commentBody;
        myPatchHunk = patchHunk;
        myFilePath = filePath;
        myStartLineIndex = startLineIndex;
        myEndLineIndex = endLineIndex;
        mySyntax = syntax;
    }

    public String getCommentBody() {
        return myCommentBody;
    }

    public String getFilePath() {
        return myFilePath;
    }

    public int getStartLineIndex() {
        return myStartLineIndex;
    }

    public int getEndLineIndex() {
        return myEndLineIndex;
    }

    public List<String> cutContextContent() {
        List<String> lines = notRemovedLines();
        int end = Math.max(0, lines.size() - (myEndLineIndex - myStartLineIndex + 1));
        return lines.subList(Math.max(0, end - 3), end);
    }

    public List<String> cutChangedContent() {
        List<String> lines = notRemovedLines();
        return lines.subList(Math.max(0, lines.size() - (myEndLineIndex - myStartLineIndex + 1)), lines.size());
    }

    public List<String> cutSuggestedChangeContent() {
        List<String> result = new ArrayList<>();
        boolean inside = false;
        for (String line : myCommentBody.split("\n", -1)) {
            if (!inside) {
                inside = mySyntax.isSuggestionStart(line);
                continue;
            }
            if (line.startsWith("```")) {
                break;
            }
            result.add(line);
        }
        return result;
    }

    private List<String> notRemovedLines() {
        List<String> result = new ArrayList<>();
        for (PatchLine line : myPatchHunk.getLines()) {
            if (line.getType() != PatchLine.Type.REMOVE) {
                result.add(line.getText());
            }
        }
        return result;
    }

    public static boolean containsSuggestedChange(String markdownText, CodeReviewSuggestionSyntax syntax) {
        return syntax.containsSuggestedChange(markdownText);
    }
}
