package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.module.ModuleConfigurationEditor;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TabbedLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ksafonov
 */
public class TabbedModuleEditor extends ModuleEditor {
    private final List<Tab> myTabs = new ArrayList<>();

    private @Nullable Tab mySelectedTab;

    public TabbedModuleEditor(Project project, ModulesConfigurator modulesProvider, LibrariesConfigurator librariesConfigurator, Module module) {
        super(project, modulesProvider, librariesConfigurator, module);
    }

    @RequiredUIAccess
    @Override
    protected Component createCenterPanel(Disposable parentUIDisposable) {
        TabbedLayout tabbedLayout = TabbedLayout.create();
        myTabs.clear();

        for (ModuleConfigurationEditor editor : myEditors) {
            Component component = ConfigurableUIMigrationUtil.createUIComponent(editor, parentUIDisposable);
            editor.initialize();
            editor.reset();

            LocalizeValue title = editor.getDisplayName();
            Tab tab = tabbedLayout.createTab();
            tab.setRenderer((it, presentation) -> presentation.append(title));
            myTabs.add(tabbedLayout.addTab(tab, component == null ? Label.create(LocalizeValue.empty()) : component));
        }

        tabbedLayout.addSelectListener(event -> mySelectedTab = event.getTab());

        restoreSelectedEditor();
        return tabbedLayout;
    }

    @RequiredUIAccess
    @Override
    protected void restoreSelectedEditor() {
        if (!myTabs.isEmpty()) {
            Tab tab = myTabs.get(0);
            tab.select();
            mySelectedTab = tab;
        }
    }

    @RequiredUIAccess
    @Override
    public void selectEditor(@Nullable String name) {
        int index = getEditorTabIndex(name);
        if (index >= 0) {
            Tab tab = myTabs.get(index);
            tab.select();
            mySelectedTab = tab;
        }
    }

    private int getEditorTabIndex(@Nullable String editorName) {
        if (editorName == null) {
            return -1;
        }

        for (int index = 0; index < myEditors.size() && index < myTabs.size(); index++) {
            if (editorName.equals(myEditors.get(index).getDisplayName().get())) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public @Nullable ModuleConfigurationEditor getEditor(String displayName) {
        int index = getEditorTabIndex(displayName);
        return index >= 0 ? myEditors.get(index) : null;
    }

    @Override
    public @Nullable ModuleConfigurationEditor getSelectedEditor() {
        Tab selectedTab = mySelectedTab;
        int index = selectedTab == null ? -1 : myTabs.indexOf(selectedTab);
        return index >= 0 && index < myEditors.size() ? myEditors.get(index) : null;
    }

    @Override
    protected void disposeCenterPanel() {
        mySelectedTab = null;
        myTabs.clear();
    }
}
