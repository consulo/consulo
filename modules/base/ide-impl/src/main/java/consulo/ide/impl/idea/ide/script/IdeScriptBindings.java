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

import consulo.project.Project;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.ide.impl.script.IdeScriptEngine;

public class IdeScriptBindings {
  public static final Binding<IDE> IDE = Binding.create("IDE", IDE.class);

  public static void ensureIdeIsBound(@Nullable Project project, @Nonnull IdeScriptEngine engine) {
    IDE oldIdeBinding = IDE.get(engine);
    if (oldIdeBinding == null) {
      IDE.set(engine, new IDE(project, engine));
    }
  }

  private IdeScriptBindings() {
  }

  public static class Binding<T> {
    private final String myName;
    private final Class<T> myClass;

    private Binding(@Nonnull String name, @Nonnull Class<T> clazz) {
      myName = name;
      myClass = clazz;
    }

    public void set(@Nonnull IdeScriptEngine engine, T value) {
      engine.setBinding(myName, value);
    }

    public T get(@Nonnull IdeScriptEngine engine) {
      return ObjectUtil.tryCast(engine.getBinding(myName), myClass);
    }

    static <T> Binding<T> create(@Nonnull String name, @Nonnull Class<T> clazz) {
      return new Binding<>(name, clazz);
    }
  }
}
