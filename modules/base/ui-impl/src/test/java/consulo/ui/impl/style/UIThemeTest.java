/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.impl.style;

import consulo.ui.color.RGBColor;
import consulo.ui.style.Style;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public class UIThemeTest {
    @Test
    public void testLightThemeLoaded() {
        UITheme theme = UITheme.get(Style.LIGHT_ID);

        assertThat(theme).isNotNull();
        assertThat(theme.getName()).isEqualTo("Light");
        assertThat(theme.isDark()).isFalse();
        assertThat(theme.getIconLibraryId()).isEqualTo("light");
        assertThat(theme.getEditorSchemeId()).isEqualTo("Default");
    }

    @Test
    public void testNestedKeysFlattened() {
        UITheme theme = UITheme.get(Style.LIGHT_ID);

        assertThat(theme.getColor("Component.error.borderColor")).isEqualTo(new RGBColor(235, 184, 188));
    }

    @Test
    public void testNamedColorReference() {
        UITheme theme = UITheme.get(Style.DARK_ID);

        // Panel.background points at the "background" palette entry, not at a literal
        assertThat(theme.getColor("Panel.background")).isEqualTo(new RGBColor(30, 30, 30));
    }

    @Test
    public void testAlphaSurvives() {
        UITheme theme = UITheme.get(Style.DARK_ID);

        RGBColor separator = theme.getColor("Separator.foreground");
        assertThat(separator).isNotNull();
        assertThat(separator.getAlpha()).isEqualTo(0x19);
    }

    @Test
    public void testSemiDarkInheritsFromDark() {
        UITheme theme = UITheme.get(Style.SEMI_DARK);

        assertThat(theme).isNotNull();
        assertThat(theme.isDark()).isTrue();

        // overridden through the palette - the ui key itself is only declared by the parent
        assertThat(theme.getColor("Panel.background")).isEqualTo(new RGBColor(60, 63, 65));
        assertThat(theme.getColor("TextField.background")).isEqualTo(new RGBColor(63, 64, 65));

        // overridden directly
        assertThat(theme.getColor("Component.focusColor")).isEqualTo(new RGBColor(0, 73, 153));

        // inherited untouched
        assertThat(theme.getColor("List.selectionBackground")).isEqualTo(new RGBColor(0, 32, 77));
        assertThat(theme.getColor("Label.foreground")).isEqualTo(new RGBColor(221, 221, 221));
    }

    @Test
    public void testUnknownThemeIsNull() {
        assertThat(UITheme.get("no-such-theme")).isNull();
    }

    @Test
    public void testUnknownKeyIsNull() {
        assertThat(UITheme.get(Style.LIGHT_ID).getColor("No.Such.Key")).isNull();
    }
}
