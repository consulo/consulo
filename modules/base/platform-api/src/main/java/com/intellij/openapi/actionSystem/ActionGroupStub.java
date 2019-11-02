/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.openapi.actionSystem;

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.ObjectUtil;

/**
 * from kotlin
 */
public class ActionGroupStub extends DefaultActionGroup implements ActionStubBase {
  private final String myId;
  private final String myActionClass;
  private final ClassLoader myClassLoader;
  private PluginId myPluginId;
  private String myIconPath;
  private boolean myPopupDefinedInXml;

  public ActionGroupStub(String id, String actionClass, ClassLoader classLoader, PluginId pluginId) {
    myId = id;
    myActionClass = actionClass;
    myClassLoader = classLoader;
    myPluginId = pluginId;
  }

  public String getActionClass() {
    return myActionClass;
  }

  public void setPopupDefinedInXml(boolean popupDefinedInXml) {
    myPopupDefinedInXml = popupDefinedInXml;
  }

  public boolean isPopupDefinedInXml() {
    return myPopupDefinedInXml;
  }

  public ClassLoader getClassLoader() {
    return myClassLoader;
  }

  @Override
  public String getId() {
    return myId;
  }

  @Override
  public PluginId getPluginId() {
    return myPluginId;
  }

  @Override
  public String getIconPath() {
    return myIconPath;
  }

  public void setIconPath(String iconPath) {
    myIconPath = iconPath;
  }

  public void initGroup(ActionGroup target, ActionManager actionManager) {
    ActionStub.copyTemplatePresentation(getTemplatePresentation(), target.getTemplatePresentation());

    target.setShortcutSet(getShortcutSet());

    AnAction[] children = getChildren(null, actionManager);
    if (children.length > 0) {
      DefaultActionGroup dTarget = ObjectUtil.tryCast(target, DefaultActionGroup.class);
      if(dTarget == null) {
        throw new PluginException("Action group class must extend DefaultActionGroup for the group to accept children:" + myActionClass, myPluginId);
      }

      for (AnAction action : children) {
        dTarget.addAction(action, Constraints.LAST, actionManager);
      }
    }

    if(myPopupDefinedInXml) {
      target.setPopup(isPopup());
    }
  }
}
