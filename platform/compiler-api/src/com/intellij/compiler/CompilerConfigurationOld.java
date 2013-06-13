/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.compiler;

import com.intellij.openapi.compiler.options.ExcludedEntriesConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

@Deprecated
public abstract class CompilerConfigurationOld {
  // need this flag for profiling purposes. In production code is always set to 'true'
  public static final boolean MAKE_ENABLED = true;


  @Nullable
  public abstract String getBytecodeTargetLevel(Module module);

  public static CompilerConfigurationOld getInstance(Project project) {
    return new CompilerConfigurationOld(){
      @Nullable
      @Override
      public String getBytecodeTargetLevel(Module module) {
        return null;
      }



      @Override
      public void addResourceFilePattern(String namePattern) throws MalformedPatternException {

      }

      @Override
      public String[] getResourceFilePatterns() {
        return new String[0];
      }

      @Override
      public void removeResourceFilePatterns() {

      }

      @Override
      public void convertPatterns() {

      }

      @Override
      public boolean isAddNotNullAssertions() {
        return false;
      }

      @Override
      public void setAddNotNullAssertions(boolean enabled) {

      }

      @Override
      public ExcludedEntriesConfiguration getExcludedEntriesConfiguration() {
        return null;
      }
    };
  }



  public abstract void addResourceFilePattern(String namePattern) throws MalformedPatternException;

  public abstract String[] getResourceFilePatterns();

  public abstract void removeResourceFilePatterns();

  public abstract void convertPatterns();

  public abstract boolean isAddNotNullAssertions();

  public abstract void setAddNotNullAssertions(boolean enabled);

  public abstract ExcludedEntriesConfiguration getExcludedEntriesConfiguration();
}