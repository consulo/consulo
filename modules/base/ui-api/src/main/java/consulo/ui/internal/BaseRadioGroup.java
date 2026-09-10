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
package consulo.ui.internal;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.RadioButton;
import consulo.ui.RadioGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * What every frontend's group has in common - which option is chosen, and telling the others when that changes.
 * <p/>
 * A frontend extends this to hand its buttons to whatever its own toolkit calls a group, so the arrow keys and
 * the screen reader behave the way they do in every other application on that platform.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public class BaseRadioGroup<V> implements RadioGroup<V> {
    private final Map<RadioButton, V> myValues = new LinkedHashMap<>();
    private final List<Consumer<V>> myListeners = Lists.newLockFreeCopyOnWriteList();
    private @Nullable V myValue;

    @RequiredUIAccess
    @Override
    public RadioButton newButton(LocalizeValue text, V value) {
        RadioButton button = UIInternal.get()._Components_radioButton(text, false);

        myValues.put(button, value);
        attach(button);

        button.addValueListener(event -> {
            if (Boolean.TRUE.equals(event.getValue())) {
                select(value, true);
            }
        });

        return button;
    }

    /**
     * Hands the button to whatever this frontend calls a group. Nothing has to happen here - which option is
     * chosen is answered above, the same way everywhere.
     */
    protected void attach(RadioButton button) {
    }

    @Override
    public @Nullable V getValue() {
        return myValue;
    }

    @Override
    public V getValueOrError() {
        return Objects.requireNonNull(myValue, "value required");
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable V value) {
        setValue(value, true);
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable V value, boolean fireListeners) {
        for (Map.Entry<RadioButton, V> entry : myValues.entrySet()) {
            entry.getKey().setValue(Objects.equals(entry.getValue(), value), false);
        }

        select(value, fireListeners);
    }

    @Override
    public Disposable addValueListener(Consumer<V> listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    /**
     * A button reporting that it was chosen has already drawn itself that way, so the others are the ones told,
     * and told without firing so that this does not come straight back round.
     */
    @RequiredUIAccess
    private void select(@Nullable V value, boolean fireListeners) {
        if (Objects.equals(myValue, value)) {
            return;
        }

        myValue = value;

        for (Map.Entry<RadioButton, V> entry : myValues.entrySet()) {
            if (!Objects.equals(entry.getValue(), value) && Boolean.TRUE.equals(entry.getKey().getValue())) {
                entry.getKey().setValue(false, false);
            }
        }

        if (fireListeners) {
            for (Consumer<V> listener : myListeners) {
                listener.accept(value);
            }
        }
    }
}
