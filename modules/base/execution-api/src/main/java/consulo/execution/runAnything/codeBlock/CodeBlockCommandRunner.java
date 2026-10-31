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
package consulo.execution.runAnything.codeBlock;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.Language;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds and runs the commands a document writes in its code - a line of a block of code, or inline code - the way Run Anything
 * runs what is typed into it
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface CodeBlockCommandRunner {
    /**
     * @param line             one line of a block of code, or inline code, its prompt left as written
     * @param workingDirectory where the command would run
     * @param inlineCode       whether it is inline code, which may also name a run configuration
     * @return whether something would run the command: a command a shell runs as it is does not count, nor does anything else
     * which matches every text
     */
    @RequiredReadAction
    boolean canRunLine(String line, @Nullable VirtualFile workingDirectory, boolean inlineCode);

    /**
     * Runs what {@link #canRunLine} found, and does nothing when nothing matches the line any more
     */
    @RequiredUIAccess
    void runLine(String line, @Nullable VirtualFile workingDirectory, boolean inlineCode);

    /**
     * @see CodeBlockRunner#isApplicable
     */
    @Nullable CodeBlockRunner findBlockRunner(String languageName, @Nullable Language language);

    /**
     * Turns what a document shows into what a shell runs: drops the {@code $} prompt of every line, a comment after a line and the
     * lines left empty
     */
    static String trimPrompt(String command) {
        List<String> lines = new ArrayList<>();
        for (String line : command.split("\\r\\n|\\r|\\n")) {
            String withoutPrompt = line.startsWith("$") ? line.substring(1) : line;
            String withoutComment = stripTrailingShellComment(withoutPrompt);
            if (!withoutComment.isEmpty()) {
                lines.add(withoutComment);
            }
        }
        return String.join("\n", lines);
    }

    private static String stripTrailingShellComment(String line) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            }
            else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            }
            else if (c == '#' && !inSingle && !inDouble && (i == 0 || Character.isWhitespace(line.charAt(i - 1)))) {
                return line.substring(0, i).stripTrailing();
            }
        }
        return line;
    }
}
