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

import consulo.index.io.data.CompactDataInput;
import consulo.index.io.data.CompactDataOutput;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Not synchronized, stream implementations must be used from one thread at a time only
 */
public class OutputInfoSerializer {
  private static final int TEXT_ID = 0;
  private static final int STYLE_ID = 1;
  private static final int FOREGROUND_ID = 2;
  private static final int BACKGROUND_ID = 3;
  private static final int FONT_ID = 4;

  public static class OutputStream implements MarkupHandler {
    private final CompactDataOutput myOutputStream;
    private int myCurrentOffset;

    public OutputStream(java.io.OutputStream stream) {
      myOutputStream = new CompactDataOutput(stream);
    }

    @Override
    public void handleText(int startOffset, int endOffset) throws IOException {
      myOutputStream.write(TEXT_ID);
      myOutputStream.writeInt(startOffset - myCurrentOffset);
      myOutputStream.writeInt(endOffset - startOffset);
      myCurrentOffset = endOffset;
    }

    @Override
    public void handleForeground(int foregroundId) throws IOException {
      myOutputStream.write(FOREGROUND_ID);
      myOutputStream.writeInt(foregroundId);
    }

    @Override
    public void handleBackground(int backgroundId) throws IOException {
      myOutputStream.write(BACKGROUND_ID);
      myOutputStream.writeInt(backgroundId);
    }

    @Override
    public void handleFont(int fontNameId) throws IOException {
      myOutputStream.write(FONT_ID);
      myOutputStream.writeInt(fontNameId);
    }

    @Override
    public void handleStyle(int style) throws IOException {
      myOutputStream.write(STYLE_ID);
      myOutputStream.writeInt(style);
    }

    @Override
    public boolean canHandleMore() {
      return true;
    }
  }

  public static class InputStream {
    private final CompactDataInput myInputStream;
    private int myCurrentOffset;

    public InputStream(byte[] stream) {
      myInputStream = new CompactDataInput(new ByteArrayInputStream(stream));
    }

    public void read(MarkupHandler handler) throws Exception {
      int id = myInputStream.readByte();
      switch (id) {
        case TEXT_ID:
          int startOffset = myCurrentOffset + myInputStream.readInt();
          myCurrentOffset = startOffset;
          int endOffset = myCurrentOffset + myInputStream.readInt();
          myCurrentOffset = endOffset;
          handler.handleText(startOffset, endOffset);
          break;
        case STYLE_ID:
          handler.handleStyle(myInputStream.readInt());
          break;
        case FOREGROUND_ID:
          handler.handleForeground(myInputStream.readInt());
          break;
        case BACKGROUND_ID:
          handler.handleBackground(myInputStream.readInt());
          break;
        case FONT_ID:
          handler.handleFont(myInputStream.readInt());
          break;
        default:
          throw new IllegalStateException("Unknown tag id: " + id);
      }
    }
  }
}
