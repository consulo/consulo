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
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;

import java.util.Map;
import java.util.Set;

public abstract class CompilerConfiguration {
  // need this flag for profiling purposes. In production code is always set to 'true'
  public static final boolean MAKE_ENABLED = true;


  @Nullable
  public abstract String getBytecodeTargetLevel(Module module);

  @NotNull
  public abstract AnnotationProcessingConfiguration getAnnotationProcessingConfiguration(Module module);

  /**
   * @return true if exists at least one enabled annotation processing profile
   */
  public abstract boolean isAnnotationProcessorsEnabled();

  public static CompilerConfiguration getInstance(Project project) {
    return new CompilerConfiguration(){
      @Nullable
      @Override
      public String getBytecodeTargetLevel(Module module) {
        return null;
      }

      @NotNull
      @Override
      public AnnotationProcessingConfiguration getAnnotationProcessingConfiguration(Module module) {
        return new AnnotationProcessingConfiguration() {
          @Override
          public boolean isEnabled() {
            return false;
          }

          @NotNull
          @Override
          public String getProcessorPath() {
            return null;
          }

          @NotNull
          @Override
          public String getGeneratedSourcesDirectoryName(boolean forTests) {
            return null;
          }

          @Override
          public boolean isOutputRelativeToContentRoot() {
            return false;
          }

          @NotNull
          @Override
          public Set<String> getProcessors() {
            return null;
          }

          @NotNull
          @Override
          public Map<String, String> getProcessorOptions() {
            return null;
          }

          @Override
          public boolean isObtainProcessorsFromClasspath() {
            return false;
          }
        };
      }

      @Override
      public boolean isAnnotationProcessorsEnabled() {
        return false;
      }

      @Override
      public boolean isExcludedFromCompilation(VirtualFile virtualFile) {
        return false;
      }

      @Override
      public boolean isResourceFile(VirtualFile virtualFile) {
        return false;
      }

      @Override
      public boolean isResourceFile(String path) {
        return false;
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

  public abstract boolean isExcludedFromCompilation(VirtualFile virtualFile);

  public abstract boolean isResourceFile(VirtualFile virtualFile);

  public abstract boolean isResourceFile(String path);

  public abstract void addResourceFilePattern(String namePattern) throws MalformedPatternException;

  public abstract String[] getResourceFilePatterns();

  public abstract void removeResourceFilePatterns();

  public abstract void convertPatterns();

  public abstract boolean isAddNotNullAssertions();

  public abstract void setAddNotNullAssertions(boolean enabled);

  public abstract ExcludedEntriesConfiguration getExcludedEntriesConfiguration();
}