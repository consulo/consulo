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
package consulo.language.editor.impl.internal.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.CompletionStatistician;
import consulo.language.statistician.StatisticsInfo;
import consulo.language.statistician.StatisticsManager;
import consulo.language.editor.completion.CompletionLocation;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementDecorator;

/**
 * @author peter
 */
@ExtensionImpl(id = "decorator", order = "first")
public class DecoratorCompletionStatistician extends CompletionStatistician {

  @Override
  public StatisticsInfo serialize(LookupElement element, CompletionLocation location) {
    if (element instanceof LookupElementDecorator) {
      return StatisticsManager.serialize(STATISTICS_KEY, ((LookupElementDecorator)element).getDelegate(), location);
    }
    return null;
  }
}