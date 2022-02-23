/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;

import javax.annotation.Nonnull;

/**
 * {@link ToggleUseSoftWrapsMenuAction} extension that doesn't suppress configured icon (if any).
 *
 * @author Denis Zhdanov
 * @since Aug 19, 2010 5:07:08 PM
 */
public class ToggleUseSoftWrapsToolbarAction extends AbstractToggleUseSoftWrapsAction {

  public ToggleUseSoftWrapsToolbarAction(@Nonnull SoftWrapAppliancePlaces place) {
    super(place, true);
    copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS));
  }
}
