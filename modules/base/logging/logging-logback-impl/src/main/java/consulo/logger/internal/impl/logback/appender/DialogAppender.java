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
package consulo.logger.internal.impl.logback.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationStarterCore;
import consulo.logger.internal.impl.logback.LoggingEventWithIdeaEvent;
import consulo.logging.attachment.ExceptionWithAttachments;
import consulo.logging.internal.FatalErrorReporter;
import consulo.logging.internal.IdeaLoggingEvent;
import consulo.logging.internal.LogEventException;
import consulo.logging.internal.LogMessageEx;
import consulo.util.lang.ExceptionUtil;
import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 2024-09-05
 */
public class DialogAppender<E> extends UnsynchronizedAppenderBase<E> {
    private static final int MAX_ASYNC_LOGGING_EVENTS = 5;

    private volatile Runnable myDialogRunnable = null;
    private final AtomicInteger myPendingAppendCounts = new AtomicInteger();

    @Override
    protected void append(E event) {
        if (!(event instanceof ILoggingEvent loggingEvent)) {
            return;
        }

        if (!ApplicationStarterCore.isLoaded()) {
            return;
        }

        if (myPendingAppendCounts.addAndGet(1) > MAX_ASYNC_LOGGING_EVENTS) {
            // Stop adding requests to the queue or we can get OOME on pending logging requests (IDEA-95327)
            myPendingAppendCounts.decrementAndGet(); // number of pending logging events should not increase
        }
        else {
            IdeaLoggingEvent ideaEvent = null;
            final String message = loggingEvent.getMessage();
            if (loggingEvent instanceof LoggingEventWithIdeaEvent likeEvent) {
                ideaEvent = likeEvent.getIdeaLoggingEvent();
            }

            if (ideaEvent == null) {
                Throwable thrown = extractThrowable(loggingEvent.getThrowableProxy());
                if (thrown == null) {
                    myPendingAppendCounts.decrementAndGet();
                    return;
                }
                ideaEvent = extractLoggingEvent(message, thrown);
            }

            // Note, we MUST avoid SYNCHRONOUS invokeAndWait to prevent deadlocks
            final IdeaLoggingEvent finalIdeaEvent = ideaEvent;
            Application.get().invokeLater(() -> {
                try {
                    appendToLoggers(finalIdeaEvent);
                }
                finally {
                    myPendingAppendCounts.decrementAndGet();
                }
            });
        }
    }

    void appendToLoggers(@Nonnull IdeaLoggingEvent e) {
        if (myDialogRunnable != null) {
            return;
        }

        final FatalErrorReporter logger = FatalErrorReporter.INSTANCE;
        if (!logger.canHandle(e)) {
            return;
        }
        myDialogRunnable = () -> {
            try {
                logger.handle(e);
            }
            finally {
                myDialogRunnable = null;
            }
        };

        final Application app = ApplicationManager.getApplication();
        if (app == null) {
            new Thread(myDialogRunnable).start();
        }
        else {
            app.executeOnPooledThread(myDialogRunnable);
        }
    }

    private Throwable extractThrowable(IThrowableProxy proxy) {
        if (proxy == null) {
            return null;
        }

        if (proxy instanceof ThrowableProxy throwableProxy) {
            return throwableProxy.getThrowable();
        }

        return null;
    }

    @Nonnull
    private static IdeaLoggingEvent extractLoggingEvent(@Nonnull String strMessage, @Nonnull Throwable throwable) {
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable rootCause = ExceptionUtil.getRootCause(throwable);
        if (rootCause instanceof LogEventException) {
            return ((LogEventException) rootCause).getLogMessage();
        }

        ExceptionWithAttachments withAttachments = ExceptionUtil.findCause(throwable, ExceptionWithAttachments.class);
        if (withAttachments != null) {
            return LogMessageEx.createEvent(strMessage, ExceptionUtil.getThrowableText(throwable), withAttachments.getAttachments());
        }

        return new IdeaLoggingEvent(strMessage, throwable);
    }
}
