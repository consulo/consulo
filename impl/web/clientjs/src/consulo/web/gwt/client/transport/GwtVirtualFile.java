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

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-May-16
 */
public class GwtVirtualFile implements IsSerializable {
  private String url;

  private List<String> iconLayers = new ArrayList<String>();

  private String rightIcon;

  private String name;

  private List<GwtVirtualFile> children = new ArrayList<GwtVirtualFile>();

  private boolean isDirectory;

  public GwtVirtualFile() {
  }

  public List<String> getIconLayers() {
    return iconLayers;
  }

  public String getRightIcon() {
    return rightIcon;
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
