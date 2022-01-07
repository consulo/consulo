/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.intelliLang.inject.config.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.LanguageTextField;
import consulo.util.dataholder.Key;
import org.intellij.plugins.intelliLang.inject.config.BaseInjection;

import javax.swing.*;

public class AdvancedPanel extends AbstractInjectionPanel<BaseInjection>
{
	public static final Key<Boolean> KEY = Key.create("IS_VALUE_REGEXP");

	private JPanel myRoot;

	private EditorTextField myValuePattern;
	private JCheckBox mySingleFileCheckBox;

	public AdvancedPanel(Project project, BaseInjection injection)
	{
		super(injection, project);
	}

	@Override
	protected void apply(BaseInjection other)
	{
		other.setValuePattern(myValuePattern.getText());
		other.setSingleFile(mySingleFileCheckBox.isSelected());
	}

	@Override
	protected void resetImpl()
	{
		BaseInjection origInjection = getOrigInjection();
		myValuePattern.setText(origInjection.getValuePattern());

		mySingleFileCheckBox.setSelected(origInjection.isSingleFile());
	}

	@Override
	public JPanel getComponent()
	{
		return myRoot;
	}

	private void createUIComponents()
	{
		Language language = Language.findLanguageByID("RegExp");
		myValuePattern = new LanguageTextField(language, getProject(), getOrigInjection().getValuePattern(), new LanguageTextField.SimpleDocumentCreator()
		{
			@Override
			public void customizePsiFile(PsiFile psiFile)
			{
				psiFile.putCopyableUserData(KEY, Boolean.TRUE);
			}
		});
	}
}
