/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.document;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionPointName;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An extension point to create filters which may put restrictions on how trailing spaces will be handled in a document.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class StripTrailingSpacesFilterFactory {
  public static final ExtensionPointName<StripTrailingSpacesFilterFactory> EXTENSION_POINT = ExtensionPointName.create(StripTrailingSpacesFilterFactory.class);

  /**
   * Creates a filter which may restrict trailing spaces removal.
   *
   * @param project The current project or null if there is no project context.
   * @param document The document to be processed.
   * @return The filter which will be called on document save. The factory may return one of the several predefined filters:<ul>
   *         <li>{@link StripTrailingSpacesFilter#NOT_ALLOWED}</li> No stripping allowed. IDEA will not try to strip any whitespace at all in this case.
   *         <li>{@link StripTrailingSpacesFilter#POSTPONED}</li> The stripping is not possible at the moment. For example, the caret
   *         is in the way and the "Settings|General|Editor|Allow caret after end of the line" is off. In this case the IDEA will try to restart
   *         the stripping later.
   *         <li>{@link StripTrailingSpacesFilter#ALL_LINES}</li> Allow stripping with no restrictions. Return this value by default.
   *         </ul>
   */
  @Nonnull
  public abstract StripTrailingSpacesFilter createFilter(@Nullable ComponentManager project, @Nonnull Document document);
}
