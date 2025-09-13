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
package consulo.ide.impl.internal.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ui.ex.internal.KeyMapSetting;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-09-13
 */
@Singleton
@ServiceImpl
@State(name = "KeyboardSettings", storages = @Storage("keyboard.xml"))
public class KeyMapSettingImpl implements KeyMapSetting, PersistentStateComponent<KeyMapSettingState> {
    private KeyMapSettingState myState = new KeyMapSettingState();

    @Override
    public Boolean isUseUnicodeShortcuts() {
        return myState.useUnicodeCharactersForShortcuts;
    }

    @Override
    public void setUseUnicodeShortcuts(@Nullable Boolean value) {
        myState.useUnicodeCharactersForShortcuts = value;
    }

    @Nullable
    @Override
    public KeyMapSettingState getState() {
        return myState;
    }

    @Override
    public void loadState(KeyMapSettingState state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
}
