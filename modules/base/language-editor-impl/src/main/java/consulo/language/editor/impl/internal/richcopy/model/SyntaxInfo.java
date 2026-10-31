/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.richcopy.model;

import consulo.ui.color.ColorValue;
import consulo.ui.font.Typeface;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * @author Denis Zhdanov
 * @since 3/25/13 1:19 PM
 */
public class SyntaxInfo {
  private final int myOutputInfoCount;
  private final byte[] myOutputInfosSerialized;
  private final @Nullable SyntaxInfoCompressor myCompressor;
  
  private final ColorRegistry    myColorRegistry;
  
  private final TypefaceRegistry myTypefaceRegistry;

  private final int myDefaultForeground;
  private final int myDefaultBackground;
  private final float myFontSize;

  private SyntaxInfo(int outputInfoCount,
                     byte[] outputInfosSerialized,
                     @Nullable SyntaxInfoCompressor compressor,
                     int defaultForeground,
                     int defaultBackground,
                     float fontSize,
                     TypefaceRegistry typefaceRegistry,
                     ColorRegistry colorRegistry)
  {
    myOutputInfoCount = outputInfoCount;
    myOutputInfosSerialized = outputInfosSerialized;
    myCompressor = compressor;
    myDefaultForeground = defaultForeground;
    myDefaultBackground = defaultBackground;
    myFontSize = fontSize;
    myTypefaceRegistry = typefaceRegistry;
    myColorRegistry = colorRegistry;
  }

  
  public ColorRegistry getColorRegistry() {
    return myColorRegistry;
  }

  
  public TypefaceRegistry getTypefaceRegistry() {
    return myTypefaceRegistry;
  }

  public int getDefaultForeground() {
    return myDefaultForeground;
  }

  public int getDefaultBackground() {
    return myDefaultBackground;
  }

  public float getFontSize() {
    return myFontSize;
  }

  public void processOutputInfo(MarkupHandler handler) {
    MarkupIterator it = new MarkupIterator();
    while (it.hasNext()) {
      it.processNext(handler);
      if (!handler.canHandleMore()) {
        break;
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append("default colors: foreground=").append(myDefaultForeground).append(", background=").append(myDefaultBackground).append("; output infos: ");
    boolean first = true;
    MarkupIterator it = new MarkupIterator();
    while (it.hasNext()) {
      if (first) {
        b.append(',');
      }
      it.processNext(new MarkupHandler() {
        @Override
        public void handleText(int startOffset, int endOffset) throws Exception {
          b.append("text(").append(startOffset).append(",").append(endOffset).append(")");
        }

        @Override
        public void handleForeground(int foregroundId) throws Exception {
          b.append("foreground(").append(foregroundId).append(")");
        }

        @Override
        public void handleBackground(int backgroundId) throws Exception {
          b.append("background(").append(backgroundId).append(")");
        }

        @Override
        public void handleFont(int fontNameId) throws Exception {
          b.append("font(").append(fontNameId).append(")");
        }

        @Override
        public void handleStyle(int style) throws Exception {
          b.append("style(").append(style).append(")");
        }

        @Override
        public boolean canHandleMore() {
          return true;
        }
      });
      first = false;
    }
    return b.toString();
  }

  public static class Builder {
    private final ColorRegistry myColorRegistry = new ColorRegistry();
    private final TypefaceRegistry myTypefaceRegistry = new TypefaceRegistry();
    private final int myDefaultForeground;
    private final int myDefaultBackground;
    private final float myFontSize;
    private final BufferExposingByteArrayOutputStream myStream = new BufferExposingByteArrayOutputStream();
    private final OutputInfoSerializer.OutputStream myOutputInfoStream;
    private final @Nullable SyntaxInfoCompressor myCompressor;
    private int myOutputInfoCount;

    public Builder(ColorValue defaultForeground, ColorValue defaultBackground, float fontSize) {
      this(defaultForeground, defaultBackground, fontSize, null);
    }

    /**
     * @param compressor packs the markup of a syntax info which is kept for long, as the clipboard keeps it; without one the markup
     *                   is stored as is
     */
    public Builder(ColorValue defaultForeground, ColorValue defaultBackground, float fontSize, @Nullable SyntaxInfoCompressor compressor) {
      myDefaultForeground = myColorRegistry.getId(defaultForeground);
      myDefaultBackground = myColorRegistry.getId(defaultBackground);
      myFontSize = fontSize;
      myCompressor = compressor;
      myOutputInfoStream = new OutputInfoSerializer.OutputStream(myStream);
    }

    public void addFontStyle(int fontStyle) {
      try {
        myOutputInfoStream.handleStyle(fontStyle);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      myOutputInfoCount++;
    }

    public void addTypeface(Typeface typeface) {
      try {
        myOutputInfoStream.handleFont(myTypefaceRegistry.getId(typeface));
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      myOutputInfoCount++;
    }

    public void addForeground(ColorValue foreground) {
      try {
        myOutputInfoStream.handleForeground(myColorRegistry.getId(foreground));
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      myOutputInfoCount++;
    }

    public void addBackground(ColorValue background) {
      try {
        myOutputInfoStream.handleBackground(myColorRegistry.getId(background));
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      myOutputInfoCount++;
    }

    public void addText(int startOffset, int endOffset) {
      try {
        myOutputInfoStream.handleText(startOffset, endOffset);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      myOutputInfoCount++;
    }

    public SyntaxInfo build() {
      myColorRegistry.seal();
      myTypefaceRegistry.seal();
      byte[] serialized = myCompressor == null
        ? myStream.toByteArray()
        : myCompressor.compress(myStream.getInternalBuffer(), 0, myStream.size());
      return new SyntaxInfo(myOutputInfoCount, serialized, myCompressor, myDefaultForeground, myDefaultBackground, myFontSize,
                            myTypefaceRegistry, myColorRegistry);
    }
  }

  public class MarkupIterator {
    private int pos;
    private final OutputInfoSerializer.InputStream myOutputInfoStream;

    MarkupIterator() {
      myOutputInfoStream = new OutputInfoSerializer.InputStream(
        myCompressor == null ? myOutputInfosSerialized : myCompressor.decompress(myOutputInfosSerialized));
    }

    public boolean hasNext() {
      return pos < myOutputInfoCount;
    }

    public void processNext(MarkupHandler handler) {
      if (!hasNext()) {
        throw new IllegalStateException();
      }
      pos++;
      try {
        myOutputInfoStream.read(handler);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
