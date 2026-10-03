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
package consulo.it.internal.ui;


import consulo.ui.IntSlider;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessIntSlider extends HeadlessValueComponentBase<Integer> implements IntSlider {
    private int myMin;
    private int myMax;
    private boolean myFocusable = true;

    public HeadlessIntSlider(int min, int max, int value) {
        super(clamp(value, min, max));
        myMin = min;
        myMax = max;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, Math.max(min, max)));
    }

    @Override
    protected Integer normalize(@Nullable Integer value) {
        return clamp(value == null ? myMin : value, myMin, myMax);
    }

    @Override
    public Integer getValue() {
        return normalize(super.getValue());
    }

    @Override
    @RequiredUIAccess
    public void setRange(int min, int max) {
        myMin = min;
        myMax = max;
        setValue(super.getValue(), true);
    }

    public int getMin() {
        return myMin;
    }

    public int getMax() {
        return myMax;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public void focus() {
    }

    @Override
    public void setFocusable(boolean focusable) {
        myFocusable = focusable;
    }

    @Override
    public boolean isFocusable() {
        return myFocusable;
    }
}
