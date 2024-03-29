/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.continuation;

import jakarta.annotation.Nonnull;

public class TaskData {
  private final String myName;
  @Nonnull
  private final Where myWhere;

  public TaskData(String name, @Nonnull Where where) {
    myName = name;
    myWhere = where;
  }

  public String getName() {
    return myName;
  }

  @Nonnull
  public Where getWhere() {
    return myWhere;
  }
}
