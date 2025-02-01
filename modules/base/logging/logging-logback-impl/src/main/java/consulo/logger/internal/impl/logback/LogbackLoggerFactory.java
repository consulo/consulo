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
package consulo.logger.internal.impl.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.CoreConstants;
import consulo.application.Application;
import consulo.container.boot.ContainerPathManager;
import consulo.container.internal.ShowErrorCaller;
import consulo.logging.Logger;
import consulo.logging.internal.LoggerFactory;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2024-09-05
 */
public class LogbackLoggerFactory implements LoggerFactory {
    public static final String CONSULO_MAVEN_CONSOLE_LOG = "consulo.maven.console.log";

    private static final String SYSTEM_MACRO = "$SYSTEM_DIR$";
    private static final String APPLICATION_MACRO = "$APPLICATION_DIR$";
    private static final String LOG_DIR_MACRO = "$LOG_DIR$";

    private final LoggerContext myLoggerContext;

    public LogbackLoggerFactory() {
        myLoggerContext = init();
    }

    @Nonnull
    @Override
    public Logger getLoggerInstance(@Nonnull String name) {
        return new LogbackLogger(myLoggerContext, name);
    }

    @Nonnull
    @Override
    public Logger getLoggerInstance(@Nonnull Class<?> clazz) {
        return new LogbackLogger(myLoggerContext, clazz);
    }

    @Override
    public void shutdown() {
        if (myLoggerContext != null) {
            myLoggerContext.stop();
        }
    }

    @Nullable
    private static LoggerContext init() {
        try {
            //String fileRef = Boolean.getBoolean(CONSULO_MAVEN_CONSOLE_LOG) ? "/logback-info.xml" : "/logback-warn.xml";
            String fileRef = "/logback-info.xml";

            String text = FileUtil.loadTextAndClose(LogbackLoggerFactory.class.getResourceAsStream(fileRef));
            text = StringUtil.replace(text, SYSTEM_MACRO, StringUtil.replace(ContainerPathManager.get().getSystemPath(), "\\", "\\\\"));
            text = StringUtil.replace(text, APPLICATION_MACRO, StringUtil.replace(ContainerPathManager.get().getHomePath(), "\\", "\\\\"));
            text = StringUtil.replace(text, LOG_DIR_MACRO, StringUtil.replace(ContainerPathManager.get().getLogPath().getAbsolutePath(), "\\", "\\\\"));

            File file = ContainerPathManager.get().getLogPath();
            if (!file.mkdirs() && !file.exists()) {
                System.err.println("Cannot create log directory: " + file);
            }

            LoggerContext context = new LoggerContext();
//            context.getStatusManager().add(new StatusListener() {
//                @Override
//                public void addStatusEvent(Status status) {
//                    System.out.println(status.getMessage());
//
//                    Throwable throwable = status.getThrowable();
//                    if (throwable != null) {
//                        throwable.printStackTrace();
//                    }
//                }
//            });
            context.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
            context.setMDCAdapter(new LogbackMDCAdapter());

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));

            return context;
        }
        catch (Exception e) {
            ShowErrorCaller.showErrorDialog(Application.get().getName().get(), e.getMessage(), e);
            return null;
        }
    }
}
