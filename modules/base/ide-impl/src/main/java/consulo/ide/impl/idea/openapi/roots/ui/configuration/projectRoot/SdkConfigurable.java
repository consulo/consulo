/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModel;
import consulo.content.bundle.SdkType;
import consulo.content.bundle.SdkUtil;
import consulo.disposer.Disposable;
import consulo.ide.impl.bundle.SdkEditor;
import consulo.localize.LocalizeValue;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author anna
 * @since 2006-06-05
 */
public class SdkConfigurable implements MasterDetailsConfigurable<Sdk> {
    private final SdkImpl mySdk;
    private final SdkEditor mySdkEditor;

    @RequiredUIAccess
    public SdkConfigurable(SdkImpl sdk, SdkModel sdksModel, Runnable updateTree) {
        mySdk = sdk;
        mySdkEditor = new SdkEditor(sdksModel, mySdk, updateTree);
    }

    @Override
    public Sdk getEditableObject() {
        return mySdk;
    }

    @Override
    public LocalizeValue getBannerSlogan() {
        return ProjectLocalize.sdkBannerText(((SdkType) mySdk.getSdkType()).getDisplayName(), mySdk.getName());
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(mySdk.getName());
    }

    @Override
    public Image getIcon() {
        return SdkUtil.getIcon(mySdk);
    }

    @Override
    public @Nullable String getHelpTopic() {
        return ((SdkType) mySdk.getSdkType()).getHelpTopic();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentUIDisposable) {
        return mySdkEditor.createUIComponent(parentUIDisposable);
    }

    @RequiredUIAccess
    @Override
    public void updateName() {
        mySdkEditor.updateName();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return mySdkEditor.isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        mySdkEditor.apply();
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        mySdkEditor.reset();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        mySdkEditor.disposeUIResources();
    }

    public Sdk getSdk() {
        return mySdk;
    }
}
