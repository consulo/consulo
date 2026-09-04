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

import consulo.disposer.Disposable;
import consulo.it.HeadlessApplicationExtension;
import consulo.localize.LocalizeValue;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputValidators;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageBoxRemember;
import consulo.ui.MessageBoxes;
import consulo.ui.MessageButtonRole;
import consulo.ui.RememberScope;
import consulo.ui.impl.ScriptedDialogs;
import consulo.util.lang.ThreeState;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import consulo.application.Application;
import consulo.ui.impl.MessagePresentation;
import consulo.ui.DialogCancelledException;

@ExtendWith(HeadlessApplicationExtension.class)
public class MessageBoxSeamTest {
    @Test
    public void yesNoAnswersTheValueItsButtonCarries() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expect(MessageButtonRole.YES);

            assertTrue(MessageBoxes.yesNo().text(LocalizeValue.of("go?")).showAsync().get());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void yesNoCancelAnswersThreeState() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expect(MessageButtonRole.CANCEL);

            assertEquals(ThreeState.UNSURE, MessageBoxes.yesNoCancel().text(LocalizeValue.of("go?")).showAsync().get());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void aButtonMayAnswerNullWithoutThatReadingAsDismissal() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expect(MessageButtonRole.YES);

            MessageBoxBuilder<@Nullable String> box = MessageBoxBuilder.create();
            box.text(LocalizeValue.of("pick"));
            box.button(MessageButtonRole.YES, (String)null);
            box.button(MessageButtonRole.NO, "dismissed");
            box.asExitButton();

            // the YES button was pressed, so its own null is the answer - not the exit value
            assertNull(box.showAsync().get());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void anUnscriptedBoxFailsRatherThanHanging() {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL);

            CompletableFuture<Boolean> result = MessageBoxes.yesNo().text(LocalizeValue.of("go?")).showAsync();

            assertTrue(result.isDone());
            assertThrows(ExecutionException.class, result::get);
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void aRememberedAnswerNeverOpensABox() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs dialogs = ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL);

            MessageBoxBuilder<Boolean> box = MessageBoxBuilder.create();
            box.text(LocalizeValue.of("go?"));
            box.button(MessageButtonRole.YES, Boolean.TRUE);
            box.button(MessageButtonRole.NO, Boolean.FALSE);
            box.asExitButton();
            box.remember(new StoredRemember(Boolean.TRUE));

            assertTrue(box.showAsync().get());
            assertTrue(dialogs.seen().isEmpty());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void onAcceptDoesNotRememberARejectingAnswer() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expect(MessageButtonRole.NO);

            StoredRemember remember = new StoredRemember(null, RememberScope.ON_ACCEPT);

            MessageBoxBuilder<Boolean> box = MessageBoxBuilder.create();
            box.text(LocalizeValue.of("go?"));
            box.button(MessageButtonRole.YES, Boolean.TRUE);
            box.button(MessageButtonRole.NO, Boolean.FALSE);
            box.asExitButton();
            box.remember(remember);

            assertFalse(box.showAsync().get());
            assertNull(remember.getValue());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void onAnswerRemembersEitherButton() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expect(MessageButtonRole.NO);

            StoredRemember remember = new StoredRemember(null, RememberScope.ON_ANSWER);

            MessageBoxBuilder<Boolean> box = MessageBoxBuilder.create();
            box.text(LocalizeValue.of("go?"));
            box.button(MessageButtonRole.YES, Boolean.TRUE);
            box.button(MessageButtonRole.NO, Boolean.FALSE);
            box.asExitButton();
            box.remember(remember);

            assertFalse(box.showAsync().get());
            assertEquals(Boolean.FALSE, remember.getValue());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void anInputAnswerIsTrimmed() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expectInput("  spaced  ");

            assertEquals("spaced", InputBoxBuilder.text().text(LocalizeValue.of("name?")).showAsync().get());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void aDismissedInputFailsWithItsOwnType() {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expectInput(null);

            CompletableFuture<String> result = InputBoxBuilder.text().text(LocalizeValue.of("name?")).showAsync();

            ExecutionException thrown = assertThrows(ExecutionException.class, result::get);
            assertInstanceOf(DialogCancelledException.class, thrown.getCause());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void anInputThatCannotBeShownFailsRatherThanReadingAsCancelled() {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL);

            CompletableFuture<String> result = InputBoxBuilder.text().text(LocalizeValue.of("name?")).showAsync();

            // the two outcomes stay apart by type: this one is not a dismissal
            ExecutionException thrown = assertThrows(ExecutionException.class, result::get);
            assertFalse(thrown.getCause() instanceof DialogCancelledException);
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void anEmptyConfirmedValueIsNotACancel() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expectInput("");

            // "" is a confirmed empty string, and a confirmed value is never a dismissal
            assertEquals("", InputBoxBuilder.text().text(LocalizeValue.of("name?")).showAsync().get());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void nonEmptyRejectsBlankAndAcceptsText() {
        assertNull(InputValidators.nonEmpty(LocalizeValue.of("required")).validate("ok"));
        assertTrue(InputValidators.nonEmpty(LocalizeValue.of("required")).validate("   ").blocksConfirm());
    }

    @Test
    public void anIntegerInputAnswersAnInteger() throws Exception {
        Disposable scope = Disposable.newDisposable();
        try {
            ScriptedDialogs.install(scope, ScriptedDialogs.Unexpected.FAIL).expectInput(7);

            assertEquals(7, InputBoxBuilder.integer().text(LocalizeValue.of("how many?")).showAsync().get());
        }
        finally {
            scope.dispose();
        }
    }

    @Test
    public void anUnsetTitleFallsBackToTheApplicationName() {
        assertEquals(Application.get().getName(), MessagePresentation.title(LocalizeValue.empty()));
    }

    @Test
    public void titleResolutionNeverThrows() {
        // an early startup failure is shown before there is an application, and must still open
        assertNotNull(MessagePresentation.title(LocalizeValue.empty()));
    }

    @Test
    public void aSetTitleIsKept() {
        LocalizeValue own = LocalizeValue.of("Open Project");
        assertEquals(own, MessagePresentation.title(own));
    }

    private static class StoredRemember implements MessageBoxRemember<Boolean> {
        private @Nullable Boolean myValue;
        private final RememberScope myScope;

        StoredRemember(@Nullable Boolean value) {
            this(value, RememberScope.ON_ANSWER);
        }

        StoredRemember(@Nullable Boolean value, RememberScope scope) {
            myValue = value;
            myScope = scope;
        }

        @Override
        public void setValue(Boolean value) {
            myValue = value;
        }

        @Override
        public @Nullable Boolean getValue() {
            return myValue;
        }

        @Override
        public LocalizeValue getMessageText() {
            return LocalizeValue.of("do not ask again");
        }

        @Override
        public boolean isRememberByDefault() {
            return true;
        }

        @Override
        public RememberScope getScope() {
            return myScope;
        }
    }
}
