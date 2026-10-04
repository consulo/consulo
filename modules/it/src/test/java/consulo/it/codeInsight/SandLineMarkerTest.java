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
package consulo.it.codeInsight;

import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.sandboxPlugin.ide.run.SandRunLineMarkerContributor;
import consulo.sandboxPlugin.lang.SandLineMarkerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Gutter markers from {@link SandLineMarkerProvider} and {@link SandRunLineMarkerContributor}, collected by the line
 * marker pass and checked against the markup.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SandLineMarkerTest {
    @Test
    public void everyClassNameGetsAGutterMarker(CodeInsightTestFixture fixture) throws Exception {
        fixture.configureByText("some.sand", """
            class <lineMarker descr="Run sand class Item"><lineMarker descr="Sand class Item">Item</lineMarker></lineMarker> { "body" }
            class <lineMarker descr="Run sand class User"><lineMarker descr="Sand class User">User</lineMarker></lineMarker> : Item {}
            """);

        fixture.checkLineMarkers();
    }
}
