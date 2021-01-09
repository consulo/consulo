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
package consulo.logging.impl.log4j2;

import com.intellij.idea.StartupUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import consulo.application.ApplicationProperties;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.logging.internal.LoggerFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2018-08-15
 */
public class Log4J2LoggerFactory implements LoggerFactory {
  private static final String SYSTEM_MACRO = "$SYSTEM_DIR$";
  private static final String APPLICATION_MACRO = "$APPLICATION_DIR$";
  private static final String LOG_DIR_MACRO = "$LOG_DIR$";

  private final LoggerContext myLoggerContext;

  public Log4J2LoggerFactory() {
    // map message factory to simple, since default message factory is reusable - and will remap our object to string message, without access to object variant
    System.setProperty("log4j2.messageFactory", "org.apache.logging.log4j.message.SimpleMessageFactory");
    System.setProperty(ShutdownCallbackRegistry.SHUTDOWN_HOOK_ENABLED, "false");
    myLoggerContext = init();
  }

  @Nonnull
  @Override
  public Logger getLoggerInstance(@Nonnull String name) {
    return new Log4J2Logger(myLoggerContext, name);
  }

  @Nonnull
  @Override
  public Logger getLoggerInstance(@Nonnull Class<?> clazz) {
    return new Log4J2Logger(myLoggerContext, clazz);
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
      String fileRef = Boolean.getBoolean(ApplicationProperties.CONSULO_MAVEN_CONSOLE_LOG) ? "/log4j2-console.xml" : "/log4j2-default.xml";

      String text = FileUtil.loadTextAndClose(Log4J2LoggerFactory.class.getResourceAsStream(fileRef));
      text = StringUtil.replace(text, SYSTEM_MACRO, StringUtil.replace(ContainerPathManager.get().getSystemPath(), "\\", "\\\\"));
      text = StringUtil.replace(text, APPLICATION_MACRO, StringUtil.replace(ContainerPathManager.get().getHomePath(), "\\", "\\\\"));
      text = StringUtil.replace(text, LOG_DIR_MACRO, StringUtil.replace(ContainerPathManager.get().getLogPath().getAbsolutePath(), "\\", "\\\\"));

      File file = ContainerPathManager.get().getLogPath();
      if (!file.mkdirs() && !file.exists()) {
        System.err.println("Cannot create log directory: " + file);
      }

      ConfigurationSource source = new ConfigurationSource(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));

      return Configurator.initialize(Log4J2LoggerFactory.class.getClassLoader(), source);
    }
    catch (Exception e) {
      e.printStackTrace();
      StartupUtil.showMessage("Consulo", e);
      return null;
    }
  }
}
