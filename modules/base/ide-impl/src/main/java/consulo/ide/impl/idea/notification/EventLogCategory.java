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
package consulo.ide.impl.idea.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import javax.annotation.Nonnull;

/**
 * @author gregsh
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class EventLogCategory {
  public static final ExtensionPointName<EventLogCategory> EP_NAME = ExtensionPointName.create(EventLogCategory.class);

  private final String myDisplayName;

  protected EventLogCategory(@Nonnull String displayName) {
    myDisplayName = displayName;
  }

  @Nonnull
  public final String getDisplayName() {
    return myDisplayName;
  }

  public abstract boolean acceptsNotification(@Nonnull String groupId);
}
