/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ide;

import com.intellij.ide.presentation.Presentation;
import com.intellij.ide.presentation.PresentationProvider;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.style.StandardColors;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * @author peter
 */
@Singleton
public class TypePresentationServiceImpl extends TypePresentationService {

  @Override
  public Image getIcon(@Nonnull Object o) {
    return getIcon(o.getClass(), o);
  }

  @Override
  @Nullable
  public Image getTypeIcon(Class type) {
    return getIcon(type, null);
  }

  @Nullable
  @Override
  public String getPresentableName(Object o) {
    Set<PresentationTemplate> templates = mySuperClasses.get(o.getClass());
    for (PresentationTemplate template : templates) {
      String name = template.getName(o);
      if (name != null) {
        return name;
      }
    }
    return null;
  }

  private Image getIcon(Class type, Object o) {
    Set<PresentationTemplate> templates = mySuperClasses.get(type);
    for (PresentationTemplate template : templates) {
      Image icon = template.getIcon(o, 0);
      if (icon != null) return icon;
    }
    return null;
  }

  @Override
  @Nullable
  public String getTypePresentableName(Class type) {
    Set<PresentationTemplate> templates = mySuperClasses.get(type);
    for (PresentationTemplate template : templates) {
      String typeName = template.getTypeName();
      if (typeName != null) return typeName;
    }
    return getDefaultTypeName(type);
  }

  @Override
  public String getTypeName(Object o) {
    Set<PresentationTemplate> templates = mySuperClasses.get(o.getClass());
    for (PresentationTemplate template : templates) {
      String typeName = template.getTypeName(o);
      if (typeName != null) return typeName;
    }
    return null;
  }

  public TypePresentationServiceImpl() {
    for (TypeIconEP ep : TypeIconEP.EP_NAME.getExtensionList()) {
      myIcons.put(ep.className, ep.getIcon());
    }
    for (TypeNameEP ep : TypeNameEP.EP_NAME.getExtensionList()) {
      myNames.put(ep.className, ep.getTypeName());
    }
  }

  @Nullable
  private PresentationTemplate createPresentationTemplate(Class<?> type) {
    Presentation presentation = type.getAnnotation(Presentation.class);
    if (presentation != null) {
      return new PresentationTemplateImpl(presentation);
    }
    final NullableLazyValue<Image> icon = myIcons.get(type.getName());
    final NullableLazyValue<String> typeName = myNames.get(type.getName());
    if (icon != null || typeName != null) {
      return new PresentationTemplate() {
        @Override
        public Image getIcon(Object o, int flags) {
          return icon == null ? null : icon.getValue();
        }

        @Override
        public String getName(Object o) {
          return null;
        }

        @Override
        public String getTypeName() {
          return typeName == null ? null : typeName.getValue();
        }

        @Override
        public String getTypeName(Object o) {
          return getTypeName();
        }
      };
    }
    return null;
  }

  private final Map<String, NullableLazyValue<Image>> myIcons = new HashMap<>();
  private final Map<String, NullableLazyValue<String>> myNames = new HashMap<>();

  private final ConcurrentMap<Class, Set<PresentationTemplate>> mySuperClasses = ConcurrentFactoryMap.createMap(key -> {
    LinkedHashSet<PresentationTemplate> templates = new LinkedHashSet<>();
    walkSupers(key, new LinkedHashSet<>(), templates);
    return templates;
  });

  private void walkSupers(Class aClass, Set<Class> classes, Set<PresentationTemplate> templates) {
    if (!classes.add(aClass)) {
      return;
    }
    ContainerUtil.addIfNotNull(templates, createPresentationTemplate(aClass));
    final Class superClass = aClass.getSuperclass();
    if (superClass != null) {
      walkSupers(superClass, classes, templates);
    }

    for (Class intf : aClass.getInterfaces()) {
      walkSupers(intf, classes, templates);
    }
  }

  /**
   * @author Dmitry Avdeev
   */
  public static class PresentationTemplateImpl implements PresentationTemplate {

    @Override
    @Nullable
    public Image getIcon(Object o, int flags) {
      if (o == null) return myIcon.getValue();
      PresentationProvider provider = myPresentationProvider.getValue();
      if (provider == null) {
        return myIcon.getValue();
      }
      else {
        Image icon = provider.getIcon(o);
        return icon == null ? myIcon.getValue() : icon;
      }
    }

    @Override
    @Nullable
    public String getTypeName() {
      return StringUtil.isEmpty(myPresentation.typeName()) ? null : myPresentation.typeName();
    }

    @Override
    public String getTypeName(Object o) {
      PresentationProvider provider = myPresentationProvider.getValue();
      if (provider != null) {
        String typeName = provider.getTypeName(o);
        if (typeName != null) return typeName;
      }
      return getTypeName();
    }

    @Override
    @Nullable
    public String getName(Object o) {
      PresentationProvider namer = myPresentationProvider.getValue();
      return namer == null ? null : namer.getName(o);
    }

    public PresentationTemplateImpl(Presentation presentation) {
      this.myPresentation = presentation;
    }

    private final Presentation myPresentation;

    private final NullableLazyValue<Image> myIcon = new NullableLazyValue<Image>() {
      @Override
      protected Image compute() {
        if (!StringUtil.isEmpty(myPresentation.icon())) {
          return ImageEffects.colorFilled(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE, StandardColors.BLACK);
        }

        String groupId = myPresentation.iconGroupId();
        String imageId = myPresentation.imageId();
        if(!StringUtil.isEmpty(groupId) && !StringUtil.isEmpty(imageId)) {
          return ImageKey.of(groupId, imageId.toLowerCase(Locale.ROOT), Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
        }
        return null;
      }
    };

    private final NullableLazyValue<PresentationProvider> myPresentationProvider = new NullableLazyValue<PresentationProvider>() {
      @Override
      protected PresentationProvider compute() {
        Class<? extends PresentationProvider> aClass = myPresentation.provider();

        try {
          return aClass == PresentationProvider.class ? null : aClass.newInstance();
        }
        catch (Exception e) {
          return null;
        }
      }
    };
  }

  interface PresentationTemplate {

    @Nullable
    Image getIcon(Object o, int flags);

    @Nullable
    String getName(Object o);

    @Nullable
    String getTypeName();

    String getTypeName(Object o);
  }
}
