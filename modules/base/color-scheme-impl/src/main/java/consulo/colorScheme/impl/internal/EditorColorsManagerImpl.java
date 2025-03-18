/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.colorScheme.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.colorScheme.*;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.colorScheme.internal.ReadOnlyColorsScheme;
import consulo.component.persist.*;
import consulo.component.persist.scheme.BaseSchemeProcessor;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.ComponentTreeEventDispatcher;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.annotation.OptionTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
@State(name = "EditorColorsManagerImpl",
    // make roamingType per platform, due user can use light laf on one platform, and dark on other
    storages = @Storage(value = "colors.scheme.xml", roamingType = RoamingType.PER_OS), additionalExportFile = EditorColorsManagerImpl.FILE_SPEC)
@ServiceImpl
public class EditorColorsManagerImpl extends EditorColorsManager implements PersistentStateComponent<EditorColorsManagerImpl.State> {
    private static final Logger LOG = Logger.getInstance(EditorColorsManagerImpl.class);

    private static final String SCHEME_NODE_NAME = "scheme";
    private static final String DEFAULT_NAME = "Default";
    static final String FILE_SPEC = StoragePathMacros.ROOT_CONFIG + "/colors";

    private final ComponentTreeEventDispatcher<EditorColorsListener> myTreeDispatcher =
        ComponentTreeEventDispatcher.create(EditorColorsListener.class);

    private final SchemeManager<EditorColorsScheme, EditorColorsSchemeImpl> mySchemeManager;
    private State myState = new State();
    private final Map<String, EditorColorsScheme> myDefaultColorsSchemes = new LinkedHashMap<>();

    @Inject
    public EditorColorsManagerImpl(@Nonnull Application application, @Nonnull SchemeManagerFactory schemeManagerFactory) {
        mySchemeManager =
            schemeManagerFactory.createSchemeManager(FILE_SPEC, new BaseSchemeProcessor<EditorColorsScheme, EditorColorsSchemeImpl>() {
                @Nonnull
                @Override
                public EditorColorsSchemeImpl readScheme(@Nonnull Element element) {
                    EditorColorsSchemeImpl scheme = new EditorColorsSchemeImpl(null, EditorColorsManagerImpl.this);
                    scheme.readExternal(element);
                    return scheme;
                }

                @Override
                public Element writeScheme(@Nonnull final EditorColorsSchemeImpl scheme) {
                    Element root = new Element(SCHEME_NODE_NAME);
                    try {
                        scheme.writeExternal(root);
                    }
                    catch (WriteExternalException e) {
                        LOG.error(e);
                        return null;
                    }
                    return root;
                }

                @Nonnull
                @Override
                public State getState(@Nonnull EditorColorsSchemeImpl scheme) {
                    return scheme instanceof ReadOnlyColorsScheme ? State.NON_PERSISTENT : State.POSSIBLY_CHANGED;
                }

                @Override
                public void onCurrentSchemeChanged(final EditorColorsSchemeImpl newCurrentScheme) {
                    fireChanges(mySchemeManager.getCurrentScheme());
                }

                @Nonnull
                @Override
                public String getSchemeExtension() {
                    return ".icls";
                }

                @Override
                public boolean isUpgradeNeeded() {
                    return true;
                }

                @Nonnull
                @Override
                public String getName(@Nonnull EditorColorsScheme immutableElement) {
                    return immutableElement.getName();
                }
            }, RoamingType.DEFAULT);

        addDefaultSchemes();

        // Load default schemes from providers
        application.getExtensionPoint(BundledColorSchemeProvider.class).forEachExtensionSafe(provider -> {
            for (String colorSchemeFile : provider.getColorSchemeFiles()) {
                URL resource = provider.getClass().getResource(colorSchemeFile);
                if (resource == null) {
                    throw new IllegalArgumentException("Can't find resource: " + colorSchemeFile);
                }
                
                mySchemeManager.loadBundledScheme(resource, element -> {
                    DefaultColorsScheme defaultColorsScheme = new DefaultColorsScheme(EditorColorsManagerImpl.this);
                    defaultColorsScheme.readExternal(element);

                    myDefaultColorsSchemes.put(defaultColorsScheme.getName(), defaultColorsScheme);
                    return defaultColorsScheme;
                });
            }
        });

        mySchemeManager.loadSchemes();

        application.getExtensionPoint(EditorColorSchemeExtender.class).forEachExtensionSafe(it -> {
            EditorColorsScheme editorColorsScheme = mySchemeManager.findSchemeByName(it.getColorSchemeId());
            Class<? extends EditorColorSchemeExtender> providerClass = it.getClass();
            PluginId pluginId = PluginManager.getPlugin(providerClass).getPluginId();

            if (editorColorsScheme == null) {
                LOG.warn("Cannot find scheme: " + it.getColorSchemeId() + " from plugin: " + pluginId);
                return;
            }

            AbstractColorsScheme scheme = (AbstractColorsScheme) editorColorsScheme;

            var builder = new EditorColorSchemeExtender.Builder() {
                boolean finished;

                @Override
                public void add(@Nonnull EditorColorKey key, @Nonnull ColorValue colorValue) {
                    if (finished) {
                        throw new IllegalArgumentException("Can't add new keys. Builder closed");
                    }

                    if (!scheme.setColorValueIfNew(key, colorValue)) {
                        LOG.error("Failed to override color by key " + key + ". Plugin: " + pluginId + ", Class: " + providerClass);
                    }
                }

                @Override
                public void add(@Nonnull TextAttributesKey key, @Nonnull AttributesFlyweight attributes) {
                    if (finished) {
                        throw new IllegalArgumentException("Can't add new keys. Builder closed");
                    }

                    TextAttributes textAttributes = TextAttributes.fromFlyweight(attributes);
                    if (!scheme.setTextAttributesIfNew(key, textAttributes)) {
                        LOG.error("Failed to override text attributes by key " + key + ". Plugin: " + pluginId + ", Class: " + providerClass);
                    }
                }
            };

            it.extend(builder);

            builder.finished = true;
        });

        setGlobalSchemeInner(getDefaultSchemeFromStyle());
    }

