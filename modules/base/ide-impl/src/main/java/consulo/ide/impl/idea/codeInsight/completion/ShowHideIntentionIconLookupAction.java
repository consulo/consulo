/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.language.editor.completion.lookup.LookupElementAction;
import consulo.application.util.registry.Registry;
import consulo.platform.base.icon.PlatformIconGroup;

/**
 * @author Konstantin Bulenkov
 */
public class ShowHideIntentionIconLookupAction extends LookupElementAction {
  static final String KEY = "completion.show.intention.icon";

  public ShowHideIntentionIconLookupAction() {
    super(PlatformIconGroup.actionsIntentionbulb(), (shouldShowLookupHint() ? "Never show" : "Show") + " intention icon");
  }

  public static boolean shouldShowLookupHint() {
    return Registry.is(KEY);
  }

  @Override
  public Result performLookupAction() {
    Registry.get(KEY).setValue(!shouldShowLookupHint());
    return Result.REFRESH_ITEM;
  }
}
