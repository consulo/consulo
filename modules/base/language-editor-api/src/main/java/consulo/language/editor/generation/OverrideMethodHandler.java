/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.generation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.editor.action.LanguageCodeInsightActionHandler;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageOneToOne;

/**
 * @author VISTALL
 * @since 17-Jul-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface OverrideMethodHandler extends LanguageCodeInsightActionHandler {
  ExtensionPointCacheKey<OverrideMethodHandler, ByLanguageValue<OverrideMethodHandler>> KEY = ExtensionPointCacheKey.create("OverrideMethodHandler", LanguageOneToOne.build());
}
