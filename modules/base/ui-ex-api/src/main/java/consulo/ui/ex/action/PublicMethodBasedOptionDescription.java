/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.action;

import consulo.util.lang.EmptyRunnable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Sergey.Malenkov
 */
public final class PublicMethodBasedOptionDescription<T> extends BooleanOptionDescription {
    private final Supplier<T> myInstanceGetter;
    private final Function<T, Boolean> myGetter;
    private final BiConsumer<T, Boolean> mySetter;
    private final Runnable myFireUpdated;

    public PublicMethodBasedOptionDescription(String option,
                                              String configurableId,
                                              Supplier<T> instanceGetter,
                                              Function<T, Boolean> getter,
                                              BiConsumer<T, Boolean> setter) {
        this(option, configurableId, instanceGetter, getter, setter, EmptyRunnable.getInstance());
    }

    public PublicMethodBasedOptionDescription(String option,
                                              String configurableId,
                                              Supplier<T> instanceGetter,
                                              Function<T, Boolean> getter,
                                              BiConsumer<T, Boolean> setter,
                                              Runnable fireUpdated) {
        super(option, configurableId);
        myInstanceGetter = instanceGetter;
        myGetter = getter;
        mySetter = setter;
        myFireUpdated = fireUpdated;
    }

    public T getInstance() {
        return myInstanceGetter.get();
    }

    @Override
    public boolean isOptionEnabled() {
        return myGetter.apply(getInstance());
    }

    @Override
    public void setOptionState(boolean enabled) {
        mySetter.accept(getInstance(), enabled);

        myFireUpdated.run();
    }
}
