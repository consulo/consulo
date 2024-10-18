/*
 * Copyright 2013-2024 consulo.io
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
package consulo.find;

import consulo.find.localize.FindLocalize;
import consulo.localize.LocalizeValue;

/**
 * @author UNV
 * @since 2024-10-18
 */
public enum FindSearchContext {
    ANY(true, true, true, FindLocalize.findContextAnywhereScopeLabel()),
    IN_STRING_LITERALS(true, false, false, FindLocalize.findContextInLiteralsScopeLabel()),
    IN_COMMENTS(false, true, false, FindLocalize.findContextInCommentsScopeLabel()),
    EXCEPT_STRING_LITERALS(false, true, true, FindLocalize.findContextExceptLiteralsScopeLabel()),
    EXCEPT_COMMENTS(true, false, true, FindLocalize.findContextExceptCommentsScopeLabel()),
    EXCEPT_COMMENTS_AND_STRING_LITERALS(false, false, true, FindLocalize.findContextExceptCommentsAndLiteralsScopeLabel());

    private final boolean myInStringLiterals, myInComments, myOutsideCommentsAndLiterals;
    private final LocalizeValue myName;

    FindSearchContext(boolean inStringLiterals, boolean inComments, boolean outsideCommentsAndLiterals, LocalizeValue name) {
        myInStringLiterals = inStringLiterals;
        myInComments = inComments;
        myOutsideCommentsAndLiterals = outsideCommentsAndLiterals;
        myName = name;
    }

    public LocalizeValue getName() {
        return myName;
    }

    public boolean isInStringLiterals() {
        return myInStringLiterals;
    }

    public boolean isInComments() {
        return myInComments;
    }

    public boolean isOutsideCommentsAndLiterals() {
        return myOutsideCommentsAndLiterals;
    }
}
