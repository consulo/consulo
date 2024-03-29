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
package consulo.ide.impl.idea.openapi.actionSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 * @since 13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class AbbreviationManager {
  public static AbbreviationManager getInstance() {
    return ServiceManager.getService(AbbreviationManager.class);
  }

  public abstract Set<String> getAbbreviations();

  public abstract Set<String> getAbbreviations(String actionId);

  public abstract List<String> findActions(String abbreviation);

  public abstract void register(String abbreviation, String actionId);

  public abstract void remove(String abbreviation, String actionId);

  public abstract void removeAllAbbreviations(@Nonnull String actionId);
}
