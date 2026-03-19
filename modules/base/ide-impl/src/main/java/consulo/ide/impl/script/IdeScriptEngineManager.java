/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.script;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;

import org.jspecify.annotations.Nullable;

import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class IdeScriptEngineManager {
  public static IdeScriptEngineManager getInstance() {
    return ServiceManager.getService(IdeScriptEngineManager.class);
  }

  
  public abstract List<String> getLanguages();

  
  public abstract List<String> getFileExtensions(@Nullable String language);

  public abstract @Nullable IdeScriptEngine getEngineForLanguage(String language, @Nullable ClassLoader loader);

  public abstract @Nullable IdeScriptEngine getEngineForFileExtension(String extension, @Nullable ClassLoader loader);

  public abstract boolean isInitialized();
}
