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
package consulo.ui.ex.internal;

import consulo.application.Application;
import consulo.component.ProcessCanceledException;
import consulo.container.PluginException;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.style.StandardColors;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * The main (and single) purpose of this class is provide lazy initialization
 * of the actions. ClassLoader eats a lot of time on startup to load the actions' classes.
 *
 * @author Vladimir Kondratyev
 */
public class XmlActionStub extends AnAction implements ActionStubBase {
    private static final Logger LOG = Logger.getInstance(XmlActionStub.class);

    private final String myClassName;
    private final String myId;
    private final String myIconPath;
    private final Supplier<Presentation> myTemplatePresentation;
    @Nonnull
    private final PluginDescriptor myPluginDescriptor;

    public XmlActionStub(@Nonnull String actionClass, @Nonnull String id, @Nonnull PluginDescriptor pluginDescriptor, String iconPath, @Nonnull Supplier<Presentation> templatePresentation) {
        LOG.assertTrue(id.length() > 0);
        myPluginDescriptor = pluginDescriptor;
        myClassName = actionClass;
        myId = id;
        myIconPath = iconPath;
        myTemplatePresentation = templatePresentation;
    }

    @Nonnull
    @Override
    protected Presentation createTemplatePresentation() {
        return myTemplatePresentation.get();
    }

    public String getClassName() {
        return myClassName;
    }

    @Override
    public String getId() {
        return myId;
    }

    public ClassLoader getLoader() {
        return myPluginDescriptor.getPluginClassLoader();
    }

    @Override
    public PluginId getPluginId() {
        return myPluginDescriptor.getPluginId();
    }

    @Override
    public String getIconPath() {
        return myIconPath;
    }

    @Nullable
    @Override
    public AnAction initialize(@Nonnull Application application, @Nonnull ActionManager manager) {
        return convertStub(application, this);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    static AnAction convertStub(@Nonnull Application application, @Nonnull XmlActionStub stub) {
        AnAction anAction = instantiate(application, stub.getClassName(), stub.getLoader(), stub.getPluginId(), AnAction.class);
        if (anAction == null) {
            return null;
        }

        stub.initAction(anAction);
        updateIconFromStub(stub, anAction);
        return anAction;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <T> T instantiate(@Nonnull Application application,
                             String stubClassName,
                             ClassLoader classLoader,
                             PluginId pluginId,
                             Class<T> expectedClass) {
        Object obj;
        try {
            Class<?> actionClass = Class.forName(stubClassName, true, classLoader);
            obj = application.getUnbindedInstance(actionClass);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(new PluginException(e, pluginId));
            return null;
        }

        if (!expectedClass.isInstance(obj)) {
            LOG.error(new PluginException("class with name '" + stubClassName + "' must be an instance of '" + expectedClass.getName() + "'; got " + obj, pluginId));
            return null;
        }
        //noinspection unchecked
        return (T) obj;
    }

    static void updateIconFromStub(@Nonnull ActionStubBase stub, AnAction anAction) {
        String iconPath = stub.getIconPath();
        if (iconPath != null) {
            ImageKey imageKey = ImageKey.fromString(iconPath, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
            if (imageKey != null) {
                anAction.getTemplatePresentation().setIcon(imageKey);
            }
            else {
                LOG.warn("Wrong icon path: " + iconPath);
                anAction.getTemplatePresentation().setIcon(ImageEffects.colorFilled(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE, StandardColors.MAGENTA));
            }
        }
    }

    public final void initAction(@Nonnull AnAction targetAction) {
        copyTemplatePresentation(getTemplatePresentation(), targetAction.getTemplatePresentation());

        targetAction.setShortcutSet(getShortcutSet());
    }

    /**
     * Copies template presentation and shortcuts set to <code>targetAction</code>.
     *
     * @param targetAction cannot be <code>null</code>
     */
    public static void copyTemplatePresentation(@Nonnull Presentation sourcePresentation, @Nonnull Presentation targetPresentation) {
        if (targetPresentation.getIcon() == null && sourcePresentation.getIcon() != null) {
            targetPresentation.setIcon(sourcePresentation.getIcon());
        }

        if (targetPresentation.getTextValue() == LocalizeValue.of() && sourcePresentation.getTextValue() != LocalizeValue.of()) {
            targetPresentation.setTextValue(sourcePresentation.getTextValue());
        }

        if (targetPresentation.getDescriptionValue() == LocalizeValue.of() && sourcePresentation.getDescriptionValue() != LocalizeValue.of()) {
            targetPresentation.setDescriptionValue(sourcePresentation.getDescriptionValue());
        }
    }
}
