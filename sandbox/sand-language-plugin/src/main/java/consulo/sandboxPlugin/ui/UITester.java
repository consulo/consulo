/*
 * Copyright 2013-2020 consulo.io
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
package consulo.sandboxPlugin.ui;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Tab;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.TabbedLayout;
import consulo.util.lang.ControlFlowException;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public class UITester {
    private static final Logger LOG = Logger.getInstance(UITester.class);

    private static class MyWindowWrapper extends DialogDescriptor {
        private final Application myApplication;

        public MyWindowWrapper(Application application) {
            super(LocalizeValue.of("UI Tester"));
            myApplication = application;
        }

        @Override
        @RequiredUIAccess
        public Component createCenterComponent(Disposable uiDisposable) {
            TabbedLayout tabbedLayout = TabbedLayout.create();

            myApplication.getExtensionPoint(UITesterTab.class).forEach(tab -> {
                Tab added = tabbedLayout.addTab(tab.getName().get(), createTabComponent(tab, uiDisposable));
                if (tab.isClosable()) {
                    added.setCloseHandler((closedTab, component) -> {
                    });
                }
            });

            return tabbedLayout;
        }

        @RequiredUIAccess
        private static Component createTabComponent(UITesterTab tab, Disposable uiDisposable) {
            try {
                return tab.createComponent(uiDisposable);
            }
            catch (Throwable e) {
                if (e instanceof ControlFlowException) {
                    throw ControlFlowException.rethrow(e);
                }

                LOG.error("Failed to create UI tester tab " + tab.getClass().getName(), e);
                return Label.create(LocalizeValue.join(LocalizeValue.localizeTODO("Failed to create the tab: "), LocalizeValue.of(e)));
            }
        }

        @Override
        public boolean hasDefaultContentBorder() {
            return false;
        }

        @Override
        public @Nullable WidthAndHeight getInitialSize() {
            // wide enough for the data grid tab
            return WidthAndHeight.ofFont(70, 40);
        }
    }

    @RequiredUIAccess
    public static void show(DialogService dialogService) {
        dialogService.build(new MyWindowWrapper(Application.get())).showAsync();
    }
}
