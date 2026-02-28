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
package consulo.test.junit.impl.light;

import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.test.junit.impl.extension.ConsuloApplicationLoader;
import consulo.test.junit.impl.extension.NoParamDisplayNameGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author VISTALL
 * @since 2026-02-28
 */
@DisplayNameGeneration(NoParamDisplayNameGenerator.class)
@ExtendWith(ConsuloApplicationLoader.class)
public class LocalizationTest {
    @Test
    public void testLocalizeNoParam() {
        LocalizeValue value = CommonLocalize.actionHelp();

        Assertions.assertEquals(value.get(), "[consulo.platform.base.CommonLocalize@action.help]");
    }

    @Test
    public void testParam1() {
        LocalizeValue value = CommonLocalize.formatFileSizeBytes(1);

        Assertions.assertEquals(value.get(), "[consulo.platform.base.CommonLocalize@format.file.size.bytes](1)");
    }

    @Test
    public void testParam2() {
        LocalizeValue value = CommonLocalize.labelOldWayJvmPropertyUsed(1, 2);

        Assertions.assertEquals(value.get(), "[consulo.platform.base.CommonLocalize@label.old.way.jvm.property.used](1, 2)");
    }
}
