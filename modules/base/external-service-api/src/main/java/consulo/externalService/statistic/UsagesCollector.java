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
package consulo.externalService.statistic;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;

import org.jspecify.annotations.Nullable;
import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class UsagesCollector {
  
  public abstract Set<UsageDescriptor> getUsages(@Nullable Project project) throws CollectUsagesException;

  
  public abstract String getGroupId();
}
