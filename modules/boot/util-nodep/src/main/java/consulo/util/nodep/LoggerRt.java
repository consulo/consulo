/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.util.nodep;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A wrapper which uses either IDE logging subsystem (if available) or java.util.logging.
 *
 * @since 12.0
 */
public abstract class LoggerRt {
  private interface Factory {
    LoggerRt getInstance(@Nonnull final String category);
  }

  private static Factory ourFactory;

  private synchronized static Factory getFactory() {
    if (ourFactory == null) {
      ourFactory = new SystemFactory();
    }
    return ourFactory;
  }

  @Nonnull
  public static LoggerRt getInstance(@Nonnull final String category) {
    return getFactory().getInstance(category);
  }

  @Nonnull
  public static LoggerRt getInstance(@Nonnull final Class<?> clazz) {
    return getInstance(clazz.getName());
  }

  public void info(@Nullable final String message) {
    info(message, null);
  }

  public void info(@Nonnull final Throwable t) {
    info(t.getMessage(), t);
  }

  public void warn(@Nullable final String message) {
    warn(message, null);
  }

  public void warn(@Nonnull final Throwable t) {
    warn(t.getMessage(), t);
  }

  public void error(@Nullable final String message) {
    error(message, null);
  }

  public void error(@Nonnull final Throwable t) {
    error(t.getMessage(), t);
  }

  public abstract void info(@Nullable final String message, @Nullable final Throwable t);
  public abstract void warn(@Nullable final String message, @Nullable final Throwable t);
  public abstract void error(@Nullable final String message, @Nullable final Throwable t);

  private static class SystemFactory implements Factory {
    @Override
    public LoggerRt getInstance(@Nonnull String category) {
      return new LoggerRt() {
        @Override
        public void info(@Nullable String message, @Nullable Throwable t) {
          System.out.println("[INFO] " + message);
          if(t != null) {
            t.printStackTrace(System.out);
          }
        }

        @Override
        public void warn(@Nullable String message, @Nullable Throwable t) {
          System.out.println("[WARN] " + message);
          if (t != null) {
            t.printStackTrace(System.out);
          }
        }

        @Override
        public void error(@Nullable String message, @Nullable Throwable t) {
          System.err.println("[ERROR] " + message);
          if (t != null) {
            t.printStackTrace(System.err);
          }
        }
      };
    }
  }
}