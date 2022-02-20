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
package consulo.util.lang;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author mike
 */
public class CompoundRuntimeException extends RuntimeException {
  @Nonnull
  private final List<? extends Throwable> myExceptions;

  public CompoundRuntimeException(@Nonnull List<? extends Throwable> throwables) {
    myExceptions = throwables;
  }

  @Override
  public synchronized Throwable getCause() {
    return !myExceptions.isEmpty() ? myExceptions.get(0) : null;
  }

  public List<Throwable> getExceptions() {
    return new ArrayList<>(myExceptions);
  }

  @Override
  public String getMessage() {
    return processAll(Throwable::getMessage, s -> {
    });
  }

  @Override
  public String getLocalizedMessage() {
    return processAll(Throwable::getLocalizedMessage, s -> {
    });
  }

  @Override
  public String toString() {
    return processAll(Throwable::toString, s -> {
    });
  }

  @Override
  public void printStackTrace(final PrintStream s) {
    processAll(throwable -> {
      throwable.printStackTrace(s);
      return "";
    }, s::print);
  }

  @Override
  public void printStackTrace(final PrintWriter s) {
    processAll(throwable -> {
      throwable.printStackTrace(s);
      return "";
    }, s::print);
  }

  private String processAll(@Nonnull Function<? super Throwable, String> exceptionProcessor, @Nonnull Consumer<? super String> stringProcessor) {
    if (myExceptions.size() == 1) {
      Throwable throwable = myExceptions.get(0);
      String s = exceptionProcessor.apply(throwable);
      stringProcessor.accept(s);
      return s;
    }

    StringBuilder sb = new StringBuilder();
    String line = "CompositeException (" + myExceptions.size() + " nested):\n------------------------------\n";
    stringProcessor.accept(line);
    sb.append(line);

    for (int i = 0; i < myExceptions.size(); i++) {
      Throwable exception = myExceptions.get(i);

      line = "[" + i + "]: ";
      stringProcessor.accept(line);
      sb.append(line);

      line = exceptionProcessor.apply(exception);
      if (line == null) {
        line = "null\n";
      }
      else if (!line.endsWith("\n")) line += '\n';
      stringProcessor.accept(line);
      sb.append(line);
    }

    line = "------------------------------\n";
    stringProcessor.accept(line);
    sb.append(line);

    return sb.toString();
  }

  public static void throwIfNotEmpty(@Nullable List<? extends Throwable> throwables) {
    if (throwables == null || throwables.isEmpty()) {
      return;
    }

    if (throwables.size() == 1) {
      ExceptionUtil.rethrow(throwables.get(0));
    }
    else {
      throw new CompoundRuntimeException(throwables);
    }
  }
}
