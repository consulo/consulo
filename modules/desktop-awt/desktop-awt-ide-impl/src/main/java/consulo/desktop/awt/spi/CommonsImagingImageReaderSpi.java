/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.desktop.awt.spi;

import consulo.util.collection.ArrayUtil;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;
import org.apache.commons.imaging.*;
import org.apache.commons.imaging.common.bytesource.ByteSource;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CommonsImagingImageReaderSpi extends ImageReaderSpi {
  private ThreadLocal<ImageFormat> myFormat = new ThreadLocal<>();
  private final List<ImageFormat> myFormats;

  public CommonsImagingImageReaderSpi() {
    super();
    vendorName = "consulo.io";
    version = "1.0";

    // todo standard GIF/BMP formats can be optionally skipped as well
    // JPEG is skipped due to Exception: cannot read or write JPEG images. (JpegImageParser.java:92)
    // tiff reader seems to be broken
    // PNG reader has bugs with well-compressed PNG images, use standard one instead
    myFormats = new ArrayList<>(Arrays.asList(ImageFormats.values()));
    myFormats.removeAll(Arrays.asList(ImageFormats.UNKNOWN, ImageFormats.JPEG, ImageFormats.TIFF, ImageFormats.PNG));

    suffixes = new String[myFormats.size()];
    MIMETypes = new String[myFormats.size()];
    pluginClassName = MyImageReader.class.getName();
    inputTypes = new Class[]{ImageInputStream.class};

    Set<String> names = new LinkedHashSet<>();
    Set<String> suffixes = new LinkedHashSet<>();
    Set<String> MIMETypes = new LinkedHashSet<>();

    for (ImageFormat format : myFormats) {
      for (String extension : format.getExtensions()) {
        names.add(extension);
        String loweredExtension = StringUtil.toLowerCase(extension);
        names.add(loweredExtension);

        suffixes.add(loweredExtension);
        MIMETypes.add("image/" + loweredExtension);
      }
    }

    this.names = names.toArray(new String[names.size()]);
    this.suffixes = suffixes.toArray(new String[suffixes.size()]);
    this.MIMETypes = MIMETypes.toArray(new String[MIMETypes.size()]);
  }

  @Override
  public String getDescription(Locale locale) {
    return "Apache Commons Imaging adapter reader";
  }

  @Override
  public boolean canDecodeInput(Object input) throws IOException {
    if (!(input instanceof ImageInputStream)) {
      return false;
    }

    ImageInputStream stream = (ImageInputStream)input;
    ImageFormat imageFormat = Imaging.guessFormat(new MyByteSource(stream));
    if (myFormats.contains(imageFormat)) {
      myFormat.set(imageFormat);
      return true;
    }
    return false;
  }

  @Override
  public ImageReader createReaderInstance(Object extension) {
    return new MyImageReader(this, myFormat.get());
  }

  private static class MyByteSource extends ByteSource {
    private final ImageInputStream myStream;

    public MyByteSource(ImageInputStream stream) {
      super(stream.toString());
      myStream = stream;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      myStream.seek(0);
      return new InputStream() {
        @Override
        public int read() throws IOException {
          return myStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          return myStream.read(b, off, len);
        }
      };
    }

    @Override
    public byte[] getBlock(int start, int length) throws IOException {
      myStream.seek(start);
      byte[] bytes = new byte[length];
      int read = myStream.read(bytes);
      return ArrayUtil.realloc(bytes, read);
    }

    @Override
    public byte[] getBlock(long start, int length) throws IOException {
      myStream.seek(start);
      byte[] bytes = new byte[length];
      int read = myStream.read(bytes);
      return ArrayUtil.realloc(bytes, read);
    }

    @Override
    public byte[] getAll() throws IOException {
      return StreamUtil.loadFromStream(getInputStream());
    }

    @Override
    public long getLength() throws IOException {
      return myStream.length();
    }

    @Override
    public String getDescription() {
      return myStream.toString();
    }
  }

  private static class MyImageReader extends ImageReader {
    private byte[] myBytes;
    private ImageInfo myInfo;
    private BufferedImage[] myImages;
    private final ImageFormat myDefaultFormat;

    private MyImageReader(CommonsImagingImageReaderSpi provider, ImageFormat imageFormat) {
      super(provider);
      myDefaultFormat = imageFormat == null ? ImageFormats.UNKNOWN : imageFormat;
    }

    @Override
    public void dispose() {
      myBytes = null;
      myInfo = null;
      myImages = null;
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
      super.setInput(input, seekForwardOnly, ignoreMetadata);
      myBytes = null;
      myInfo = null;
      myImages = null;
    }

    private ImageInfo getInfo() throws IOException {
      if (myInfo == null) {
        try {
          myInfo = Imaging.getImageInfo(getBytes());
        }
        catch (ImageReadException e) {
          throw new IOException(e);
        }
      }
      return myInfo;
    }

    private byte[] getBytes() throws IOException {
      if (myBytes == null) {
        ImageInputStream stream = (ImageInputStream)input;
        myBytes = new MyByteSource(stream).getAll();
      }
      return myBytes;
    }

    private BufferedImage[] getImages() throws IOException {
      if (myImages == null) {
        try {
          List<BufferedImage> images = Imaging.getAllBufferedImages(getBytes());
          myImages = images.toArray(new BufferedImage[images.size()]);
        }
        catch (ImageReadException e) {
          throw new IOException(e);
        }
      }
      return myImages;
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
      return getInfo().getNumberOfImages();
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
      return getInfo().getWidth();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
      return getInfo().getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
      return Collections.singletonList(ImageTypeSpecifier.createFromRenderedImage(getImages()[imageIndex])).iterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
      return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
      return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
      return getImages()[imageIndex];
    }

    @Override
    public String getFormatName() throws IOException {
      // return default if called before setInput
      return input == null ? myDefaultFormat.getName() : getInfo().getFormat().getName();
    }
  }
}
