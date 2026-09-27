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

import consulo.application.Application;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.it.UseInspection;
import consulo.sandboxPlugin.lang.inspection.SandLocalInspection;
import consulo.sandboxPlugin.lang.inspection.SandLocalInspectionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A local inspection enabled on its own, with the expected problems marked up in the source and the daemon's
 * highlighting checked against the markup.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SandLocalInspectionTest {
    @Test
    @UseInspection(SandLocalInspection.class)
    public void reportsTheClassWhileTheCheckIsOn(CodeInsightTestFixture fixture) throws Exception {
        fixture.<SandLocalInspectionState>getInspectionState(SandLocalInspection.class).setCheckClass(true);

        fixture.configureByText("some.sand", "class <error descr=\"Test Error\">Item</error> { \"body\" }\n");

        fixture.checkHighlighting();
    }

    @Test
    @UseInspection(SandLocalInspection.class)
    public void reportsNothingWhileTheCheckIsOff(CodeInsightTestFixture fixture) throws Exception {
        fixture.configureByText("some.sand", "class Item { \"body\" }\n");

        fixture.checkHighlighting();
    }

    @Test
    public void settingsStayWithTheirProject(Application application, HeadlessProjects projects, CodeInsightTestFixture fixture)
        throws Exception {
        fixture.<SandLocalInspectionState>getInspectionState(SandLocalInspection.class).setCheckClass(true);

        CodeInsightTestFixture other = CodeInsightTestFixture.create(application, projects);

        assertThat(other.<SandLocalInspectionState>getInspectionState(SandLocalInspection.class).isCheckClass())
            .as("a setting changed in one project's profile must not show up in the next project")
            .isFalse();
    }
}
