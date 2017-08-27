/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.idea.starter;

import com.intellij.openapi.application.impl.ApplicationImpl;
import consulo.annotations.Internal;
import consulo.start.CommandLineArgs;
import org.jetbrains.annotations.NotNull;

@Internal
public abstract class ApplicationPostStarter {
  public static final String IDEA_APPLICATION = "idea";


  public void createApplication(boolean internal, boolean isUnitTestMode, boolean isHeadlessMode, boolean isCommandline, CommandLineArgs args) {
    new ApplicationImpl(internal, isUnitTestMode, isHeadlessMode, isCommandline, IDEA_APPLICATION, null);
  }

  public void premain(@NotNull CommandLineArgs args) {
  }

  public void main(boolean newConfigFolder, @NotNull CommandLineArgs args) {
  }
}
