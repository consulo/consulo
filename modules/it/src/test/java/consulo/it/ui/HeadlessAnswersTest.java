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

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.HeadlessAnswers;
import consulo.it.HeadlessDialog;
import consulo.it.HeadlessUIThread;
import consulo.it.internal.HeadlessLoggerFactory;
import consulo.localize.LocalizeValue;
import consulo.ui.ColorPickerBuilder;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.MessageButtonRole;
import consulo.ui.Window;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author VISTALL
 * @since 2026-11-10
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class HeadlessAnswersTest {
    @Test
    public void messageBoxIsAnsweredByItsId() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            HeadlessAnswers answers = HeadlessAnswers.of(scope).messageBox("demo.question", MessageButtonRole.NO);

            Boolean answer = MessageBoxes.yesNo().id("demo.question").text(LocalizeValue.of("go?")).showAsync().get();

            assertThat(answer).isFalse();
            assertThat(answers.shown()).containsExactly("demo.question");
            answers.assertAllAnswered();
        }
        finally {
            Disposer.dispose(scope);
        }
    }

    @Test
    public void inputBoxAndColorPickerAreAnsweredByTheirId() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ColorValue red = new RGBColor(255, 0, 0);
            HeadlessAnswers answers = HeadlessAnswers.of(scope)
                .inputBox("demo.name", "  Item  ")
                .colorPicker("demo.color", red);

            String name = InputBoxBuilder.text().id("demo.name").text(LocalizeValue.of("name?")).showAsync().get();
            ColorValue color = HeadlessUIThread.compute(() -> ColorPickerBuilder.create().id("demo.color").showAsync((Window)null)).get();

            assertThat(name).isEqualTo("Item");
            assertThat(color).isEqualTo(red);
            assertThat(answers.shown()).containsExactly("demo.name", "demo.color");
            answers.assertAllAnswered();
        }
        finally {
            Disposer.dispose(scope);
        }
    }

    @Test
    public void dialogWithoutAnswersIsCancelled() {
        CompletableFuture<DialogValue> result = show(new SubDescriptor());

        assertThatThrownBy(() -> result.get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
        assertThat(HeadlessLoggerFactory.takeLoggedErrors())
            .anyMatch(error -> error.getMessage().contains("dialog shown while no answers are installed"));
    }

    @Test
    public void answerThatWasNeverUsedFails() {
        Disposable scope = Disposable.newDisposable();
        try {
            HeadlessAnswers answers = HeadlessAnswers.of(scope)
                .messageBox("never.asked", MessageButtonRole.YES)
                .dialog("never.shown", HeadlessDialog::pressOk);

            assertThatThrownBy(answers::assertAllAnswered)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("message box was never shown: never.asked")
                .hasMessageContaining("dialog was never shown: never.shown");
        }
        finally {
            Disposer.dispose(scope);
        }
    }

    @Test
    public void dialogIsAnsweredByItsDescriptorType() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            HeadlessAnswers answers = HeadlessAnswers.of(scope).dialog(BaseDescriptor.class, HeadlessDialog::pressOk);

            CompletableFuture<DialogValue> result = show(new SubDescriptor());

            assertThat(result.get(10, TimeUnit.SECONDS)).isNotNull();
            assertThat(answers.shown()).containsExactly(SubDescriptor.class.getName());
            answers.assertAllAnswered();
        }
        finally {
            Disposer.dispose(scope);
        }
    }

    @Test
    public void dialogIdWinsOverItsType() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            HeadlessAnswers answers = HeadlessAnswers.of(scope)
                .dialog("demo.dialog", HeadlessDialog::pressCancel)
                .dialog(BaseDescriptor.class, HeadlessDialog::pressOk);
            SubDescriptor descriptor = new SubDescriptor();
            descriptor.setId("demo.dialog");

            CompletableFuture<DialogValue> result = show(descriptor);

            assertThatThrownBy(() -> result.get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
            assertThatThrownBy(answers::assertAllAnswered)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("dialog was never shown: " + BaseDescriptor.class.getName());
        }
        finally {
            Disposer.dispose(scope);
        }
    }

    @Test
    public void secondInstallFails() {
        Disposable scope = Disposable.newDisposable();
        try {
            HeadlessAnswers.of(scope);

            assertThatThrownBy(() -> HeadlessAnswers.of(scope)).isInstanceOf(IllegalStateException.class);
        }
        finally {
            Disposer.dispose(scope);
        }
    }

    private static CompletableFuture<DialogValue> show(DialogDescriptor descriptor) {
        return HeadlessUIThread.compute(() -> Application.get().getInstance(DialogService.class).build(descriptor).showAsync());
    }

    private static class BaseDescriptor extends DialogDescriptor {
        BaseDescriptor() {
            super(LocalizeValue.of("demo"));
        }

        @Override
        public Component createCenterComponent(Disposable uiDisposable) {
            return Label.create(LocalizeValue.of("demo"));
        }
    }

    private static class SubDescriptor extends BaseDescriptor {
    }
}
