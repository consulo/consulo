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
package consulo.configurable;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yole
 */
@Deprecated
@DeprecationInfo("Use 'consulo.ide.impl.options.SimpleConfigurableByProperties'")
public abstract class BeanConfigurable<T> implements UnnamedConfigurable {
  private final T myInstance;

  private static abstract class BeanField<T extends Component> {
    String myFieldName;
    T myComponent;

    private BeanField(final String fieldName) {
      myFieldName = fieldName;
    }

    T getComponent() {
      if (myComponent == null) {
        myComponent = createComponent();
      }
      return myComponent;
    }

    abstract T createComponent();

    boolean isModified(Object instance) {
      final Object componentValue = getComponentValue();
      final Object beanValue = getBeanValue(instance);
      return !Objects.equals(componentValue, beanValue);
    }

    void apply(Object instance) {
      setBeanValue(instance, getComponentValue());
    }

    void reset(Object instance) {
      setComponentValue(getBeanValue(instance));
    }

    abstract Object getComponentValue();

    abstract void setComponentValue(Object instance);

    Object getBeanValue(Object instance) {
      try {
        Field field = instance.getClass().getField(myFieldName);
        return field.get(instance);
      }
      catch (NoSuchFieldException e) {
        try {
          final Method method = instance.getClass().getMethod(getterName());
          return method.invoke(instance);
        }
        catch (Exception e1) {
          throw new RuntimeException(e1);
        }
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    @NonNls
    protected String getterName() {
      return "get" + StringUtil.capitalize(myFieldName);
    }

    void setBeanValue(Object instance, Object value) {
      try {
        Field field = instance.getClass().getField(myFieldName);
        field.set(instance, value);
      }
      catch (NoSuchFieldException e) {
        try {
          final Method method = instance.getClass().getMethod("set" + StringUtil.capitalize(myFieldName), getValueClass());
          method.invoke(instance, value);
        }
        catch (Exception e1) {
          throw new RuntimeException(e1);
        }
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    protected abstract Class getValueClass();
  }

  private static class CheckboxField extends BeanField<CheckBox> {
    private final String myTitle;

    private CheckboxField(final String fieldName, final String title) {
      super(fieldName);
      myTitle = title;
    }

    @Override
    CheckBox createComponent() {
      return CheckBox.create(LocalizeValue.of(myTitle));
    }

    @Override
    Object getComponentValue() {
      return getComponent().getValueOrError();
    }

    @Override
    void setComponentValue(final Object instance) {
      getComponent().setValue(((Boolean)instance).booleanValue());
    }

    @Override
    protected String getterName() {
      return "is" + StringUtil.capitalize(myFieldName);
    }

    @Override
    protected Class getValueClass() {
      return boolean.class;
    }
  }

  private final List<BeanField> myFields = new ArrayList<>();

  protected BeanConfigurable(T beanInstance) {
    myInstance = beanInstance;
  }

  protected void checkBox(String fieldName, String title) {
    myFields.add(new CheckboxField(fieldName, title));
  }

  @RequiredUIAccess
  @Override
  public Component createUIComponent(@Nonnull Disposable uiDisposable) {
    final VerticalLayout panel = VerticalLayout.create();
    for (BeanField field : myFields) {
      panel.add(field.getComponent());
    }
    return panel;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    for (BeanField field : myFields) {
      if (field.isModified(myInstance)) return true;
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    for (BeanField field : myFields) {
      field.apply(myInstance);
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    for (BeanField field : myFields) {
      field.reset(myInstance);
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
  }
}
