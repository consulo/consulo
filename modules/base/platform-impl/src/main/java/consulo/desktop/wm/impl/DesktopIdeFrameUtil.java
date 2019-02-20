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
package consulo.desktop.wm.impl;

import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import consulo.awt.TargetAWT;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-02-20
 */
public class DesktopIdeFrameUtil {
  @Nullable
  public static IdeFrame findIdeFrameFromParent(@Nullable Component component) {
     if(component == null) {
       return null;
     }

     Component target = component;

     while (target != null) {
       if(target instanceof Window) {
         consulo.ui.Window uiWindow = TargetAWT.from((Window)target);

         IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
         if(ideFrame != null) {
           return ideFrame;
         }
       }

       target = target.getParent();
     }

     return null;
  }

  @Nullable
  public static IdeFrameEx findIdeFrameExFromParent(@Nullable Component component) {
    if (component == null) {
      return null;
    }

    Component target = component;

    while (target != null) {
      if (target instanceof Window) {
        consulo.ui.Window uiWindow = TargetAWT.from((Window)target);

        IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
        if (ideFrame instanceof IdeFrameEx) {
          return (IdeFrameEx)ideFrame;
        }
      }

      target = target.getParent();
    }

    return null;
  }
}
