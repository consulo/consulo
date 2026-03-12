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

    public static final String ESCAPE_TEST_STRING = "<foo>\r\n\t\u00A0'\"&";

    @Test
    void testEscapeAttr() {
        String quotNoEscapeValue = "<foo>'";
        assertThat(XmlStringUtil.escapeAttr(quotNoEscapeValue, '"')).isSameAs(quotNoEscapeValue);

        String aposNoEscapeValue = "<foo>\"";
        assertThat(XmlStringUtil.escapeAttr(aposNoEscapeValue, '\'')).isSameAs(aposNoEscapeValue);

        assertThat(XmlStringUtil.escapeAttr(ESCAPE_TEST_STRING, '"'))
            .isEqualTo(XmlStringUtil.escapeAttr(ESCAPE_TEST_STRING, '"', sb()).toString())
            .isEqualTo("<foo>&#13;&#10;\t\u00A0'&quot;&amp;");

        assertThat(XmlStringUtil.escapeAttr(ESCAPE_TEST_STRING, '\''))
            .isEqualTo(XmlStringUtil.escapeAttr(ESCAPE_TEST_STRING, '\'', sb()).toString())
            .isEqualTo("<foo>&#13;&#10;\t\u00A0&apos;\"&amp;");
    }

    @Test
    void testEscapeIllegalXmlChars() {
        for (String s : new String[]{"ab\n\0\r\tde", "\\abc\1\2\3\uFFFFdef"}) {
            String escapedText = XmlStringUtil.escapeIllegalXmlChars(s);
            assertThat(Verifier.checkCharacterData(escapedText)).isNull();
            assertThat(XmlStringUtil.unescapeIllegalXmlChars(escapedText)).isEqualTo(s);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void testEscapeString() {
        assertThat(XmlStringUtil.escapeString(null)).isNull();

        String noEscapeValue = "foobar";
        assertThat(XmlStringUtil.escapeString(noEscapeValue)).isSameAs(noEscapeValue);

        assertThat(XmlStringUtil.escapeString(ESCAPE_TEST_STRING))
            .isEqualTo("&lt;foo&gt;\r\n\t&nbsp;'&quot;&amp;");

        assertThat(XmlStringUtil.escapeString(ESCAPE_TEST_STRING, true))
            .isEqualTo("&lt;foo&gt;&#13;&#10;&#9;&nbsp;'&quot;&amp;");

        assertThat(XmlStringUtil.escapeString(ESCAPE_TEST_STRING, true, false))
            .isEqualTo("&lt;foo&gt;&#13;&#10;&#9;\u00A0'&quot;&amp;");
    }

    @Test
    void testEscapeText() {
        String noEscapeValue = "\r\n\t\u00A0'\"";
        assertThat(XmlStringUtil.escapeText(noEscapeValue)).isSameAs(noEscapeValue);

        assertThat(XmlStringUtil.escapeText(ESCAPE_TEST_STRING))
            .isEqualTo(XmlStringUtil.escapeText(ESCAPE_TEST_STRING, sb()).toString())
            .isEqualTo("&lt;foo&gt;\r\n\t\u00A0'\"&amp;");
    }

    @Test
    void testWrapInCDATA() {
        assertThat(XmlStringUtil.wrapInCDATA("abc")).isEqualTo("<![CDATA[abc]]>");
        assertThat(XmlStringUtil.wrapInCDATA("abc]]>")).isEqualTo("<![CDATA[abc]]]><![CDATA[]>]]>");
        assertThat(XmlStringUtil.wrapInCDATA("abc]]>def")).isEqualTo("<![CDATA[abc]]]><![CDATA[]>def]]>");
        assertThat(XmlStringUtil.wrapInCDATA("123<![CDATA[wow<&>]]>]]><![CDATA[123"))
            .isEqualTo("<![CDATA[123<![CDATA[wow<&>]]]><![CDATA[]>]]]><![CDATA[]><![CDATA[123]]>");
    }

    private static StringBuilder sb() {
        return new StringBuilder();
    }
}
