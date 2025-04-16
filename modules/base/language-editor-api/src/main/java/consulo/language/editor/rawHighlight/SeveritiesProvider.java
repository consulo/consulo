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
package consulo.language.editor.rawHighlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.ui.color.ColorValue;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author anna
 * @since 2009-01-17
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class SeveritiesProvider {
  public static final ExtensionPointName<SeveritiesProvider> EP_NAME = ExtensionPointName.create(SeveritiesProvider.class);

  /**
   * @see TextAttributesKey#createTextAttributesKey(String, TextAttributes)
   */
  @Nonnull
  public abstract List<HighlightInfoType> getSeveritiesHighlightInfoTypes();

  public boolean isGotoBySeverityEnabled(HighlightSeverity minSeverity) {
    return minSeverity != HighlightSeverity.INFORMATION;
  }

  public ColorValue getTrafficRendererColor(@Nonnull TextAttributes textAttributes) {
    return textAttributes.getErrorStripeColor();
  }
}
