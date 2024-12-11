/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.webBrowser;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.ComboboxWithBrowseButton;
import consulo.ui.ex.awt.MutableCollectionComboBoxModel;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class BrowserSelector {
    private final ComboboxWithBrowseButton myBrowserComboWithBrowse;
    private MutableCollectionComboBoxModel<WebBrowser> myModel;

    public BrowserSelector() {
        this(true);
    }

    public BrowserSelector(final boolean allowDefaultBrowser) {
        this(browser -> allowDefaultBrowser || browser != null);
    }

    public BrowserSelector(@Nonnull final Predicate<WebBrowser> browserCondition) {
        myModel = createBrowsersComboModel(browserCondition);
        myBrowserComboWithBrowse = new ComboboxWithBrowseButton(new ComboBox(myModel));
        myBrowserComboWithBrowse.addActionListener(e -> {
            WebBrowserManager browserManager = WebBrowserManager.getInstance();
            long modificationCount = browserManager.getModificationCount();

            UIAccess uiAccess = UIAccess.current();

            browserManager.showSettings().whenCompleteAsync((o, throwable) -> {
                WebBrowser selectedItem = getSelected();
                if (modificationCount != browserManager.getModificationCount()) {
                    myModel = createBrowsersComboModel(browserCondition);
                    //noinspection unchecked
                    myBrowserComboWithBrowse.getComboBox().setModel(myModel);
                }
                if (selectedItem != null) {
                    setSelected(selectedItem);
                }
            }, uiAccess);
        });

        //noinspection unchecked
        myBrowserComboWithBrowse.getComboBox().setRenderer(SimpleListCellRenderer.<WebBrowser>create((label, value, index) -> {
            Image baseIcon;
            if (value == null) {
                WebBrowser firstBrowser = WebBrowserManager.getInstance().getFirstActiveBrowser();
                baseIcon = firstBrowser == null ? PlatformIconGroup.nodesPpweb() : firstBrowser.getIcon();
            }
            else {
                baseIcon = value.getIcon();
            }
            label.setIcon(TargetAWT.to(myBrowserComboWithBrowse.isEnabled() ? baseIcon : ImageEffects.grayed(baseIcon)));
            label.setText(value != null ? value.getName() : "Default");
        }));
    }

    public JComponent getMainComponent() {
        return myBrowserComboWithBrowse;
    }

    private static MutableCollectionComboBoxModel<WebBrowser> createBrowsersComboModel(@Nonnull Predicate<WebBrowser> browserCondition) {
        List<WebBrowser> list = new ArrayList<>();
        if (browserCondition.test(null)) {
            list.add(null);
        }
        list.addAll(WebBrowserManager.getInstance().getBrowsers(browserCondition));
        return new MutableCollectionComboBoxModel<>(list);
    }

    @Nullable
    public WebBrowser getSelected() {
        return myModel.getSelected();
    }

    @Nullable
    public String getSelectedBrowserId() {
        WebBrowser browser = getSelected();
        return browser != null ? browser.getId().toString() : null;
    }

    public void setSelected(@Nullable WebBrowser selectedItem) {
        myBrowserComboWithBrowse.getComboBox().setSelectedItem(selectedItem);
    }

    public boolean addAndSelect(@Nonnull WebBrowser browser) {
        if (myModel.contains(browser)) {
            return false;
        }

        myModel.addItem(browser);
        return true;
    }

    public int getSize() {
        return myModel.getSize();
    }
}
