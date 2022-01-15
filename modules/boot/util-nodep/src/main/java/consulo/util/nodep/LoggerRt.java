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


// See ContainerLogger
@Deprecated
public abstract class LoggerRt {
  private interface Factory {
    LoggerRt getInstance(final String category);
  }

  private static Factory ourFactory;

  private synchronized static Factory getFactory() {
    if (ourFactory == null) {
      ourFactory = new SystemFactory();
    }
    return ourFactory;
  }


  public static LoggerRt getInstance(final String category) {
    return getFactory().getInstance(category);
  }

  public static LoggerRt getInstance(final Class<?> clazz) {
    return getInstance(clazz.getName());
  }

  public void info(final String message) {
    info(message, null);
  }

  public void info(final Throwable t) {
    info(t.getMessage(), t);
  }

  public void warn(final String message) {
    warn(message, null);
  }

  public void warn(final Throwable t) {
    warn(t.getMessage(), t);
  }

  public void error(final String message) {
    error(message, null);
  }

  public void error(final Throwable t) {
    error(t.getMessage(), t);
  }

  public abstract void info(final String message, final Throwable t);

  public abstract void warn(final String message, final Throwable t);

  public abstract void error(final String message, final Throwable t);

  private static class SystemFactory implements Factory {
    @Override
    public LoggerRt getInstance(String category) {
      return new LoggerRt() {
        @Override
        public void info(String message, Throwable t) {
          System.out.println("[INFO] " + message);
          if (t != null) {
            t.printStackTrace(System.out);
          }
        }

        @Override
        public void warn(String message, Throwable t) {
          System.out.println("[WARN] " + message);
          if (t != null) {
            t.printStackTrace(System.out);
          }
        }

        @Override
        public void error(String message, Throwable t) {
          System.err.println("[ERROR] " + message);
          if (t != null) {
            t.printStackTrace(System.err);
          }
        }
      };
    }
  }
}