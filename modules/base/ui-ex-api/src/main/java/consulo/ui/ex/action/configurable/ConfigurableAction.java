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
package consulo.ui.ex.action.configurable;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author UNV
 * @since 2025-07-14
 */
public abstract class ConfigurableAction extends AnAction {
    protected interface ValidDataBuilder<T> {
        <R> ValidDataBuilder<R> map(@Nonnull Function<T, R> mapper);
        ValidData<T> disabledIfAbsent();
        ValidData<T> invisibleAndDisabledIfAbsent();
    }

    protected class Builder {
        protected abstract class AbstractValidDataBuilder<T> implements ValidDataBuilder<T> {
            @Override
            public <R> ValidDataBuilder<R> map(@Nonnull Function<T, R> mapper) {
                return new MappedValidDataBuilder<>(this, mapper);
            }

            @Override
            public final ValidData<T> disabledIfAbsent() {
                checkClosed();
                ValidData<T> data = create(myDisabledIfAbsent);
                myDisabledIfAbsent = data;
                myUpdatable = true;
                return data;
            }

            @Override
            public final ValidData<T> invisibleAndDisabledIfAbsent() {
                checkClosed();
                ValidData<T> data = create(myInvisibleAndDisabledIfAbsent);
                myInvisibleAndDisabledIfAbsent = data;
                myUpdatable = true;
                return data;
            }

            protected abstract ValidData<T> create(ValidData<?> next);
        }

        protected class SimpleValidDataBuilder<T> extends AbstractValidDataBuilder<T> {
            @Nonnull
            private final Key<T> myDataKey;

            public SimpleValidDataBuilder(@Nonnull Key<T> dataKey) {
                myDataKey = dataKey;
            }

            @Override
            protected ValidData<T> create(ValidData<?> next) {
                return new SimpleValidData<>(myDataKey, next);
            }
        }

        protected class MappedValidDataBuilder<T, R> extends AbstractValidDataBuilder<R> {
            @Nonnull
            private final AbstractValidDataBuilder<T> mySubBuilder;
            @Nonnull
            private final Function<T, R> myMapper;

            public MappedValidDataBuilder(@Nonnull AbstractValidDataBuilder<T> subBuilder, @Nonnull Function<T, R> mapper) {
                mySubBuilder = subBuilder;
                myMapper = mapper;
            }

            @Override
            protected ValidData<R> create(ValidData<?> next) {
                return new MappedValidData<>(mySubBuilder.create(next), myMapper);
            }
        }

        protected class CheckedValidDataBuilder<T> extends AbstractValidDataBuilder<T> {
            @Nonnull
            private final AbstractValidDataBuilder<T> mySubBuilder;
            @Nonnull
            private final Predicate<T> myValidator;

            public CheckedValidDataBuilder(@Nonnull AbstractValidDataBuilder<T> subBuilder, @Nonnull Predicate<T> validator) {
                mySubBuilder = subBuilder;
                myValidator = validator;
            }

            @Override
            protected ValidData<T> create(ValidData<?> next) {
                return new CheckedValidData<>(mySubBuilder.create(next), myValidator);
            }
        }

        private boolean myClosed = false;
        private final Presentation myTemplatePresentation;

        public Builder(Presentation templatePresentation) {
            this.myTemplatePresentation = templatePresentation;
        }

        public Builder text(@Nonnull LocalizeValue text) {
            checkClosed();
            myTemplatePresentation.setTextValue(text);
            return this;
        }

        public Builder description(@Nonnull LocalizeValue description) {
            checkClosed();
            myTemplatePresentation.setDescriptionValue(description);
            return this;
        }

        public Builder icon(@Nonnull Image icon) {
            checkClosed();
            myTemplatePresentation.setIcon(icon);
            return this;
        }

        public <T> ValidDataBuilder<T> newValidData(@Nonnull Key<T> dataKey) {
            return new SimpleValidDataBuilder<>(dataKey);
        }

        public <T> ValidData<T> disabledIfAbsent(@Nonnull Key<T> dataKey) {
            checkClosed();
            ValidData<T> data = new SimpleValidData<>(dataKey, myDisabledIfAbsent);
            myDisabledIfAbsent = data;
            myUpdatable = true;
            return data;
        }

        public <T> ValidData<T> invisibleAndDisabledIfAbsent(@Nonnull Key<T> dataKey) {
            checkClosed();
            ValidData<T> data = new SimpleValidData<>(dataKey, myInvisibleAndDisabledIfAbsent);
            myInvisibleAndDisabledIfAbsent = data;
            myUpdatable = true;
            return data;
        }

        public Builder onUpdate(Consumer<AnActionEvent> eventConsumer) {
            checkClosed();
            myUpdateEventConsumer = eventConsumer;
            return this;
        }

        protected void close() {
            myClosed = true;
        }

        private void checkClosed() {
            if (myClosed) {
                throw new IllegalStateException("Builder is already closed");
            }
        }
    }

    protected abstract class ValidData<T> {
        @Nonnull
        public final T get() {
            return Objects.requireNonNull(getInternal());
        }

        @Nullable
        public abstract ValidData<?> next();

        @Nullable
        protected abstract T getInternal();

        protected abstract void updateValue(@Nonnull AnActionEvent e);

        protected abstract boolean isValid();

        protected abstract void cleanup();
    }

