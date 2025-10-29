/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.presentationAssistant;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Nikolay Chashnikov (kotlin)
 * @author VISTALL
 * @since 2017-08-21
 */
@Singleton
@State(name = "PresentationAssistant", storages = @Storage("presentation-assistant.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class PresentationAssistant implements Disposable, PersistentStateComponent<PresentationAssistantState> {
    @Nonnull
    public static PresentationAssistant getInstance() {
        return Application.get().getInstance(PresentationAssistant.class);
    }

    private PresentationAssistantState myConfiguration = new PresentationAssistantState();
    private ShortcutPresenter myPresenter;

    @Nullable
    @Override
    public PresentationAssistantState getState() {
        return myConfiguration;
    }

    @Override
    public void loadState(PresentationAssistantState state) {
        XmlSerializerUtil.copyBean(state, myConfiguration);
    }

    @Override
    public void afterLoadState() {
        if (myConfiguration.myShowActionDescriptions) {
            myPresenter = new ShortcutPresenter();
        }
    }

    @Nonnull
    public PresentationAssistantState getConfiguration() {
        return myConfiguration;
    }

    public void setShowActionsDescriptions(boolean value, Project project) {
        myConfiguration.myShowActionDescriptions = value;
        if (value && myPresenter == null) {
            myPresenter = new ShortcutPresenter();
            myPresenter.showActionInfo(new ShortcutPresenter.ActionData(
                "presentationAssistant.ShowActionDescriptions",
                project,
                LocalizeValue.localizeTODO("Show Descriptions of Actions"),
                null
            ));
        }

        if (!value && myPresenter != null) {
            myPresenter.disable();
            myPresenter = null;
        }
    }

    @Override
    public void dispose() {
        if (myPresenter != null) {
            myPresenter.disable();
        }
    }

    public void setFontSize(int value) {
        myConfiguration.myFontSize = value;
    }
}
