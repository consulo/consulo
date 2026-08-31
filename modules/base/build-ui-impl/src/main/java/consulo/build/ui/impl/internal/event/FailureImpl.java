/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.event.Failure;
import consulo.build.ui.event.FailureResult;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.ui.notification.Notification;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class FailureImpl implements Failure {
    private static class MyBuilder implements Builder {
        private LocalizeValue myMessage = LocalizeValue.empty();
        private LocalizeValue myDescription = LocalizeValue.empty();
        private @Nullable List<Failure> myCauses = null;
        private @Nullable Throwable myError = null;
        private @Nullable Notification myNotification = null;
        private @Nullable Navigatable myNavigatable = null;

        @Override
        public Builder message(LocalizeValue message) {
            myMessage = message;
            return this;
        }

        @Override
        public Builder description(LocalizeValue description) {
            myDescription = description;
            return this;
        }

        @Override
        public Builder cause(Failure cause) {
            causes(List.of(cause));
            return this;
        }

        @Override
        public Builder causes(Collection<? extends Failure> causes) {
            if (myCauses == null) {
                myCauses = new ArrayList<>(causes);
            }
            else {
                myCauses.addAll(causes);
            }
            return this;
        }

        @Override
        public Builder error(Throwable error) {
            myError = error;
            return this;
        }

        @Override
        public Builder notification(Notification notification) {
            myNotification = notification;
            return this;
        }

        @Override
        public Builder navigatable(Navigatable navigatable) {
            myNavigatable = navigatable;
            return this;
        }

        private boolean isEmpty() {
            return myMessage.isEmpty() && myError == null;
        }

        @Override
        public Failure create() {
            if (isEmpty()) {
                throw new IllegalStateException("Failure must have at least message or error filled");
            }
            List<Failure> causes = myCauses == null ? List.of() : myCauses;
            return new FailureImpl(myMessage, myDescription, causes, myError, myNotification, myNavigatable);
        }

        @Override
        public FailureResult createResult() {
            return new FailureResultImpl(isEmpty() ? List.of() : List.of(create()));
        }
    }

    private final LocalizeValue myMessage;
    private final LocalizeValue myDescription;
    private final List<? extends Failure> myCauses;
    private final @Nullable Throwable myError;
    private final @Nullable Notification myNotification;
    private final @Nullable Navigatable myNavigatable;

    private FailureImpl(
        LocalizeValue message,
        LocalizeValue description,
        List<? extends Failure> causes,
        @Nullable Throwable error,
        @Nullable Notification notification,
        @Nullable Navigatable navigatable
    ) {
        myMessage = message;
        myDescription = description;
        myCauses = causes;
        myError = error;
        myNotification = notification;
        myNavigatable = navigatable;
    }

    @Override
    public LocalizeValue getMessage() {
        return myMessage;
    }

    @Override
    public LocalizeValue getDescription() {
        return myDescription;
    }

    @Override
    public @Nullable Throwable getError() {
        return myError;
    }

    @Override
    public List<? extends Failure> getCauses() {
        return myCauses;
    }

    @Override
    public @Nullable Notification getNotification() {
        return myNotification;
    }

    @Override
    public @Nullable Navigatable getNavigatable() {
        return myNavigatable;
    }

    public static Builder builder() {
        return new MyBuilder();
    }
}
