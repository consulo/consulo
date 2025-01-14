/*
 * Copyright 2013-2025 consulo.io
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
package consulo.test.junit.impl.light;

import consulo.language.file.LanguageFileType;
import consulo.language.plain.PlainTextFileType;
import consulo.test.junit.impl.language.SimpleParsingTest;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

/**
 * @author VISTALL
 * @since 2025-01-14
 */
public class PlainTextParserTest extends SimpleParsingTest<Object> {
    public PlainTextParserTest() {
        super("plain", "log");
    }

    @Test
    public void testFirstTest(Context context) throws Exception {
        doTest(context, null);
    }

    @Nonnull
    @Override
    protected LanguageFileType getFileType(@Nonnull Context context, @Nullable Object testContext) {
        return PlainTextFileType.INSTANCE;
    }
}
