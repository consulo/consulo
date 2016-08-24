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
package consulo.ui.internal;

import consulo.ui.ImageRef;

import java.io.Serializable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public class WGwtLayeredImageImpl extends WGwtBaseComponent {
  private ImageRef[] myImageRefs;

  public WGwtLayeredImageImpl(ImageRef[] imageRefs) {
    myImageRefs = imageRefs;
  }

  @Override
  protected void getState(Map<String, Serializable> map) {
    super.getState(map);
    map.put("size", myImageRefs.length);
    map.put("height", myImageRefs[0].getHeight());
    map.put("width", myImageRefs[0].getWidth());

    for (int i = 0; i < myImageRefs.length; i++) {
      WGwtImageRefImpl imageRef = (WGwtImageRefImpl)myImageRefs[i];

      map.put("url" + i, imageRef.getUrlForBrowser());
    }
  }
}
