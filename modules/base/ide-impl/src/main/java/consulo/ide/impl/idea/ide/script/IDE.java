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
package consulo.ide.impl.idea.ide.script;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.ide.impl.script.IdeScriptEngine;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class IDE {
  public final Application application = ApplicationManager.getApplication();
  public final Project project;

  private final Map<Object, Object> bindings = ContainerUtil.newConcurrentMap();
  private final IdeScriptEngine myEngine;

  public IDE(@Nullable Project project, @Nonnull IdeScriptEngine engine) {
    this.project = project;
    myEngine = engine;
  }

  public void print(Object o) {
    print(myEngine.getStdOut(), o);
  }

  public void error(Object o) {
    print(myEngine.getStdErr(), o);
  }

  public Object put(Object key, Object value) {
    return value == null ? bindings.remove(key) : bindings.put(key, value);
  }

  public Object get(Object key) {
    return bindings.get(key);
  }

  private static void print(Writer writer, Object o) {
    try {
      writer.append(String.valueOf(o)).append("\n");
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
