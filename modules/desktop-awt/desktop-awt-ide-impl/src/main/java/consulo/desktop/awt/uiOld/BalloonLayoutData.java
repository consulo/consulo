/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.uiOld;

import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Alexander Lobas
 */
public class BalloonLayoutData {
  
  public static BalloonLayoutData createEmpty() {
    BalloonLayoutData layoutData = new BalloonLayoutData();
    layoutData.groupId = "";
    layoutData.showSettingButton = false;
    return layoutData;
  }

  public String groupId;
  public String id;
  public MergeInfo mergeData;

  public boolean showFullContent;

  public boolean welcomeScreen;
  public NotificationType type;

  public int height;
  public int twoLineHeight;
  public int fullHeight;
  public int maxScrollHeight;

  public boolean showMinSize;

  public Runnable closeAll;
  public Runnable doLayout;

  public boolean showSettingButton = true;
  public Supplier<Boolean> showActions;

  public Project project;

  public BalloonLayoutConfiguration configuration;

  public Runnable lafHandler;

  public long fadeoutTime;

  public Color textColor;
  public Color fillColor;
  public Color borderColor;

  
  public MergeInfo merge() {
    return new MergeInfo(mergeData, id);
  }

  
  public List<String> getMergeIds() {
    List<String> ids = new ArrayList<String>(mergeData.linkIds);
    ids.add(id);
    return ids;
  }

  public static class MergeInfo {
    public List<String> linkIds;
    public int count;

    public MergeInfo(@Nullable MergeInfo info, String linkId) {
      if (info == null) {
        linkIds = new ArrayList<String>();
        count = 1;
      }
      else {
        linkIds = info.linkIds;
        count = info.count + 1;
      }
      linkIds.add(linkId);
    }
  }
}