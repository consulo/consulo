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
package consulo.component.store.impl.internal.storage;

import consulo.component.persist.RoamingType;
import consulo.component.persist.StateSplitterEx;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StateStorageManagerMacroTest {
    private StateStorageManagerImpl myManager;

    @BeforeEach
    public void setUp() {
        myManager = new StateStorageManagerImpl(null, "project", null, () -> null, () -> null, false) {
            @Override
            public StateSplitterEx createSplitter(Class<? extends StateSplitterEx> splitter) {
                return null;
            }

            @Override
            protected String getConfigurationMacro(boolean directorySpec) {
                return StoragePathMacros.PROJECT_CONFIG_DIR;
            }
        };

        myManager.addMacro(StoragePathMacros.PROJECT_CONFIG_DIR, "/x/.consulo");
        myManager.addMacro(StoragePathMacros.DEFAULT_FILE, "/x/.consulo/misc.xml");
        myManager.addMacro(StoragePathMacros.PROJECT_FILE, "/x/.consulo/misc.xml");
        myManager.addMacro(StoragePathMacros.WORKSPACE_FILE, "/x/.consulo/workspace.xml");
    }

    @Test
    public void wholeFileMacrosAreNotWrapped() {
        assertThat(myManager.buildFileSpec(storage(StoragePathMacros.DEFAULT_FILE))).isEqualTo(StoragePathMacros.DEFAULT_FILE);
        assertThat(myManager.buildFileSpec(storage(StoragePathMacros.PROJECT_FILE))).isEqualTo(StoragePathMacros.PROJECT_FILE);
        assertThat(myManager.buildFileSpec(storage(StoragePathMacros.WORKSPACE_FILE))).isEqualTo(StoragePathMacros.WORKSPACE_FILE);
    }

    @Test
    public void defaultFileExpandsToSinglePath() {
        String spec = myManager.buildFileSpec(storage(StoragePathMacros.DEFAULT_FILE));
        assertThat(myManager.expandMacros(spec)).isEqualTo("/x/.consulo/misc.xml");
    }

    @Test
    public void relativeValueIsWrappedWithConfigDir() {
        String spec = myManager.buildFileSpec(storage("misc.xml"));
        assertThat(spec).isEqualTo(StoragePathMacros.PROJECT_CONFIG_DIR + "/misc.xml");
        assertThat(myManager.expandMacros(spec)).isEqualTo("/x/.consulo/misc.xml");
    }

    @Test
    public void exactMacroResolves() {
        assertThat(myManager.expandMacros(StoragePathMacros.PROJECT_CONFIG_DIR)).isEqualTo("/x/.consulo");
    }

    @Test
    public void nestedMacroThrows() {
        assertThatThrownBy(() -> myManager.expandMacros(StoragePathMacros.PROJECT_CONFIG_DIR + "/" + StoragePathMacros.DEFAULT_FILE))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static Storage storage(String value) {
        return new Storage() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Storage.class;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public String file() {
                return "";
            }

            @Override
            public boolean deprecated() {
                return false;
            }

            @Override
            public RoamingType roamingType() {
                return RoamingType.DEFAULT;
            }

            @Override
            public Class<? extends StateSplitterEx> stateSplitter() {
                return StateSplitterEx.class;
            }
        };
    }
}
