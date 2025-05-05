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
package consulo.web.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.awt.dnd.AdvancedDnDSource;
import consulo.ui.ex.awt.dnd.DnDManager;
import consulo.ui.ex.awt.dnd.DnDSource;
import consulo.ui.ex.awt.dnd.DnDTarget;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2025-05-05
 */
@ServiceImpl
@Singleton
public class WebStubDnDManagerImpl extends DnDManager {
    @Override
    public void registerSource(DnDSource source, JComponent component) {

    }

    @Override
    public void registerSource(AdvancedDnDSource source) {

    }

    @Override
    public void unregisterSource(DnDSource source, JComponent component) {

    }

    @Override
    public void unregisterSource(AdvancedDnDSource source) {

    }

    @Override
    public void registerTarget(DnDTarget target, JComponent component) {

    }

    @Override
    public void unregisterTarget(DnDTarget target, JComponent component) {

    }

    @Override
    public Component getLastDropHandler() {
        return null;
    }
}
