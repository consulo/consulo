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

import consulo.localize.LocalizeValue;
import consulo.ui.ComponentItemRender;
import consulo.ui.Length;
import consulo.ui.ListBoxBase;
import consulo.ui.TextItemRender;
import consulo.ui.TransferHandler;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Base for dummy-but-creatable headless {@link ListBoxBase}s: holds the model and the transfer handler.
 *
 * @author VISTALL
 */
public abstract class HeadlessListBoxBase<E, V> extends HeadlessValueComponentBase<V> implements ListBoxBase<E> {
    private @Nullable TransferHandler<E> myTransferHandler;
    private final FlatDataModel<E> myModel;

    protected HeadlessListBoxBase(FlatDataModel<E> model) {
        myModel = model;
    }

    protected HeadlessListBoxBase(FlatDataModel<E> model, @Nullable V value) {
        super(value);
        myModel = model;
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<E, Length> getter) {
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<E> handler) {
        myTransferHandler = handler;
    }

    @Override
    public @Nullable TransferHandler<E> getTransferHandler() {
        return myTransferHandler;
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
    }
}
