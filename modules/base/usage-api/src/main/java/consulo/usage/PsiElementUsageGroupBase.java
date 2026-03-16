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
package consulo.usage;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.language.editor.util.NavigationItemFileStatus;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.navigation.NavigateOptions;
import consulo.navigation.NavigationItem;
import consulo.ui.image.Image;
import consulo.usage.localize.UsageLocalize;
import consulo.virtualFileSystem.status.FileStatus;


import java.util.Objects;

/**
 * @author Maxim.Mossienko
 */
public class PsiElementUsageGroupBase<T extends PsiElement & NavigationItem> implements UsageGroup, NamedPresentably, UiDataProvider {
    private final SmartPsiElementPointer myElementPointer;
    private final String myName;
    private final Image myIcon;

    public PsiElementUsageGroupBase(T element, Image icon) {
        myElementPointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
        myName = getPresentationName(element);
        myIcon = icon;
    }

    @RequiredReadAction
    public PsiElementUsageGroupBase(T element) {
        this(element, IconDescriptorUpdaters.getIcon(element, 0));
    }

    
    private static <T extends PsiElement & NavigationItem> String getPresentationName(T element) {
        String name = element.getName();
        return name != null ? name : UsageLocalize.usageElementWithoutName().get();
    }

    @Override
    public Image getIcon() {
        return myIcon;
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public T getElement() {
        return (T)myElementPointer.getElement();
    }

    @Override
    
    public String getText(UsageView view) {
        return myName;
    }

    @Override
    @RequiredReadAction
    public FileStatus getFileStatus() {
        return isValid() ? NavigationItemFileStatus.get(getElement()) : null;
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        T element = getElement();
        return element != null && element.isValid();
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean focus) throws UnsupportedOperationException {
        if (getNavigateOptions().canNavigate()) {
            getElement().navigate(focus);
        }
    }

    @Override
    @RequiredReadAction
    public NavigateOptions getNavigateOptions() {
        return isValid() ? NavigateOptions.CAN_NAVIGATE_FULL : NavigateOptions.CANT_NAVIGATE;
    }

    @Override
    public void update() {
    }

    @Override
    public int compareTo(UsageGroup o) {
        String name = o instanceof NamedPresentably namedPresentably ? namedPresentably.getPresentableName() : o.getText(null);
        return myName.compareToIgnoreCase(name);
    }

    @Override
    @RequiredReadAction
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PsiElementUsageGroupBase group)) {
            return false;
        }
        if (isValid() && group.isValid()) {
            return getElement().getManager().areElementsEquivalent(getElement(), group.getElement());
        }
        return Objects.equals(myName, group.myName);
    }

    @Override
    public int hashCode() {
        return myName.hashCode();
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.lazy(PsiElement.KEY, () -> {
            if (!isValid()) {
                return null;
            }
            return getElement();
        });
        sink.lazy(UsageView.USAGE_INFO_KEY, () -> {
            if (!isValid()) {
                return null;
            }
            T element = getElement();
            return element != null ? new UsageInfo(element) : null;
        });
    }

    @Override
    
    public String getPresentableName() {
        return myName;
    }
}
