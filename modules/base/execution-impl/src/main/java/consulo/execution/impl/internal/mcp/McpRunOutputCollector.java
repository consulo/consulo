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
package consulo.execution.impl.internal.mcp;

import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
final class McpRunOutputCollector {
    private static final Logger LOG = Logger.getInstance(McpRunOutputCollector.class);
    private static final int MAX_PREVIEW_LENGTH = 16 * 1024;

    private final Path myOutputPath;
    private final StringBuilder myPreview = new StringBuilder();

    private @Nullable Writer myWriter;
    private boolean myTruncated;

    McpRunOutputCollector(String configurationName) throws IOException {
        myOutputPath = Files.createTempFile("consulo_run_" + configurationName.replaceAll("[^A-Za-z0-9._-]", "_") + "_", ".log");
        myWriter = Files.newBufferedWriter(myOutputPath, StandardCharsets.UTF_8);
    }

    synchronized void append(String text) {
        Writer writer = myWriter;
        if (writer != null) {
            try {
                writer.write(text);
            }
            catch (IOException e) {
                LOG.warn("Cannot write run output to " + myOutputPath, e);
            }
        }

        int room = MAX_PREVIEW_LENGTH - myPreview.length();
        if (room >= text.length()) {
            myPreview.append(text);
        }
        else {
            if (room > 0) {
                myPreview.append(text, 0, room);
            }
            myTruncated = true;
        }
    }

    synchronized void close() {
        Writer writer = myWriter;
        myWriter = null;
        if (writer != null) {
            try {
                writer.close();
            }
            catch (IOException e) {
                LOG.warn("Cannot close run output " + myOutputPath, e);
            }
        }
    }

    synchronized String getPreview() {
        return myPreview.toString();
    }

    synchronized boolean isTruncated() {
        return myTruncated;
    }

    Path getOutputPath() {
        return myOutputPath;
    }

    void deleteOutputFile() {
        try {
            Files.deleteIfExists(myOutputPath);
        }
        catch (IOException e) {
            LOG.warn("Cannot delete run output " + myOutputPath, e);
        }
    }
}
