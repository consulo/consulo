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

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.process.ProcessOutputTypes;
import consulo.process.util.AnsiEscapeDecoder;
import consulo.remoteServer.agent.impl.internal.RemoteAgentConnection;
import consulo.logging.Logger;
import consulo.remoteServer.agent.impl.internal.protocol.ExecEvent;
import consulo.remoteServer.agent.impl.internal.protocol.ExecExit;
import consulo.remoteServer.agent.impl.internal.protocol.ExecInput;
import consulo.remoteServer.agent.impl.internal.protocol.ExecKill;
import consulo.remoteServer.agent.impl.internal.protocol.ExecStart;
import consulo.process.BaseProcessHandler;
import consulo.process.ExecutionException;
import consulo.process.KillableProcessHandler;
import consulo.process.ProcessOutputType;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteAgentProcessHandler extends BaseProcessHandler implements KillableProcessHandler {
    private static final Logger LOG = Logger.getInstance(RemoteAgentProcessHandler.class);

    private static final long START_TIMEOUT_SECONDS = 30;
    private static final int UNKNOWN_EXIT_CODE = -1;
    private static final int SIGNAL_EXIT_CODE_BASE = 128;
    // an incomplete line (a prompt) is reported when nothing follows it for this long
    private static final long INCOMPLETE_LINE_DELAY_MS = 100;

    private final RemoteAgentConnection myConnection;
    private final String myCommandLine;
    private final Charset myCharset;
    private final RemoteOutputDecoder myStdoutDecoder;
    private final RemoteOutputDecoder myStderrDecoder;
    private final RemoteOutputLines myStdoutLines = new RemoteOutputLines();
    private final RemoteOutputLines myStderrLines = new RemoteOutputLines();
    private final CompletableFuture<Long> myStarted = new CompletableFuture<>();
    private final RemoteAgentProcessInput myProcessInput = new RemoteAgentProcessInput(this::send);

    private volatile @Nullable ClientCallStreamObserver<ExecInput> myRequests;
    private volatile long myPid = -1;
    private volatile int myExitCode = UNKNOWN_EXIT_CODE;
    private volatile boolean myDetached;
    // decodes ANSI escape sequences into colored text, as a local colored process handler does - null when not colored
    private final @Nullable AnsiEscapeDecoder myAnsiEscapeDecoder;

    public RemoteAgentProcessHandler(RemoteAgentConnection connection, String commandLine, Charset charset) {
        this(connection, commandLine, charset, false);
    }

    public RemoteAgentProcessHandler(RemoteAgentConnection connection, String commandLine, Charset charset, boolean colored) {
        myAnsiEscapeDecoder = colored ? new AnsiEscapeDecoder() : null;
        myConnection = connection;
        myCommandLine = commandLine;
        myCharset = charset;
        myStdoutDecoder = new RemoteOutputDecoder(charset);
        myStderrDecoder = new RemoteOutputDecoder(charset);
    }

    void start(ExecStart start) throws ExecutionException {
        myConnection.exec(new ExecObserver());
        send(ExecInput.newBuilder().setStart(start).build());
        try {
            myPid = myStarted.get(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (java.util.concurrent.ExecutionException e) {
            throw new ExecutionException(myConnection.toException(e.getCause()).getMessage(), e.getCause());
        }
        catch (TimeoutException e) {
            cancel("Process did not start in time");
            throw new ExecutionException("Remote process did not start in " + START_TIMEOUT_SECONDS + " seconds: " + myCommandLine);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancel("Interrupted");
            throw new ExecutionException("Interrupted while starting " + myCommandLine, e);
        }
    }

    @Override
    public void startNotify() {
        // the first line of the console - as a local process shows it
        notifyTextAvailable(myCommandLine + '\n', ProcessOutputTypes.SYSTEM);

        super.startNotify();
        ClientCallStreamObserver<ExecInput> requests = myRequests;
        if (requests != null) {
            requests.request(1);
        }
    }

    private synchronized void send(ExecInput input) {
        ClientCallStreamObserver<ExecInput> requests = myRequests;
        if (requests != null) {
            requests.onNext(input);
        }
    }

    private void cancel(String reason) {
        ClientCallStreamObserver<ExecInput> requests = myRequests;
        if (requests != null) {
            requests.cancel(reason, null);
        }
    }

    private void onEvent(ExecEvent event) {
        switch (event.getEventCase()) {
            case STARTED -> myStarted.complete(event.getStarted());
            case STDOUT -> onStdout(event.getStdout().toByteArray());
            case STDERR -> notifyLines(myStderrLines, myStderrDecoder.decode(event.getStderr().toByteArray()), ProcessOutputType.STDERR);
            case EXITED -> myExitCode = exitCodeOf(event.getExited());
            case EVENT_NOT_SET -> {
            }
        }
    }

    protected void onStdout(byte[] data) {
        notifyLines(myStdoutLines, myStdoutDecoder.decode(data), ProcessOutputType.STDOUT);
    }

    private void onFinished(@Nullable Throwable error) {
        if (!myStarted.isDone()) {
            myStarted.completeExceptionally(error != null ? error : new IllegalStateException("Exec stream ended before start"));
            return;
        }

        notifyLines(myStdoutLines, myStdoutDecoder.flush(), ProcessOutputType.STDOUT);
        notifyLines(myStderrLines, myStderrDecoder.flush(), ProcessOutputType.STDERR);
        synchronized (myStdoutLines) {
            notifyText(myStdoutLines.flush(), ProcessOutputType.STDOUT);
        }
        synchronized (myStderrLines) {
            notifyText(myStderrLines.flush(), ProcessOutputType.STDERR);
        }
        if (error != null && !myDetached) {
            LOG.warn("Lost remote process pid=" + myPid, error);
            notifyText(myConnection.toException(error).getMessage() + "\n", ProcessOutputType.SYSTEM);
        }
        if (!myDetached) {
            notifyProcessTerminated(myExitCode);
        }
    }

    /**
     * Reports the output line by line, as a local process handler does.
     */
    private void notifyLines(RemoteOutputLines lines, String text, ProcessOutputType outputType) {
        // text is taken and reported under one lock, so a delayed incomplete line never overtakes the next ones
        synchronized (lines) {
            for (String line : lines.append(text)) {
                notifyText(line, outputType);
            }
            if (lines.hasIncomplete()) {
                AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                    synchronized (lines) {
                        notifyText(lines.flush(), outputType);
                    }
                }, INCOMPLETE_LINE_DELAY_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void notifyText(String text, ProcessOutputType outputType) {
        if (text.isEmpty()) {
            return;
        }
        AnsiEscapeDecoder decoder = myAnsiEscapeDecoder;
        if (decoder != null && outputType != ProcessOutputType.SYSTEM) {
            decoder.escapeText(text, outputType, this::notifyTextAvailable);
        }
        else {
            notifyTextAvailable(text, outputType);
        }
    }

    private static int exitCodeOf(ExecExit exit) {
        if (exit.hasExitCode()) {
            return exit.getExitCode();
        }
        if (exit.hasSignal()) {
            return SIGNAL_EXIT_CODE_BASE + exit.getSignal();
        }
        return UNKNOWN_EXIT_CODE;
    }

    @Override
    protected void destroyProcessImpl() {
        send(ExecInput.newBuilder().setKill(ExecKill.newBuilder().setForce(false)).build());
    }

    @Override
    protected void detachProcessImpl() {
        myDetached = true;
        ClientCallStreamObserver<ExecInput> requests = myRequests;
        if (requests != null) {
            synchronized (this) {
                requests.onCompleted();
            }
        }
        notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Override
    public boolean canKillProcess() {
        return true;
    }

    @Override
    public void killProcess() {
        send(ExecInput.newBuilder().setKill(ExecKill.newBuilder().setForce(true)).build());
    }

    @Override
    public OutputStream getProcessInput() {
        return myProcessInput;
    }

    @Override
    public long getId() {
        return myPid;
    }

    @Override
    public Charset getCharset() {
        return myCharset;
    }

    @Override
    public String getCommandLine() {
        return myCommandLine;
    }

    private class ExecObserver implements ClientResponseObserver<ExecInput, ExecEvent> {
        @Override
        public void beforeStart(ClientCallStreamObserver<ExecInput> requests) {
            requests.disableAutoRequestWithInitial(1);
            myRequests = requests;
        }

        @Override
        public void onNext(ExecEvent event) {
            onEvent(event);
            ClientCallStreamObserver<ExecInput> requests = myRequests;
            if (requests != null && isStartNotified()) {
                requests.request(1);
            }
        }

        @Override
        public void onError(Throwable error) {
            onFinished(error);
        }

        @Override
        public void onCompleted() {
            onFinished(null);
        }
    }
}
