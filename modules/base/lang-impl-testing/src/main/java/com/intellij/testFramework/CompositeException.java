/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.testFramework;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import javax.annotation.Nonnull;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeException extends Exception {
  private final List<Throwable> myExceptions = new ArrayList<>();
  public CompositeException(@Nonnull Throwable... e) {
    myExceptions.addAll(Arrays.asList(e));
  }

  public void add(@Nonnull Throwable e) {
    if (e instanceof CompositeException) {
      myExceptions.addAll(((CompositeException)e).myExceptions);
    }
    else {
      myExceptions.add(e);
    }
  }

  public boolean isEmpty() {
    return myExceptions.isEmpty();
  }

  @Override
  public String getMessage() {
    return processAll(Throwable::getMessage, CommonProcessors.<String>alwaysTrue());
  }

  @Override
  public String getLocalizedMessage() {
    return processAll(Throwable::getLocalizedMessage, CommonProcessors.<String>alwaysTrue());
  }

  @Override
  public String toString() {
    return processAll(Throwable::toString, CommonProcessors.<String>alwaysTrue());
  }

  @Override
  public void printStackTrace(final PrintStream s) {
    processAll(throwable -> {
      throwable.printStackTrace(s);
      return "";
    }, str -> {
      s.print(str);
      return false;
    });
  }

  @Override
  public void printStackTrace(final PrintWriter s) {
    processAll(throwable -> {
      throwable.printStackTrace(s);
      return "";
    }, str -> {
      s.print(str);
      return false;
    });
  }

  private String processAll(Function<Throwable, String> exceptionProcessor, Processor<String> stringProcessor) {
    if (myExceptions.size() == 1) {
      Throwable throwable = myExceptions.get(0);
      String s = exceptionProcessor.fun(throwable);
      stringProcessor.process(s);
      return s;
    }

    StringBuilder sb = new StringBuilder();
    String line = "CompositeException ("+myExceptions.size() +" nested):\n------------------------------\n";
    stringProcessor.process(line);
    sb.append(line);

    for (int i = 0; i < myExceptions.size(); i++) {
      Throwable exception = myExceptions.get(i);

      line = "[" + i + "]: ";
      stringProcessor.process(line);
      sb.append(line);

      line = StringUtil.notNullize(exceptionProcessor.fun(exception));
      if (!line.endsWith("\n")) line += '\n';
      stringProcessor.process(line);
      sb.append(line);
    }

    line = "------------------------------\n";
    stringProcessor.process(line);
    sb.append(line);

    return sb.toString();
  }
}
