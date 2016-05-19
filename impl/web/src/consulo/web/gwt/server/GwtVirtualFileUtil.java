/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwt.server;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DeferredIcon;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.IconUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.web.gwt.shared.transport.GwtVirtualFile;

import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 18-May-16
 */
public class GwtVirtualFileUtil {
  public static GwtVirtualFile createVirtualFile(final Project project, final VirtualFile virtualFile) {
    final GwtVirtualFile gwtVirtualFile = new GwtVirtualFile();
    gwtVirtualFile.url = virtualFile.getUrl();
    gwtVirtualFile.name = virtualFile.getName();
    gwtVirtualFile.isDirectory = virtualFile.isDirectory();

    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        Icon icon = IconUtil.getIcon(virtualFile, Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY, project);
        if (icon instanceof DeferredIcon) {
          icon = ((DeferredIcon)icon).evaluate();
        }

        if (setIconLayers(icon, gwtVirtualFile.iconLayers)) {
          return;
        }

        if (icon instanceof RowIcon) {
          int iconCount = ((RowIcon)icon).getIconCount();
          for (int i = 0; i < iconCount; i++) {
            Icon nextIcon = ((RowIcon)icon).getIcon(i);

            if (i == 0) {
              setIconLayers(nextIcon, gwtVirtualFile.iconLayers);
            }
            else if (i == 1) {
              List<String> list = new SmartList<String>();
              setIconLayers(nextIcon, list);
              if (!list.isEmpty()) {
                gwtVirtualFile.rightIcon = list.get(0);
              }
            }
          }
        }
      }
    });

    VirtualFile[] children = virtualFile.getChildren();
    for (VirtualFile child : children) {
      gwtVirtualFile.children.add(createVirtualFile(project, child));
    }

    if(gwtVirtualFile.iconLayers.isEmpty()) {
      gwtVirtualFile.iconLayers.add(iconUrl(AllIcons.FileTypes.Unknown));
    }
    return gwtVirtualFile;
  }

  private static boolean setIconLayers(Icon icon, List<String> list) {
    if (icon instanceof RowIcon) {
      return false;
    }
    if (icon instanceof LayeredIcon) {
      Icon[] allLayers = ((LayeredIcon)icon).getAllLayers();
      for (Icon layerIcon : allLayers) {
        ContainerUtil.addIfNotNull(list, iconUrl(layerIcon));
      }
    }
    else {
      ContainerUtil.addIfNotNull(list, iconUrl(icon));
    }
    return true;
  }

  private static String iconUrl(Icon icon) {
    String maybeUrl = icon.toString();
    int i = maybeUrl.indexOf("!/");
    if (i != -1) {
       return maybeUrl.substring(i + 1, maybeUrl.length());
    }
    return null;
  }
}
