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
package consulo.language.editor.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.language.PairedBraceMatcher;
import consulo.language.editor.highlight.LanguageBraceMatcher;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 05-Sep-22
 */
@ExtensionImpl
public class LanguageBraceMatcherExtender implements ExtensionExtender<LanguageBraceMatcher> {
  @Override
  public void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<LanguageBraceMatcher> consumer) {
    for (PairedBraceMatcher matcher : componentManager.getExtensionList(PairedBraceMatcher.class)) {
      consumer.accept(new PairedBraceMatcherAdapter(matcher, matcher.getLanguage()));
    }
  }

  @Nonnull
  @Override
  public Class<LanguageBraceMatcher> getExtensionClass() {
    return LanguageBraceMatcher.class;
  }

  @Override
  public boolean hasAnyExtensions(ComponentManager componentManager) {
    return componentManager.getExtensionPoint(PairedBraceMatcher.class).hasAnyExtensions();
  }
}
