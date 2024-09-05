/*
 * Copyright 2013-2019 consulo.io
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
package consulo.logging.internal;

import jakarta.annotation.Nonnull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2019-08-10
 */
public class LoggerFactoryInitializer {
  private static LoggerFactory ourFactory = new DefaultLoggerFactoryImpl();

  public static void setFactory(LoggerFactory factory) {
    if (isInitialized()) {
      if (factory == ourFactory) {
        return;
      }

      //noinspection UseOfSystemOutOrSystemErr
      System.out.println("Changing log factory\n" + getThrowableText(new Throwable()));
    }

    ourFactory = factory;
  }

  @Nonnull
  private static String getThrowableText(@Nonnull Throwable aThrowable) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    aThrowable.printStackTrace(writer);
    return stringWriter.getBuffer().toString();
  }

  public static void initializeLogger(@Nonnull ClassLoader classLoader) {
    ServiceLoader<LoggerFactoryProvider> factories = ServiceLoader.load(LoggerFactoryProvider.class, classLoader);
    ServiceLoader.Provider<LoggerFactoryProvider> factory = factories
        .stream()
        .sorted((o1, o2) -> o2.get().getPriority() - o1.get().getPriority())
        .findFirst()
        .get();
    LoggerFactoryInitializer.setFactory(factory.get().create());
  }

  public static boolean isInitialized() {
    return !(ourFactory instanceof DefaultLoggerFactoryImpl);
  }

  public static LoggerFactory getFactory() {
    return ourFactory;
  }
}
