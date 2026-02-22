// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.build.ui.impl.internal.output;

import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.localize.BuildLocalize;
import consulo.build.ui.output.BuildOutputInstantReader;
import consulo.build.ui.output.BuildOutputParser;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.logging.Logger;
import consulo.process.io.ProcessIOExecutorService;
import consulo.util.concurrent.ConcurrencyUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BuildOutputInstantReaderImpl implements BuildOutputInstantReader, Closeable, Appendable {
    private static final Logger LOG = Logger.getInstance(BuildOutputInstantReader.class);

    private final Object buildId;
    private final Object parentEventId;
    private final int pushBackBufferSize;
    private final LinkedBlockingQueue<String> channel;
    private final LinkedList<String> readLinesBuffer = new LinkedList<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.Idle);
    private final LineProcessor appendedLineProcessor = new AppendedLineProcessor();

    private int readLinesBufferPosition = -1;
    private volatile boolean useActiveReading = true;
    private CompletableFuture<Void> readFinishedFuture = new CompletableFuture<>();

    private final Runnable readerRunnable;

    public BuildOutputInstantReaderImpl(@Nonnull Object buildId,
                                        @Nonnull Object parentEventId,
                                        @Nonnull BuildProgressListener buildProgressListener,
                                        @Nonnull List<BuildOutputParser> parsers) {
        this(buildId, parentEventId, buildProgressListener, parsers, 50, 64);
    }

    public BuildOutputInstantReaderImpl(@Nonnull Object buildId,
                                        @Nonnull Object parentEventId,
                                        @Nonnull BuildProgressListener buildProgressListener,
                                        @Nonnull List<BuildOutputParser> parsers,
                                        int pushBackBufferSize,
                                        int channelBufferCapacity) {
        this.buildId = buildId;
        this.parentEventId = parentEventId;
        this.pushBackBufferSize = pushBackBufferSize;
        this.channel = new LinkedBlockingQueue<>(channelBufferCapacity);

        this.readerRunnable = ConcurrencyUtil.underThreadNameRunnable(
            "Reader thread for BuildOutputInstantReaderImpl@" + System.identityHashCode(parentEventId),
            () -> {
                if (readFinishedFuture.isDone()) {
                    throw new IllegalStateException(BuildLocalize.errorCanTReadFromClosedStream().get());
                }

                BuildEvent[] lastMessage = {null};
                Consumer<BuildEvent> messageConsumer = event -> {
                    if (!Objects.equals(event, lastMessage[0])) {
                        buildProgressListener.onEvent(buildId, event);
                    }
                    lastMessage[0] = event;
                };

                try {
                    while (true) {
                        String line = doReadLine(useActiveReading);
                        if (line == null) break;
                        if (line.isBlank()) continue;
                        for (BuildOutputParser parser : parsers) {
                            BuildOutputInstantReaderWrapper readerWrapper = new BuildOutputInstantReaderWrapper(this);
                            try {
                                if (parser.parse(line, readerWrapper, messageConsumer)) {
                                    break;
                                }
                            }
                            catch (Exception e) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.warn("Build output parser error", e);
                                }
                                else {
                                    LOG.warn("Build output parser error: " + e.getMessage());
                                }
                            }
                            readerWrapper.pushBackReadLines();
                        }
                    }
                }
                catch (Throwable ex) {
                    if (LOG.isDebugEnabled()) {
                        LOG.warn("Build output reading error", ex);
                    }
                    else {
                        LOG.warn("Build output reading error: " + ex.getMessage());
                    }
                }
                finally {
                    if (!state.compareAndSet(State.Running, State.Idle)) {
                        readFinishedFuture.complete(null);
                    }
                }
            }
        );
    }

    @Override
    public @Nonnull Object getParentEventId() {
        return parentEventId;
    }

    @Override
    public BuildOutputInstantReaderImpl append(@Nonnull CharSequence csq) {
        appendedLineProcessor.append(csq);
        return this;
    }

    @Override
    public BuildOutputInstantReaderImpl append(@Nonnull CharSequence csq, int start, int end) {
        appendedLineProcessor.append(csq, start, end);
        return this;
    }

    @Override
    public BuildOutputInstantReaderImpl append(char c) {
        appendedLineProcessor.append(c);
        return this;
    }

    @Override
    public void close() {
        closeAndGetFuture();
    }

    public CompletableFuture<Void> closeAndGetFuture() {
        if (state.get() == State.Closed) return readFinishedFuture;
        if (state.compareAndSet(State.Idle, State.Closed)) {
            readFinishedFuture.complete(null);
        }
        else {
            state.set(State.Closed);
        }
        return readFinishedFuture;
    }

    @Override
    public @Nullable String readLine() {
        return doReadLine(true);
    }

    private @Nullable String doReadLine(boolean waitIfNotClosed) {
        if (readLinesBufferPosition >= 0) {
            String line = readLinesBuffer.get(readLinesBufferPosition);
            readLinesBufferPosition--;
            return line;
        }
        String line;
        while (true) {
            try {
                line = channel.poll(100, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (line != null || state.get() == State.Closed) break;
            if (!waitIfNotClosed) return null;
        }
        if (line == null) return null;
        readLinesBuffer.addFirst(line);
        if (readLinesBuffer.size() > pushBackBufferSize) {
            readLinesBuffer.removeLast();
        }
        return line;
    }

    @Override
    public void pushBack() {
        pushBack(1);
    }

    @Override
    public void pushBack(int numberOfLines) {
        readLinesBufferPosition += numberOfLines;
        if (readLinesBufferPosition >= pushBackBufferSize) {
            readLinesBufferPosition = pushBackBufferSize - 1;
        }
    }

    public void disableActiveReading() {
        useActiveReading = false;
    }

    private final class AppendedLineProcessor extends LineProcessor {
        @Override
        protected void process(@Nonnull String line) {
            if (state.get() == State.Closed) {
                throw new IllegalStateException(BuildLocalize.errorCanTAppendToClosedStream(line).get());
            }
            try {
                while (state.get() != State.Closed) {
                    if (state.compareAndSet(State.Idle, State.Running)) {
                        ProcessIOExecutorService.INSTANCE.submit(readerRunnable);
                    }
                    if (channel.offer(line, 100, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                }
            }
            catch (InterruptedException e) {
                throw new RuntimeException(new IOException(e));
            }
        }
    }

    private static final class BuildOutputInstantReaderWrapper implements BuildOutputInstantReader {
        private final BuildOutputInstantReader reader;
        private int linesRead;

        private BuildOutputInstantReaderWrapper(BuildOutputInstantReader reader) {
            this.reader = reader;
        }

        @Override
        public @Nonnull Object getParentEventId() {
            return reader.getParentEventId();
        }

        @Override
        public @Nullable String readLine() {
            String line = reader.readLine();
            if (line != null) linesRead++;
            return line;
        }

        @Override
        public void pushBack() {
            pushBack(1);
        }

        @Override
        public void pushBack(int numberOfLines) {
            int numberToPushBack = Math.min(numberOfLines, linesRead);
            linesRead -= numberToPushBack;
            reader.pushBack(numberToPushBack);
        }

        private void pushBackReadLines() {
            if (linesRead != 0) {
                reader.pushBack(linesRead);
                linesRead = 0;
            }
        }
    }

    private enum State {
        Idle,
        Running,
        Closed
    }
}
