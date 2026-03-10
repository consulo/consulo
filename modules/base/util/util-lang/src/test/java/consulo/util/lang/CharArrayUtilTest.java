// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.lang;

import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Eugene Zhuravlev
 * @author UNV
 */
public class CharArrayUtilTest {
    @Test
    void testFromSequenceWithoutCopying() {
        String s = "abc.d";
        CharBuffer buffer = CharBuffer.allocate(s.length());
        buffer.append(s);
        buffer.rewind();

        assertThat(CharArrayUtil.fromSequenceWithoutCopying(buffer)).isNotNull();
        assertThat(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(0, 5))).isNotNull();
        assertThat(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(1, 5))).isNull();
        assertThat(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(1, 2))).isNull();
    }
}
