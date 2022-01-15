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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.intellij.plugins.intelliLang.inject.config.BaseInjection;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

/**
 * Abstract base class for the different configuration panels that tries to simplify the use of
 * of nested forms
 */
public abstract class AbstractInjectionPanel<T extends BaseInjection> implements InjectionPanel<T>
{
	private final List<Field> myOtherPanels = new ArrayList<Field>(3);
	private final List<Runnable> myUpdaters = new ArrayList<Runnable>(1);

	@Nonnull
	private final Project myProject;

	/**
	 * The orignal item - must not be modified unless apply() is called.
	 */
	@Nonnull
	private final T myOrigInjection;

	/**
	 * Represents the current UI state. Outside access should use {@link #getInjection()}
	 */
	private T myEditCopy;

	protected AbstractInjectionPanel(@Nonnull T injection, @Nonnull Project project)
	{
		myOrigInjection = injection;
		myProject = project;

		final Field[] declaredFields = getClass().getDeclaredFields();
		for(Field field : declaredFields)
		{
			if(InjectionPanel.class.isAssignableFrom(field.getType()))
			{
				field.setAccessible(true);
				myOtherPanels.add(field);
			}
		}
	}

	@Nonnull
	public Project getProject()
	{
		return myProject;
	}

	@Nonnull
	public T getOrigInjection()
	{
		return myOrigInjection;
	}

	@Override
	public final T getInjection()
	{
		apply(myEditCopy);
		return myEditCopy;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public final void init(@Nonnull T copy)
	{
		myEditCopy = copy;

		for(Field panel : myOtherPanels)
		{
			final InjectionPanel p = getField(panel);
			p.init(copy);
		}
		reset();
	}

	@Override
	public final boolean isModified()
	{
		apply(myEditCopy);

		for(Field panel : myOtherPanels)
		{
			final InjectionPanel p = getField(panel);
			p.isModified();
		}

		return !myEditCopy.equals(myOrigInjection);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public final void apply()
	{
		apply(myOrigInjection);

		for(Field panel : myOtherPanels)
		{
			getField(panel).apply();
		}
		myOrigInjection.generatePlaces();
		myEditCopy.copyFrom(myOrigInjection);
	}

	protected abstract void apply(T other);

	@Override
	@SuppressWarnings({"unchecked"})
	public final void reset()
	{
		for(Field panel : myOtherPanels)
		{
			getField(panel).reset();
		}
		myEditCopy.copyFrom(myOrigInjection);
		UIUtil.invokeAndWaitIfNeeded(new Runnable()
		{
			@Override
			public void run()
			{
				resetImpl();
			}
		});
	}

	protected abstract void resetImpl();

	@Override
	public void addUpdater(Runnable updater)
	{
		myUpdaters.add(updater);
		for(Field panel : myOtherPanels)
		{
			final InjectionPanel field = getField(panel);
			field.addUpdater(updater);
		}
	}

	private InjectionPanel getField(Field field)
	{
		try
		{
			return ((InjectionPanel) field.get(this));
		}
		catch(IllegalAccessException e)
		{
			throw new Error(e);
		}
	}

	protected void updateTree()
	{
		apply(myEditCopy);
		for(Runnable updater : myUpdaters)
		{
			updater.run();
		}
	}

	public class TreeUpdateListener extends DocumentAdapter
	{
		@Override
		public void documentChanged(DocumentEvent e)
		{
			updateTree();
		}
	}
}
