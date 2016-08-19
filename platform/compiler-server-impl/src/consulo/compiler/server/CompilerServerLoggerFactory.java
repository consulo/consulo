/*
 * Copyright 2013-2014 must-be.org
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
package consulo.compiler.server;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 21:45/03.10.13
 */
public class CompilerServerLoggerFactory implements Logger.Factory {
  @Override
  public Logger getLoggerInstance(String category) {
    return new Logger() {
      @Override
      public boolean isDebugEnabled() {
        return false;
      }

      @Override
      public void debug(@NonNls String message) {

      }

      @Override
      public void debug(@Nullable Throwable t) {

      }

      @Override
      public void debug(@NonNls String message, @Nullable Throwable t) {

      }

      @Override
      public void error(@NonNls String message, @Nullable Throwable t, @NonNls String... details) {
        System.out.println(message);
        if (t != null) {
          t.printStackTrace();
        }
      }

      @Override
      public void info(@NonNls String message) {
        System.out.println(message);
      }

      @Override
      public void info(@NonNls String message, @Nullable Throwable t) {
        System.out.println(message);
        if (t != null) {
          t.printStackTrace();
        }
      }

      @Override
      public void warn(@NonNls String message, @Nullable Throwable t) {
        System.out.println(message);
        if (t != null) {
          t.printStackTrace();
        }
      }

      @Override
      public void setLevel(Level level) {
      }
    };
  }
}
