/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal.image;

import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import consulo.logging.Logger;
import consulo.ui.desktop.internal.image.libraryImage.DesktopAWTImageImpl;
import consulo.ui.desktop.internal.image.libraryImage.DesktopSvgImageImpl;
import consulo.ui.image.Image;
import consulo.ui.impl.image.BaseIconLibraryImpl;
import consulo.ui.impl.image.BaseIconLibraryManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-09-27
 */
public class DesktopIconLibrary extends BaseIconLibraryImpl {
  private static final Logger LOG = Logger.getInstance(DesktopIconLibrary.class);

  private final SVGUniverse mySVGUniverse = new SVGUniverse();
  
  public DesktopIconLibrary(String id, BaseIconLibraryManager baseIconLibraryManager) {
    super(id, baseIconLibraryManager);
  }

  @Nullable
  @Override
  protected Image createImage(@Nonnull byte[] _1xData, @Nullable byte[] _2xdata, boolean isSVG, int width, int height, String groupId, String imageId) {
    if (isSVG) {
      try {
        URI _x1Url = mySVGUniverse.loadSVG(new UnsyncByteArrayInputStream(_1xData), getId() + "/" + groupId + "/" + imageId);

        SVGDiagram _x1Diagram = Objects.requireNonNull(mySVGUniverse.getDiagram(_x1Url, false));
        SVGDiagram _x2Diagram = null;

        if(_2xdata != null) {
          URI _x2Url = mySVGUniverse.loadSVG(new UnsyncByteArrayInputStream(_2xdata), getId() + "/" + groupId + "/" + imageId + "/2x");
          _x2Diagram = Objects.requireNonNull(mySVGUniverse.getDiagram(_x2Url));
        }

        return new DesktopSvgImageImpl(_x1Diagram, _x2Diagram, width, height, null, groupId, imageId);
      }
      catch (IOException e) {
        LOG.warn(e);
        return Image.empty(width, height);
      }
    }
    else {
      return new DesktopAWTImageImpl(DesktopAWTImageImpl.ImageBytes.of(_1xData), DesktopAWTImageImpl.ImageBytes.of(_2xdata), width, height, null);
    }
  }
}
