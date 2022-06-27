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
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoActionModel;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIExAWTDataKey;

import javax.annotation.Nonnull;

@ExtensionImpl
public class ActionSearchEverywhereContributorFactory implements SearchEverywhereContributorFactory<GotoActionModel.MatchedValue> {
  @Nonnull
  @Override
  public SearchEverywhereContributor<GotoActionModel.MatchedValue> createContributor(@Nonnull AnActionEvent initEvent) {
    return new ActionSearchEverywhereContributor(initEvent.getData(CommonDataKeys.PROJECT), initEvent.getData(UIExAWTDataKey.CONTEXT_COMPONENT), initEvent.getData(CommonDataKeys.EDITOR));
  }
}
