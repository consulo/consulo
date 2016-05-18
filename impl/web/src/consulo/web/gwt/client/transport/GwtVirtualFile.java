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
package consulo.web.gwt.client.transport;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DeferredIcon;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.IconUtil;
import com.intellij.util.SmartList;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 16-May-16
 */
public class GwtVirtualFile implements IsSerializable {
  private String url;

  private List<String> iconLayers = new ArrayList<String>();

  private String rightIcon;

  private String name;

  private List<GwtVirtualFile> children = new ArrayList<GwtVirtualFile>();

  private boolean isDirectory;

  public GwtVirtualFile(final Project project, final VirtualFile virtualFile) {
    url = virtualFile.getUrl();
    name = virtualFile.getName();
    isDirectory = virtualFile.isDirectory();

    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        Icon icon = IconUtil.getIcon(virtualFile, Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY, project);
        if (icon instanceof DeferredIcon) {
          icon = ((DeferredIcon)icon).evaluate();
        }

        if(setIconLayers(icon, iconLayers)) {
          return;
        }

        if (icon instanceof RowIcon) {
          int iconCount = ((RowIcon)icon).getIconCount();
          for (int i = 0; i < iconCount; i++) {
            Icon nextIcon = ((RowIcon)icon).getIcon(i);

            if(i == 0) {
              setIconLayers(nextIcon, iconLayers);
            }
            else if(i == 1) {
              List<String> list = new SmartList<String>();
              setIconLayers(nextIcon, list);
              if(!list.isEmpty()) {
                rightIcon = list.get(0);
              }
            }
          }
        }
      }
    });

    VirtualFile[] children = virtualFile.getChildren();
    for (VirtualFile child : children) {
      this.children.add(new GwtVirtualFile(project, child));
    }
  }

  private static boolean setIconLayers(Icon icon, List<String> list) {
    if(icon instanceof RowIcon) {
      return false;
    }
    if (icon instanceof LayeredIcon) {
      Icon[] allLayers = ((LayeredIcon)icon).getAllLayers();
      for (Icon layerIcon : allLayers) {
        String maybeUrl = layerIcon.toString();
        int i = maybeUrl.indexOf("!/");
        if (i != -1) {
          list.add(maybeUrl.substring(i + 2, maybeUrl.length()));
        }
      }
    }
    else {
      String maybeUrl = icon.toString();
      int i = maybeUrl.indexOf("!/");
      if (i != -1) {
        list.add(maybeUrl.substring(i + 2, maybeUrl.length()));
      }
    }
    return true;
  }

  public String getUrl() {
    return url;
  }

  public String getName() {
    return name;
  }

  public List<GwtVirtualFile> getChildren() {
    return children;
  }

  public boolean isDirectory() {
    return isDirectory;
  }
}
