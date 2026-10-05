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
package consulo.it.execution;

import consulo.execution.internal.ExecutionActionValue;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.util.TextWithMnemonic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecutionActionValueTest {
    private static final String CONFIGURATION_NAME = "ThreadProbe";
    private static final String MARKED_CONFIGURATION_NAME = "my_app & co";

    @Test
    public void textWithoutMnemonicShowsTheConfigurationName() {
        LocalizeValue value = ExecutionActionValue.buildWithConfiguration(ExecutionActionValueTest::profileText, CONFIGURATION_NAME);

        TextWithMnemonic text = mnemonic(value);
        assertEquals("Profile 'ThreadProbe' with 'Java Flight Recorder'", text.getText());
        assertFalse(text.hasMnemonic());
        assertEquals(text.getText(), value.getValue());
    }

    @Test
    public void textWithoutMnemonicInsertsMarkedNameLiterally() {
        LocalizeValue value = ExecutionActionValue.buildWithConfiguration(ExecutionActionValueTest::profileText, MARKED_CONFIGURATION_NAME);

        TextWithMnemonic text = mnemonic(value);
        assertEquals("Profile 'my_app & co' with 'Java Flight Recorder'", text.getText());
        assertFalse(text.hasMnemonic());
    }

    @Test
    public void mnemonicBeforeTheNameIsKept() {
        TextWithMnemonic text = mnemonic(ExecutionActionValue.buildWithConfiguration(ExecutionActionValueTest::runText, CONFIGURATION_NAME));

        assertEquals("Run ThreadProbe", text.getText());
        assertEquals(1, text.getMnemonicIndex());
        assertEquals('U', text.getMnemonic());
    }

    @Test
    public void mnemonicBeforeTheNameKeepsMarkedNameLiteral() {
        TextWithMnemonic text = mnemonic(ExecutionActionValue.buildWithConfiguration(ExecutionActionValueTest::runText, MARKED_CONFIGURATION_NAME));

        assertEquals("Run my_app & co", text.getText());
        assertEquals(1, text.getMnemonicIndex());
    }

    @Test
    public void coverageMnemonicAfterTheNameIsKept() {
        TextWithMnemonic text = mnemonic(ExecutionActionValue.buildWithConfiguration(ExecutionActionValueTest::coverageText, CONFIGURATION_NAME));

        assertEquals("Run 'ThreadProbe' with Coverage", text.getText());
        assertEquals(text.getText().indexOf("verage"), text.getMnemonicIndex());
        assertEquals('V', text.getMnemonic());
    }

    @Test
    public void coverageMnemonicAfterTheNameIgnoresMarkersInTheName() {
        TextWithMnemonic text = mnemonic(ExecutionActionValue.buildWithConfiguration(ExecutionActionValueTest::coverageText, MARKED_CONFIGURATION_NAME));

        assertEquals("Run 'my_app & co' with Coverage", text.getText());
        assertEquals(text.getText().indexOf("verage"), text.getMnemonicIndex());
        assertEquals('V', text.getMnemonic());
    }

    @Test
    public void ampersandCoverageMnemonicAfterTheNameIsKept() {
        TextWithMnemonic text = mnemonic(ExecutionActionValue.buildWithConfiguration(ExecutionActionValueTest::ampersandCoverageText, CONFIGURATION_NAME));

        assertEquals("Run 'ThreadProbe' with Coverage", text.getText());
        assertEquals(text.getText().indexOf("Coverage"), text.getMnemonicIndex());
        assertEquals('C', text.getMnemonic());
    }

    @Test
    public void ampersandCoverageMnemonicAfterTheNameIgnoresMarkersInTheName() {
        LocalizeValue value = ExecutionActionValue.buildWithConfiguration(ExecutionActionValueTest::ampersandCoverageText, MARKED_CONFIGURATION_NAME);

        TextWithMnemonic text = mnemonic(value);
        assertEquals("Run 'my_app & co' with Coverage", text.getText());
        assertTrue(text.hasMnemonic());
        assertEquals(text.getText().indexOf("Coverage"), text.getMnemonicIndex());
        assertEquals('C', text.getMnemonic());
    }

    private static LocalizeValue profileText(String configurationName) {
        return LocalizeValue.of("Profile '" + configurationName + "' with 'Java Flight Recorder'");
    }

    private static LocalizeValue runText(String configurationName) {
        return LocalizeValue.of("R&un " + configurationName);
    }

    private static LocalizeValue coverageText(String configurationName) {
        return LocalizeValue.of("Run '" + configurationName + "' with Co_verage");
    }

    private static LocalizeValue ampersandCoverageText(String configurationName) {
        return LocalizeValue.of("Run '" + configurationName + "' with &Coverage");
    }

    private static TextWithMnemonic mnemonic(LocalizeValue value) {
        return assertInstanceOf(LocalizeValueWithMnemonic.class, value).mnemonic();
    }
}
