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
package consulo.remoteServer.agent.impl.internal.process;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the text of one output stream of a remote process into lines - the agent sends chunks as they come, which may
 * end anywhere, while listeners (and the parsers of tool output, e.g. a JSON message per line) expect the lines a local
 * process handler reports.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
class RemoteOutputLines {
    private final StringBuilder myIncomplete = new StringBuilder();

    /**
     * @return the lines completed by {@code text}, each with its line separator
     */
    synchronized List<String> append(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                myIncomplete.append(text, start, i + 1);
                lines.add(myIncomplete.toString());
                myIncomplete.setLength(0);
                start = i + 1;
            }
        }
        myIncomplete.append(text, start, text.length());
        return lines;
    }

    synchronized boolean hasIncomplete() {
        return myIncomplete.length() > 0;
    }

    /**
     * The incomplete rest - e.g. a prompt that waits for input, or the end of the output.
     */
    synchronized String flush() {
        String rest = myIncomplete.toString();
        myIncomplete.setLength(0);
        return rest;
    }
}
