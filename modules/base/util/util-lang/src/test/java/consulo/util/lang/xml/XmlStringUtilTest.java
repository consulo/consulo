// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.lang.xml;

import consulo.util.lang.internal.Verifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Eugene Zhuravlev
 * @author UNV
 */
public class XmlStringUtilTest {
    @Test
    void testEscapingIllegalXmlChars() {
        for (String s : new String[]{"ab\n\0\r\tde", "\\abc\1\2\3\uFFFFdef"}) {
            String escapedText = XmlStringUtil.escapeIllegalXmlChars(s);
            assertThat(Verifier.checkCharacterData(escapedText)).isNull();
            assertThat(XmlStringUtil.unescapeIllegalXmlChars(escapedText)).isEqualTo(s);
        }
    }

    @Test
    void testXmlWrapInCDATA() {
        assertThat(XmlStringUtil.wrapInCDATA("abc")).isEqualTo("<![CDATA[abc]]>");
        assertThat(XmlStringUtil.wrapInCDATA("abc]]>")).isEqualTo("<![CDATA[abc]]]><![CDATA[]>]]>");
        assertThat(XmlStringUtil.wrapInCDATA("abc]]>def")).isEqualTo("<![CDATA[abc]]]><![CDATA[]>def]]>");
        assertThat(XmlStringUtil.wrapInCDATA("123<![CDATA[wow<&>]]>]]><![CDATA[123"))
            .isEqualTo("<![CDATA[123<![CDATA[wow<&>]]]><![CDATA[]>]]]><![CDATA[]><![CDATA[123]]>");
    }
}
