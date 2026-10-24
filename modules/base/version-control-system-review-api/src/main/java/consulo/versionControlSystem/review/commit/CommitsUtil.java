// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.review.commit;

import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

public final class CommitsUtil {
    private CommitsUtil() {
    }

    /**
     * Splits full commit message into subject and description:
     * First line becomes subject, everything after first line becomes description
     * Also supports empty line that separates subject and description
     *
     * @param commitMessage full commit message
     * @return pair of subject and description based on full commit message
     */
    public static Pair<String, String> splitCommitMessage(@Nullable String commitMessage) {
        //Trim original
        String message = commitMessage == null ? "" : commitMessage.trim();
        if (message.isEmpty()) {
            return Pair.create("", "");
        }
        int firstLineEnd = message.indexOf("\n");
        String subject;
        String description;
        if (firstLineEnd > -1) {
            //Subject is always first line
            subject = message.substring(0, firstLineEnd).trim();
            //Description is all text after first line, we also trim it to remove empty lines on start of description
            description = message.substring(firstLineEnd + 1).trim();
        }
        else {
            //If we don't have any line separators and cannot detect description,
            //we just assume that it is one-line commit and use full message as subject with empty description
            subject = message;
            description = "";
        }
        return Pair.create(subject, description);
    }
}
