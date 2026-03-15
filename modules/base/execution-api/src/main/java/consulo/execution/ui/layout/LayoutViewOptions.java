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

package consulo.execution.ui.layout;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.content.Content;

import org.jspecify.annotations.Nullable;

public interface LayoutViewOptions {

  String STARTUP = "startup";

  
  LayoutViewOptions setTopToolbar(ActionGroup actions, String place);

  
  LayoutViewOptions setLeftToolbar(ActionGroup leftToolbar, String place);

  
  LayoutViewOptions setMinimizeActionEnabled(boolean enabled);

  
  LayoutViewOptions setMoveToGridActionEnabled(boolean enabled);

  
  LayoutViewOptions setAttractionPolicy(String contentId, LayoutAttractionPolicy policy);

  
  LayoutViewOptions setConditionAttractionPolicy(String condition, LayoutAttractionPolicy policy);

  boolean isToFocus(Content content, String condition);

  
  LayoutViewOptions setToFocus(@Nullable Content content, String condition);

  AnAction getLayoutActions();
  
  AnAction[] getLayoutActionsList();

  
  LayoutViewOptions setTabPopupActions(ActionGroup group);
  
  LayoutViewOptions setAdditionalFocusActions(ActionGroup group);

  AnAction getSettingsActions();
  
  AnAction[] getSettingsActionsList();
}