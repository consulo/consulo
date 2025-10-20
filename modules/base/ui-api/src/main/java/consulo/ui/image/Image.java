/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.image;

import consulo.annotation.DeprecationInfo;
import consulo.ui.Size2D;
import consulo.ui.internal.UIInternal;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public interface Image {
  Image[] EMPTY_ARRAY = new Image[0];

  int DEFAULT_ICON_SIZE = 16;

  enum ImageType {
    PNG,
    SVG
  }

  @Nonnull
  @Deprecated
  static Image create(@Nonnull URL url) throws IOException {
    return fromUrl(url);
  }

  @Nonnull
  static Image fromUrl(@Nonnull URL url) throws IOException {
    return UIInternal.get()._Image_fromUrl(url);
  }

  /**
   * Return image from bytes. JPG, PNG only
   */
  @Nonnull
  @Deprecated
  static Image fromBytes(@Nonnull byte[] bytes, int width, int height) throws IOException {
    return fromBytes(ImageType.PNG, bytes);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Image#fromBytes(imageType, bytes) - width&height ignored")
  static Image fromBytes(@Nonnull ImageType imageType, @Nonnull byte[] bytes, int width, int height) throws IOException {
    return fromBytes(imageType, bytes);
  }

  @Nonnull
  static Image fromBytes(@Nonnull ImageType imageType, @Nonnull byte[] bytes) throws IOException {
    return fromStream(imageType, new ByteArrayInputStream(bytes));
  }

  @Nonnull
  static Image fromStream(@Nonnull ImageType imageType, @Nonnull InputStream stream) throws IOException {
    return UIInternal.get()._Image_fromStream(imageType, stream);
  }

  @Nonnull
  static Image lazy(@Nonnull Supplier<Image> imageSupplier) {
    return UIInternal.get()._Image_lazy(imageSupplier);
  }

  @Nonnull
  static <S> Image stated(@Nonnull ImageState<S> state, @Nonnull Function<S, Image> funcCall) {
    return UIInternal.get()._Image_stated(state, funcCall);
  }

  @Nonnull
  static EmptyImage empty() {
    return empty(0);
  }

  @Nonnull
  static EmptyImage empty(int widthAndHeight) {
    return UIInternal.get()._ImageEffects_empty(widthAndHeight, widthAndHeight);
  }

  @Nonnull
  static EmptyImage empty(int width, int height) {
    return UIInternal.get()._ImageEffects_empty(width, height);
  }

  int getHeight();

  int getWidth();

  @Nonnull
  default Size2D getSize() {
    return new Size2D(getWidth(), getHeight());
  }
}