    protected class SimpleValidData<T> extends ValidData<T> {
        @Nonnull
        private final Key<T> myKey;
        @Nullable
        private T myValue = null;
        @Nullable
        private final ValidData<?> myNext;

        public SimpleValidData(@Nonnull Key<T> key, @Nullable ValidData<?> next) {
            myKey = key;
            myNext = next;
        }

        @Nullable
        @Override
        public ValidData<?> next() {
            return myNext;
        }

        @Nullable
        @Override
        protected T getInternal() {
            return myValue;
        }

        @Override
        protected void updateValue(@Nonnull AnActionEvent e) {
            myValue = e.getData(myKey);
        }

        @Override
        protected boolean isValid() {
            return myValue != null;
        }

        @Override
        protected void cleanup() {
            myValue = null;
        }
    }

    protected class MappedValidData<T, R> extends ValidData<R> {
        @Nonnull
        private final ValidData<T> mySubData;
        @Nonnull
        private final Function<T, R> myMapper;
        private R myCachedValue = null;
        private boolean myCached = false;

        public MappedValidData(@Nonnull ValidData<T> subData, @Nonnull Function<T, R> mapper) {
            mySubData = subData;
            myMapper = mapper;
        }

        @Nullable
        @Override
        public ValidData<?> next() {
            return mySubData.next();
        }

        @Override
        protected void updateValue(@Nonnull AnActionEvent e) {
            myCached = false;
            mySubData.updateValue(e);
        }

        @Override
        protected boolean isValid() {
            return getInternal() != null;
        }

        @Nullable
        @Override
        protected R getInternal() {
            if (!myCached) {
                myCachedValue = myMapper.apply(mySubData.get());
                myCached = true;
            }
            return myCachedValue;
        }

        @Override
        protected void cleanup() {
            mySubData.cleanup();
            myCached = false;
            myCachedValue = null;
        }
    }

    protected class CheckedValidData<T> extends ValidData<T> {
        @Nonnull
        private final ValidData<T> mySubData;
        @Nonnull
        private final Predicate<T> myValidator;

        public CheckedValidData(@Nonnull ValidData<T> subData, @Nonnull Predicate<T> validator) {
            mySubData = subData;
            myValidator = validator;
        }

        @Nullable
        @Override
        public ValidData<?> next() {
            return mySubData.next();
        }

        @Nullable
        @Override
        protected T getInternal() {
            return mySubData.getInternal();
        }

        @Override
        protected void updateValue(@Nonnull AnActionEvent e) {
            mySubData.updateValue(e);
        }

        @Override
        protected boolean isValid() {
            return mySubData.isValid() && myValidator.test(mySubData.getInternal());
        }

        @Override
        protected void cleanup() {
            mySubData.cleanup();
        }
    }

    private boolean myUpdatable = false;
    private ValidData<?> myInvisibleAndDisabledIfAbsent = null;
    private ValidData<?> myDisabledIfAbsent = null;
    private Consumer<AnActionEvent> myUpdateEventConsumer = null;

    protected ConfigurableAction() {
        Builder builder = new Builder(getTemplatePresentation());
        init(builder);
        builder.close();
    }

    @Override
    public final void update(@Nonnull AnActionEvent e) {
        try {
            for (ValidData<?> data = myInvisibleAndDisabledIfAbsent; data != null; data = data.next()) {
                data.updateValue(e);
                if (!data.isValid()) {
                    e.getPresentation().setEnabledAndVisible(false);
                    resetToDefault(e);
                    return;
                }
            }

            for (ValidData<?> data = myDisabledIfAbsent; data != null; data = data.next()) {
                data.updateValue(e);
                if (!data.isValid()) {
                    e.getPresentation().setEnabled(false);
                    resetToDefault(e);
                    return;
                }
            }

            if (myUpdateEventConsumer != null) {
                myUpdateEventConsumer.accept(e);
            }
        }
        finally {
            cleanup();
        }
    }

    @Override
    @RequiredUIAccess
    public final void actionPerformed(@Nonnull AnActionEvent e) {
        try {
            for (ValidData<?> data = myInvisibleAndDisabledIfAbsent; data != null; data = data.next()) {
                data.updateValue(e);
                if (!data.isValid()) {
                    return;
                }
            }

            for (ValidData<?> data = myDisabledIfAbsent; data != null; data = data.next()) {
                data.updateValue(e);
                if (!data.isValid()) {
                    return;
                }
            }

            performAction(e);
        }
        finally {
            cleanup();
        }
    }

    public final boolean isUpdatable() {
        return myUpdatable;
    }

    protected abstract void init(@Nonnull Builder builder);

    @RequiredUIAccess
    protected abstract void performAction(@Nonnull AnActionEvent e);

    private void resetToDefault(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Presentation templatePresentation = getTemplatePresentation();
        presentation.setTextValue(templatePresentation.getTextValue());
        presentation.setDescriptionValue(templatePresentation.getDescriptionValue());
        presentation.setIcon(templatePresentation.getIcon());
    }

    private void cleanup() {
        for (ValidData<?> data = myInvisibleAndDisabledIfAbsent; data != null; data = data.next()) {
            data.cleanup();
        }

        for (ValidData<?> data = myDisabledIfAbsent; data != null; data = data.next()) {
            data.cleanup();
        }
    }
}
