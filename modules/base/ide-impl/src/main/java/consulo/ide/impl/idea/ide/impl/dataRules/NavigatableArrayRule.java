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
package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.dataContext.DataProvider;
import consulo.dataContext.GetDataRule;
import consulo.navigation.Navigatable;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

public class NavigatableArrayRule implements GetDataRule<Navigatable[]> {
  @Nonnull
  @Override
  public Key<Navigatable[]> getKey() {
    return Navigatable.KEY_OF_ARRAY;
  }

  @Override
  public Navigatable[] getData(@Nonnull DataProvider dataProvider) {
    final Navigatable element = dataProvider.getDataUnchecked(Navigatable.KEY);
    return element == null ? null : new Navigatable[]{element};
  }
}
