/*
 * Copyright 2013-2018 consulo.io
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
package consulo.logging.impl.log4j2.appender;

import com.intellij.diagnostic.DefaultIdeaErrorLogger;
import com.intellij.diagnostic.LogEventException;
import com.intellij.diagnostic.LogMessageEx;
import com.intellij.idea.ApplicationStarter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.util.ExceptionUtil;
import consulo.logging.attachment.ExceptionWithAttachments;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 2018-08-15
 */
@Plugin(name = "Dialog", category = Node.CATEGORY, elementType = "appender", printObject = true)
public class Log4J2DialogAppender extends AbstractAppender {

  @PluginFactory
  public static Log4J2DialogAppender createAppender(@PluginAttribute("name") String name,
                                                    @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                    @PluginElement("Filter") final Filter filter) {
    if (name == null) {
      LOGGER.error("No name provided for Log4J2DialogAppender");
      return null;
    }
    if (layout == null) {
      layout = PatternLayout.createDefaultLayout();
    }
    return new Log4J2DialogAppender(name, filter, layout);
  }

  private static final int MAX_ASYNC_LOGGING_EVENTS = 5;

  private volatile Runnable myDialogRunnable = null;
  private final AtomicInteger myPendingAppendCounts = new AtomicInteger();

  public Log4J2DialogAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
    super(name, filter, layout);
  }

  @Override
  public void append(LogEvent event) {
    if (!ApplicationStarter.isLoaded()) {
      return;
    }

    if (myPendingAppendCounts.addAndGet(1) > MAX_ASYNC_LOGGING_EVENTS) {
      // Stop adding requests to the queue or we can get OOME on pending logging requests (IDEA-95327)
      myPendingAppendCounts.decrementAndGet(); // number of pending logging events should not increase
    }
    else {
      // we need calculate throwable before appender exit, since log4j2 remove references to throwable
      IdeaLoggingEvent ideaEvent = null;
      final Message message = event.getMessage();
      if (message instanceof ObjectMessage) {
        Object parameter = ((ObjectMessage)message).getParameter();
        if(parameter instanceof IdeaLoggingEvent) {
          ideaEvent = (IdeaLoggingEvent)parameter;
        }
      }

      if(ideaEvent == null) {
        Throwable thrown = event.getThrown();
        if (thrown == null) {
          myPendingAppendCounts.decrementAndGet();
          return;
        }
        ideaEvent = extractLoggingEvent(message, thrown);
      }

      // Note, we MUST avoid SYNCHRONOUS invokeAndWait to prevent deadlocks
      final IdeaLoggingEvent finalIdeaEvent = ideaEvent;
      SwingUtilities.invokeLater(() -> {
        try {
          appendToLoggers(finalIdeaEvent);
        }
        finally {
          myPendingAppendCounts.decrementAndGet();
        }
      });
    }
  }

  void appendToLoggers(@Nonnull IdeaLoggingEvent ideaEvent) {
    if (myDialogRunnable != null) {
      return;
    }

    final DefaultIdeaErrorLogger logger = DefaultIdeaErrorLogger.INSTANCE;
    if (!logger.canHandle(ideaEvent)) {
      return;
    }
    myDialogRunnable = () -> {
      try {
        logger.handle(ideaEvent);
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

  @Nonnull
  private static IdeaLoggingEvent extractLoggingEvent(@Nonnull Message message, @Nonnull Throwable throwable) {
    //noinspection ThrowableResultOfMethodCallIgnored
    Throwable rootCause = ExceptionUtil.getRootCause(throwable);
    if (rootCause instanceof LogEventException) {
      return ((LogEventException)rootCause).getLogMessage();
    }

    String strMessage = message.getFormattedMessage();
    ExceptionWithAttachments withAttachments = ExceptionUtil.findCause(throwable, ExceptionWithAttachments.class);
    if (withAttachments != null) {
      return LogMessageEx.createEvent(strMessage, ExceptionUtil.getThrowableText(throwable), withAttachments.getAttachments());
    }

    return new IdeaLoggingEvent(strMessage, throwable);
  }
}