    static class State {
        public boolean USE_ONLY_MONOSPACED_FONTS = true;

        @OptionTag(tag = "global_color_scheme", nameAttribute = "", valueAttribute = "name")
        public String colorScheme;
    }

    public TextAttributes getDefaultAttributes(TextAttributesKey key) {
        // It is reasonable to fetch attributes from Default color scheme. Otherwise if we launch IDE and then
        // try switch from custom colors scheme (e.g. with dark background) to default one. Editor will show
        // incorrect highlighting with "traces" of color scheme which was active during IDE startup.
        return getDefaultSchemeFromStyle().getAttributes(key);
    }

    @Override
    public void addColorsScheme(@Nonnull EditorColorsScheme scheme) {
        if (!isDefaultScheme(scheme) && !StringUtil.isEmpty(scheme.getName())) {
            mySchemeManager.addNewScheme(scheme, true);
        }
    }

    @Override
    public void removeAllSchemes() {
        mySchemeManager.clearAllSchemes();
        addDefaultSchemes();
    }

    @Override
    @Nonnull
    public Map<String, EditorColorsScheme> getBundledSchemes() {
        return myDefaultColorsSchemes;
    }

    private void addDefaultSchemes() {
        for (EditorColorsScheme defaultScheme : myDefaultColorsSchemes.values()) {
            mySchemeManager.addNewScheme(defaultScheme, true);
        }
    }

    @Nonnull
    @Override
    public EditorColorsScheme[] getAllSchemes() {
        List<EditorColorsScheme> schemes = mySchemeManager.getAllSchemes();
        EditorColorsScheme[] result = schemes.toArray(new EditorColorsScheme[schemes.size()]);
        Arrays.sort(result, (s1, s2) -> {
            if (isDefaultScheme(s1) && !isDefaultScheme(s2)) {
                return -1;
            }
            if (!isDefaultScheme(s1) && isDefaultScheme(s2)) {
                return 1;
            }
            if (s1.getName().equals(DEFAULT_NAME)) {
                return -1;
            }
            if (s2.getName().equals(DEFAULT_NAME)) {
                return 1;
            }
            return s1.getName().compareToIgnoreCase(s2.getName());
        });
        return result;
    }

    @Override
    public void setGlobalScheme(@Nullable EditorColorsScheme scheme) {
        setGlobalSchemeInner(scheme);

        StyleManager.get().refreshUI();

        fireChanges(scheme);
    }

    private void setGlobalSchemeInner(@Nullable EditorColorsScheme scheme) {
        mySchemeManager.setCurrentSchemeName(scheme == null ? getDefaultSchemeFromStyle().getName() : scheme.getName());
    }

    @Nonnull
    private EditorColorsScheme getDefaultSchemeFromStyle() {
        Style style = StyleManager.get().getCurrentStyle();
        return getScheme(style.isDark() ? EditorColorsScheme.DARCULA_SCHEME_NAME : EditorColorsScheme.DEFAULT_SCHEME_NAME);
    }

    @Nonnull
    @Override
    public EditorColorsScheme getGlobalScheme() {
        EditorColorsScheme scheme = mySchemeManager.getCurrentScheme();
        if (scheme != null) {
            return scheme;
        }

        return getDefaultSchemeFromStyle();
    }

    @Override
    public EditorColorsScheme getScheme(@Nonnull String schemeName) {
        return mySchemeManager.findSchemeByName(schemeName);
    }

    private void fireChanges(EditorColorsScheme scheme) {
        // we need to push events to components that use editor font, e.g. HTML editor panes
        ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorColorsListener.class).globalSchemeChange(scheme);

        myTreeDispatcher.getMulticaster().globalSchemeChange(scheme);
    }

    @Override
    public void setUseOnlyMonospacedFonts(boolean value) {
        myState.USE_ONLY_MONOSPACED_FONTS = value;
    }

    @Override
    public boolean isUseOnlyMonospacedFonts() {
        return myState.USE_ONLY_MONOSPACED_FONTS;
    }

    @Nullable
    @Override
    public State getState() {
        if (mySchemeManager.getCurrentScheme() != null) {
            String name = mySchemeManager.getCurrentScheme().getName();
            myState.colorScheme = "Default".equals(name) ? null : name;
        }
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
        setGlobalSchemeInner(myState.colorScheme == null ? getDefaultSchemeFromStyle() : mySchemeManager.findSchemeByName(myState.colorScheme));
    }

    @Override
    public boolean isDefaultScheme(EditorColorsScheme scheme) {
        return scheme instanceof DefaultColorsScheme;
    }

    @TestOnly
    public SchemeManager<EditorColorsScheme, EditorColorsSchemeImpl> getSchemeManager() {
        return mySchemeManager;
    }
}
