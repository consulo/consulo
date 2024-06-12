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
package consulo.desktop.awt.ui.impl.image;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import consulo.desktop.awt.ui.impl.image.reference.DesktopAWTPNGImageReference;
import consulo.desktop.awt.ui.impl.image.reference.DesktopAWTSVGImageReference;
import consulo.logging.Logger;
import consulo.ui.impl.image.BaseIconLibraryImpl;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.impl.image.ImageReference;
import consulo.util.io.UnsyncByteArrayInputStream;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-09-27
 */
public class DesktopAWTIconLibrary extends BaseIconLibraryImpl {
  private static final Logger LOG = Logger.getInstance(DesktopAWTIconLibrary.class);

  private final SVGLoader mySVGLoader = new SVGLoader();

  public DesktopAWTIconLibrary(String id, BaseIconLibraryManager baseIconLibraryManager) {
    super(id, baseIconLibraryManager);
  }

  @Nonnull
  @Override
  protected ImageReference createImageReference(@Nonnull byte[] _1xData,
                                                @Nullable byte[] _2xdata,
                                                boolean isSVG,
                                                String groupId,
                                                String imageId) {
    if (isSVG) {
      try {
        SVGDocument _x1Diagram = Objects.requireNonNull(mySVGLoader.load(new UnsyncByteArrayInputStream(_1xData)));
        SVGDocument _x2Diagram = null;

        if (_2xdata != null) {
          _x2Diagram = Objects.requireNonNull(mySVGLoader.load(new UnsyncByteArrayInputStream(_2xdata)));
        }

        return new DesktopAWTSVGImageReference(groupId, imageId, _x1Diagram, _x2Diagram);
      }
      catch (Exception e) {
        LOG.warn(e);
        return ImageReference.INVALID;
      }
    }
    else {
      return new DesktopAWTPNGImageReference(DesktopAWTPNGImageReference.ImageBytes.of(_1xData),
                                             DesktopAWTPNGImageReference.ImageBytes.of(_2xdata)
      );
    }
  }
}
