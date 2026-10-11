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
import consulo.it.HeadlessUIThread;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ComboBox;
import consulo.ui.Label;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.model.FlatDataModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ComboBoxSuffixContractTest {
    @Test
    public void aComboBoxHasNoSuffixUntilOneIsSet() {
        HeadlessUIThread.run(() -> {
            ComboBox<String> comboBox = ComboBox.create("first", "second");

            assertThat(comboBox.getSuffixComponent()).isNull();
        });
    }

    @Test
    public void theSuffixIsKeptReplacedAndCleared() {
        HeadlessUIThread.run(() -> {
            ComboBox<String> comboBox = ComboBox.create("first", "second");
            Button manage = Button.create(LocalizeValue.of("Manage"));
            Label other = Label.create(LocalizeValue.of("other"));

            comboBox.setSuffixComponent(manage);
            assertThat(comboBox.getSuffixComponent()).isSameAs(manage);

            comboBox.setSuffixComponent(other);
            assertThat(comboBox.getSuffixComponent()).isSameAs(other);

            comboBox.setSuffixComponent(null);
            assertThat(comboBox.getSuffixComponent()).isNull();
        });
    }

    @Test
    public void theSuffixDoesNotChangeTheValue() {
        HeadlessUIThread.run(() -> {
            ComboBox<String> comboBox = ComboBox.create("first", "second");
            comboBox.setValue("second");

            comboBox.setSuffixComponent(Button.create(LocalizeValue.of("Manage")));

            assertThat(comboBox.getValue()).isEqualTo("second");
        });
    }

    @Test
    public void aComboBoxWithCustomPopupCarriesASuffixToo() {
        HeadlessUIThread.run(() -> {
            ComboBoxWithCustomPopup<String> comboBox = ComboBoxWithCustomPopup.create(FlatDataModel.of(List.of("first")));
            Button manage = Button.create(LocalizeValue.of("Manage"));

            comboBox.setSuffixComponent(manage);

            assertThat(comboBox.getSuffixComponent()).isSameAs(manage);
        });
    }
}
