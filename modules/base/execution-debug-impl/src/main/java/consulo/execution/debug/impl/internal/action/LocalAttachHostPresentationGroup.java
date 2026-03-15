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
package consulo.execution.debug.impl.internal.action;

import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.execution.debug.attach.XAttachHost;
import consulo.execution.debug.attach.XAttachPresentationGroup;
import consulo.ui.image.Image;

/**
 * from kotlin
 */
public class LocalAttachHostPresentationGroup implements XAttachPresentationGroup<XAttachHost> {
  public static final LocalAttachHostPresentationGroup INSTANCE = new LocalAttachHostPresentationGroup();

  // Should be at the bottom of the list
  @Override
  public int getOrder() {
    return Integer.MAX_VALUE;
  }

  
  @Override
  public String getGroupName() {
    return "";
  }

  
  @Override
  public Image getProcessIcon(Project project, XAttachHost info, UserDataHolder dataHolder) {
    throw new UnsupportedOperationException();
  }

  
  @Override
  public String getProcessDisplayText(Project project, XAttachHost info, UserDataHolder dataHolder) {
    throw new UnsupportedOperationException();
  }

  
  @Override
  public Image getItemIcon(Project project, XAttachHost info, UserDataHolder dataHolder) {
    return Image.empty(16);
  }

  
  @Override
  public String getItemDisplayText(Project project, XAttachHost info, UserDataHolder dataHolder) {
    return "Local Host";
  }

  @Override
  public int compare(XAttachHost o1, XAttachHost o2) {
    return Integer.compare(o1.hashCode(), o2.hashCode());
  }
}
