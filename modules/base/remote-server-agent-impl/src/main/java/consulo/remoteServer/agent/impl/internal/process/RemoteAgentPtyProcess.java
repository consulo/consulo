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

import consulo.process.ExecutionException;
import consulo.process.internal.PlatformPtyProcess;
import consulo.remoteServer.agent.impl.internal.RemoteAgentConnection;
import consulo.remoteServer.agent.impl.internal.protocol.ExecEvent;
import consulo.remoteServer.agent.impl.internal.protocol.ExecExit;
import consulo.remoteServer.agent.impl.internal.protocol.ExecInput;
import consulo.remoteServer.agent.impl.internal.protocol.ExecKill;
import consulo.remoteServer.agent.impl.internal.protocol.ExecStart;
import consulo.remoteServer.agent.impl.internal.protocol.PtySize;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A process of a pseudo terminal of the agent's machine - its output and input go through one Exec stream of the agent.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class RemoteAgentPtyProcess implements PlatformPtyProcess {
    private static final long START_TIMEOUT_SECONDS = 30;
    private static final int UNKNOWN_EXIT_CODE = -1;
    private static final int SIGNAL_EXIT_CODE_BASE = 128;
    // the end of the output
    private static final byte[] END = new byte[0];

    private final BlockingQueue<byte[]> myOutput = new LinkedBlockingQueue<>();
    private final CompletableFuture<Long> myStarted = new CompletableFuture<>();
    private final CompletableFuture<Integer> myExit = new CompletableFuture<>();
    private final OutputInputStream myInputStream = new OutputInputStream();
    private final RemoteAgentProcessInput myOutputStream = new RemoteAgentProcessInput(this::send);
    private final StreamObserver<ExecInput> myRequests;

    private volatile int myExitCode = UNKNOWN_EXIT_CODE;

    private RemoteAgentPtyProcess(RemoteAgentConnection connection) {
        myRequests = connection.exec(new Events());
    }

    static RemoteAgentPtyProcess start(RemoteAgentConnection connection, ExecStart start, String commandLine) throws ExecutionException {
        RemoteAgentPtyProcess process = new RemoteAgentPtyProcess(connection);
        process.send(ExecInput.newBuilder().setStart(start).build());
        try {
            process.myStarted.get(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (java.util.concurrent.ExecutionException e) {
            throw new ExecutionException(connection.toException(e.getCause()).getMessage(), e.getCause());
        }
        catch (TimeoutException e) {
            process.destroy();
            throw new ExecutionException("Remote process did not start in " + START_TIMEOUT_SECONDS + " seconds: " + commandLine);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroy();
            throw new ExecutionException("Interrupted while starting " + commandLine, e);
        }
        return process;
    }

    private synchronized void send(ExecInput input) {
        if (!myExit.isDone()) {
            myRequests.onNext(input);
        }
    }

    @Override
    public InputStream getInputStream() {
        return myInputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return myOutputStream;
    }

    @Override
    public void resize(int columns, int rows) {
        send(ExecInput.newBuilder().setResize(PtySize.newBuilder().setColumns(columns).setRows(rows)).build());
    }

    @Override
    public boolean isAlive() {
        return myStarted.isDone() && !myExit.isDone();
    }

    @Override
    public CompletableFuture<Integer> onExit() {
        return myExit;
    }

    @Override
    public void destroy() {
        send(ExecInput.newBuilder().setKill(ExecKill.newBuilder().setForce(true)).build());
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

    private void finish(Throwable error) {
        if (!myStarted.isDone()) {
            myStarted.completeExceptionally(error != null ? error : new IllegalStateException("Exec stream ended before start"));
        }
        myOutput.add(END);
        myExit.complete(myExitCode);
    }

    private final class Events implements StreamObserver<ExecEvent> {
        @Override
        public void onNext(ExecEvent event) {
            switch (event.getEventCase()) {
                case STARTED -> myStarted.complete(event.getStarted());
                // the terminal has one output - the agent sends it as stdout
                case STDOUT -> myOutput.add(event.getStdout().toByteArray());
                case STDERR -> myOutput.add(event.getStderr().toByteArray());
                case EXITED -> myExitCode = exitCodeOf(event.getExited());
                case EVENT_NOT_SET -> {
                }
            }
        }

        @Override
        public void onError(Throwable error) {
            finish(error);
        }

        @Override
        public void onCompleted() {
            finish(null);
        }
    }

    /**
     * The output chunks of the stream as bytes - it blocks until the next one comes, and ends with the process.
     */
    private final class OutputInputStream extends InputStream {
        private byte[] myChunk = new byte[0];
        private int myPosition;
        private boolean myEnded;

        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            int count = read(single, 0, 1);
            return count < 0 ? -1 : single[0] & 0xFF;
        }

        @Override
        public synchronized int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            while (myPosition >= myChunk.length) {
                if (myEnded) {
                    return -1;
                }
                byte[] next;
                try {
                    next = myOutput.take();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
                if (next == END) {
                    myEnded = true;
                    return -1;
                }
                myChunk = next;
                myPosition = 0;
            }
            int count = Math.min(length, myChunk.length - myPosition);
            System.arraycopy(myChunk, myPosition, buffer, offset, count);
            myPosition += count;
            return count;
        }

        @Override
        public synchronized int available() {
            int available = myChunk.length - myPosition;
            for (byte[] chunk : myOutput) {
                available += chunk.length;
            }
            return available;
        }
    }
}
