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
package consulo.it.ui;

import consulo.it.HeadlessApplicationExtension;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.platform.base.localize.CommonLocalize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The text of a key is readable in a locale of the caller's choosing, so a search can match the default locale text
 * of an option next to the text the current locale shows. Unlike the lookup of the current locale it never falls back:
 * a locale without a text for the key answers nothing.
 * <p>
 * The headless application loads no localize library, which is what lets the difference show here: the current
 * lookup falls back to the id of the key, the lookup by locale does not.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class LocalizeLookupContractTest {
    @Test
    public void theDefaultLocaleIsTheOneEveryOtherFallsBackTo() {
        LocalizeManager manager = LocalizeManager.get();
        LocalizeKey key = CommonLocalize.titleSettings().getKey().orElseThrow();

        assertThat(manager.getDefaultLocale()).isEqualTo(Locale.US);
        assertThat(manager.getUnformattedText(key).getKey()).isEqualTo(manager.getDefaultLocale());
    }

    @Test
    public void aLocaleWithoutTheTextAnswersNothingWhereTheCurrentLookupFallsBack() {
        LocalizeManager manager = LocalizeManager.get();
        LocalizeKey key = CommonLocalize.buttonCancel().getKey().orElseThrow();

        Map.Entry<Locale, String> current = manager.getUnformattedText(key);

        assertThat(current.getValue()).isEqualTo(key.toString());
        assertThat(manager.getUnformattedText(key, manager.getDefaultLocale())).isNull();
    }

    @Test
    public void aLocaleNobodyTranslatedToAnswersNothing() {
        LocalizeKey key = CommonLocalize.titleSettings().getKey().orElseThrow();

        assertThat(LocalizeManager.get().getUnformattedText(key, Locale.of("xx", "YY"))).isNull();
    }

    @Test
    public void anUnknownKeyAnswersNothing() {
        LocalizeKey key = LocalizeKey.of("consulo.platform.base.CommonLocalize", "no.such.key.anywhere");

        assertThat(LocalizeManager.get().getUnformattedText(key, LocalizeManager.get().getDefaultLocale())).isNull();
    }

    @Test
    public void anEmptyKeyIsAnEmptyTextInEveryLocale() {
        LocalizeKey key = LocalizeKey.of("consulo.platform.base.CommonLocalize", "");

        assertThat(LocalizeManager.get().getUnformattedText(key, Locale.of("xx", "YY"))).isEmpty();
    }
}
