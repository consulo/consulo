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
package consulo.it.language;

import consulo.component.extension.ExtensionWalker;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.plain.PlainTextLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LanguageOneToMany#buildOrAny()}: the extensions of the language first, then the {@link Language#ANY} ones.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class LanguageOneToManyTest {
    private record TestExtension(String name, Language language) implements LanguageExtension {
        @Override
        public Language getLanguage() {
            return language;
        }
    }

    private static final TestExtension ANY_FIRST = new TestExtension("anyFirst", Language.ANY);
    private static final TestExtension PLAIN = new TestExtension("plain", PlainTextLanguage.INSTANCE);
    private static final TestExtension ANY_SECOND = new TestExtension("anySecond", Language.ANY);
    private static final TestExtension PLAIN_SECOND = new TestExtension("plainSecond", PlainTextLanguage.INSTANCE);

    @Test
    public void buildOrAnyPutsAnyLanguageExtensionsLast() {
        ByLanguageValue<List<TestExtension>> value = LanguageOneToMany.<TestExtension>buildOrAny().apply(walker());

        assertThat(value.requiredGet(PlainTextLanguage.INSTANCE)).containsExactly(PLAIN, PLAIN_SECOND, ANY_FIRST, ANY_SECOND);
        assertThat(value.requiredGet(Language.ANY)).containsExactly(ANY_FIRST, ANY_SECOND);
    }

    @Test
    public void buildWithAnyKeepsTheRegistrationOrder() {
        ByLanguageValue<List<TestExtension>> value = LanguageOneToMany.<TestExtension>build(true).apply(walker());

        assertThat(value.requiredGet(PlainTextLanguage.INSTANCE)).containsExactly(ANY_FIRST, PLAIN, ANY_SECOND, PLAIN_SECOND);
    }

    @Test
    public void buildWithoutAnyHasOnlyTheLanguageExtensions() {
        ByLanguageValue<List<TestExtension>> value = LanguageOneToMany.<TestExtension>build(false).apply(walker());

        assertThat(value.requiredGet(PlainTextLanguage.INSTANCE)).containsExactly(PLAIN, PLAIN_SECOND);
    }

    private static ExtensionWalker<TestExtension> walker() {
        return new ExtensionWalker<>() {
            @Override
            public void walk(Consumer<TestExtension> consumer) {
                for (TestExtension extension : List.of(ANY_FIRST, PLAIN, ANY_SECOND, PLAIN_SECOND)) {
                    consumer.accept(extension);
                }
            }

            @Override
            public Function<List<TestExtension>, List<TestExtension>> sorter() {
                return Function.identity();
            }
        };
    }
}
