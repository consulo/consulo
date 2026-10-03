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
package consulo.execution.profiler;

import consulo.application.progress.ProgressIndicator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Base for parsers of line-based text formats, fed from a stream or from chunks of process output.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class LineByLineParser {
    private final StringBuilder myPendingLine = new StringBuilder();
    private int myBadLines;

    /**
     * Handles one line, without its line separator. A line which cannot be parsed is counted with {@link #setBadLines(int)}.
     */
    public abstract void consumeLine(String line);

    /**
     * @return how many lines could not be parsed
     */
    public int getBadLines() {
        return myBadLines;
    }

    public void setBadLines(int badLines) {
        myBadLines = badLines;
    }

    /**
     * Consumes a chunk of text, such as process output. A line split across chunks is joined; the trailing part of a chunk
     * without a line separator waits for the next chunk or for {@link #flush()}.
     */
    public void consumeText(String chunk, ProgressIndicator indicator) {
        int lineStart = 0;
        for (int i = 0; i < chunk.length(); i++) {
            if (chunk.charAt(i) == '\n') {
                indicator.checkCanceled();
                myPendingLine.append(chunk, lineStart, i);
                consumePendingLine();
                lineStart = i + 1;
            }
        }
        myPendingLine.append(chunk, lineStart, chunk.length());
    }

    /**
     * Consumes the text {@link #consumeText} still holds back, when the input did not end with a line separator.
     */
    public void flush() {
        if (!myPendingLine.isEmpty()) {
            consumePendingLine();
        }
    }

    /**
     * Consumes every line of a stream read as UTF-8. The stream is not closed.
     */
    public void readFromStream(InputStream stream, ProgressIndicator indicator) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String line = reader.readLine();
        while (line != null) {
            indicator.checkCanceled();
            consumeLine(line);
            line = reader.readLine();
        }
    }

    private void consumePendingLine() {
        int length = myPendingLine.length();
        if (length > 0 && myPendingLine.charAt(length - 1) == '\r') {
            myPendingLine.setLength(length - 1);
        }
        String line = myPendingLine.toString();
        myPendingLine.setLength(0);
        consumeLine(line);
    }
}
