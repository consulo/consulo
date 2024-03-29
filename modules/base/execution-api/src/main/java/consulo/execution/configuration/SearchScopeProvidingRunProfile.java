/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.execution.configuration;

import consulo.module.Module;

import jakarta.annotation.Nonnull;

/**
 * Base interface for run configurations that can specify which part of the project should be used to search sources. This information
 * will be used to provide more accurate navigation to sources from stack traces, debugger, etc
 *
 * @author nik
 */
public interface SearchScopeProvidingRunProfile extends RunProfile {
  /**
   * @return modules where to search sources for this configuration
   */
  @Nonnull
  Module[] getModules();
}