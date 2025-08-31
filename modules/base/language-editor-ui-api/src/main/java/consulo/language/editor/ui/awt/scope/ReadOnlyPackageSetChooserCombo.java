/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.ui.awt.scope;

import consulo.content.scope.NamedScope;
import consulo.content.scope.PackageSet;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.language.editor.wolfAnalyzer.ProblemScopeHolder;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.JBComboBoxTableCellEditorComponent;
import consulo.ui.ex.awt.ListCellRendererWrapper;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ReadOnlyPackageSetChooserCombo extends ComponentWithBrowseButton<JComponent> {
    private static final Logger LOG = Logger.getInstance(ReadOnlyPackageSetChooserCombo.class);

    private final Project myProject;

    public ReadOnlyPackageSetChooserCombo(Project project, String preselect) {
        this(project, preselect, true);
    }

    public ReadOnlyPackageSetChooserCombo(Project project, @Nullable String preselect, boolean useCombo) {
        super(useCombo ? new JComboBox() : new JBComboBoxTableCellEditorComponent(), null);
        myProject = project;

        JComponent component = getChildComponent();
        if (component instanceof JComboBox) {
            component.setBorder(null);
        }

        getButton().setVisible(false);

        if (component instanceof JComboBox) {
            ((JComboBox) component).setRenderer(new ListCellRendererWrapper<NamedScope>() {
                @Override
                public void customize(JList list, NamedScope value, int index, boolean selected, boolean hasFocus) {
                    setText(value == null ? "" : value.getName());
                }
            });
        }
        else {
            ((JBComboBoxTableCellEditorComponent) component).setToString(o -> o == null ? "" : ((NamedScope) o).getName());
        }

        rebuild();

        selectScope(preselect);
    }

    private void selectScope(String preselect) {
        JComponent component = getChildComponent();
        if (preselect != null) {
            if (component instanceof JComboBox) {
                DefaultComboBoxModel model = (DefaultComboBoxModel) ((JComboBox) component).getModel();
                for (int i = 0; i < model.getSize(); i++) {
                    NamedScope descriptor = (NamedScope) model.getElementAt(i);
                    if (preselect.equals(descriptor.getName())) {
                        ((JComboBox) component).setSelectedIndex(i);
                        break;
                    }
                }
            }
            else {
                Object[] options = ((JBComboBoxTableCellEditorComponent) component).getOptions();
                for (Object option : options) {
                    NamedScope descriptor = (NamedScope) option;
                    if (preselect.equals(descriptor.getName())) {
                        ((JBComboBoxTableCellEditorComponent) component).setDefaultValue(descriptor);
                        break;
                    }
                }
            }
        }
    }

    private void rebuild() {
        JComponent component = getChildComponent();
        NamedScope[] model = createModel();
        if (component instanceof JComboBox) {
            ((JComboBox) component).setModel(new DefaultComboBoxModel(model));
        }
        else {
            ((JBComboBoxTableCellEditorComponent) component).setOptions(model);
        }
    }

    protected NamedScope[] createModel() {
        Collection<NamedScope> model = new ArrayList<>();
        DependencyValidationManager manager = DependencyValidationManager.getInstance(myProject);
        model.addAll(Arrays.asList(manager.getScopes()));
        for (PackageSet unnamedScope : manager.getUnnamedScopes().values()) {
            model.add(new NamedScope.UnnamedScope(unnamedScope));
        }
        model.remove(myProject.getInstance(ProblemScopeHolder.class).getProblemsScope());
        return model.toArray(new NamedScope[model.size()]);
    }

    @Nullable
    public NamedScope getSelectedScope() {
        JComponent component = getChildComponent();
        if (component instanceof JComboBox) {
            int idx = ((JComboBox) component).getSelectedIndex();
            if (idx < 0) {
                return null;
            }
            return (NamedScope) ((JComboBox) component).getSelectedItem();
        }
        else {
            return (NamedScope) ((JBComboBoxTableCellEditorComponent) component).getEditorValue();
        }
    }
}