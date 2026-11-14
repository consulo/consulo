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
package consulo.ai.acp;

import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.ProcessOutputTypes;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * A command the agent runs through the client ({@code terminal/create}): its output, the end of it kept up to a limit,
 * and how it ended.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AcpTerminal {
    private final String mySessionId;
    private final ProcessHandler myHandler;
    private final int myLimit;
    private final StringBuilder myOutput = new StringBuilder();
    private boolean myTruncated;
    private final CompletableFuture<Integer> myExit = new CompletableFuture<>();

    private AcpTerminal(String sessionId, ProcessHandler handler, int limit) {
        mySessionId = sessionId;
        myHandler = handler;
        myLimit = limit;
    }

    /**
     * @param limit the most characters of output kept - the start goes first
     * @param changed told whenever the output grows and once the command ended
     */
    static AcpTerminal start(ProcessHandlerBuilderFactory factory,
                             String sessionId,
                             GeneralCommandLine commandLine,
                             int limit,
                             Runnable changed) throws ExecutionException {
        ProcessHandler handler = factory.newBuilder(commandLine.withCharset(StandardCharsets.UTF_8)).killable().build();
        AcpTerminal terminal = new AcpTerminal(sessionId, handler, limit);
        handler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                if (outputType == ProcessOutputTypes.STDOUT || outputType == ProcessOutputTypes.STDERR) {
                    terminal.append(event.getText());
                    changed.run();
                }
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                terminal.myExit.complete(event.getExitCode());
                changed.run();
            }
        });
        handler.startNotify();
        return terminal;
    }

    String getSessionId() {
        return mySessionId;
    }

    private synchronized void append(String text) {
        myOutput.append(text);
        int excess = myOutput.length() - myLimit;
        if (excess > 0) {
            // a surrogate pair is never cut in half
            if (Character.isLowSurrogate(myOutput.charAt(excess))) {
                excess++;
            }
            myOutput.delete(0, excess);
            myTruncated = true;
        }
    }

    synchronized String getOutput() {
        return myOutput.toString();
    }

    synchronized boolean isTruncated() {
        return myTruncated;
    }

    /**
     * @return the exit code, null while the command runs
     */
    @Nullable Integer getExitCode() {
        return myExit.isDone() ? myExit.join() : null;
    }

    CompletableFuture<Integer> whenExited() {
        return myExit;
    }

    void kill() {
        if (!myHandler.isProcessTerminated()) {
            myHandler.destroyProcess();
        }
    }
}
