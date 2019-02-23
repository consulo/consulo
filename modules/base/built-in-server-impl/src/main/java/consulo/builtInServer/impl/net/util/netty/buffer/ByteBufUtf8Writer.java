package consulo.builtInServer.impl.net.util.netty.buffer;

import consulo.builtInServer.impl.net.util.netty.NettyKt;
import com.intellij.util.text.CharArrayCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

/**
 * from kotlin platform\platform-impl\src\io\netty\buffer\ByteBufUtf8Writer.kt
 */
public class ByteBufUtf8Writer extends Writer {
  private ByteBuf buffer;

  public ByteBufUtf8Writer(ByteBuf buffer) {
    this.buffer = buffer;
  }

  public void write(InputStream inputStream, int length) throws IOException {
    buffer.writeBytes(inputStream, length);
  }

  public void ensureWritable(int minWritableBytes) {
    buffer.ensureWritable(minWritableBytes);
  }

  @Override
  public void write(char[] chars, int off, int len) {
    NettyKt.writeUtf8(buffer, new CharArrayCharSequence(chars, off, off + len));
  }

  @Override
  public void write(String str) {
    NettyKt.writeUtf8(buffer, str);
  }

  @Override
  public void write(String str, int off, int len) {
    ByteBufUtilEx.writeUtf8(buffer, str, off, off + len);
  }

  @Override
  public Writer append(CharSequence csq) {
    if (csq == null) {
      ByteBufUtil.writeAscii(buffer, "null");
    }
    else {
      NettyKt.writeUtf8(buffer, csq);
    }
    return this;
  }

  @Override
  public Writer append(CharSequence csq, int start, int end) {
    ByteBufUtilEx.writeUtf8(buffer, csq, start, end);
    return this;
  }

  @Override
  public void flush() throws IOException {

  }

  @Override
  public void close() throws IOException {

  }
}
