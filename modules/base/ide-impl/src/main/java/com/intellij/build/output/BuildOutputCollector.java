/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.build.output;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Deque;
import java.util.LinkedList;

// from kotlin
public class BuildOutputCollector implements BuildOutputInstantReader {
  private final BuildOutputInstantReader myReader;

  private final Deque<String> myReadLines = new LinkedList<>();

  public BuildOutputCollector(BuildOutputInstantReader reader) {
    myReader = reader;
  }

  @Nonnull
  @Override
  public Object getParentEventId() {
    return myReader.getParentEventId();
  }

  @Nullable
  @Override
  public String readLine() {
    String line = myReader.readLine();
    if (line != null) {
      myReadLines.add(line);
    }
    return line;
  }

  @Override
  public void pushBack() {
    myReader.pushBack();
    myReadLines.pollLast();
  }

  @Override
  public void pushBack(int numberOfLines) {
    myReader.pushBack(numberOfLines);

    for (int i = 0; i < numberOfLines; i++) {
      if (myReadLines.pollLast() == null) {
        break;
      }
    }
  }

  @Nonnull
  public String getOutput() {
    return String.join("\n", myReadLines);
  }
}
