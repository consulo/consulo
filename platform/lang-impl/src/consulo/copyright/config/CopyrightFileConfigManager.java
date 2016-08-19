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

package consulo.copyright.config;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.maddyhome.idea.copyright.CopyrightUpdaters;
import com.maddyhome.idea.copyright.psi.UpdateCopyrightsProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 */
@consulo.lombok.annotations.Logger
public class CopyrightFileConfigManager implements JDOMExternalizable, Cloneable {
  public abstract class LoadedOption {
    public abstract CopyrightFileConfig getConfig();

    public abstract void read(Element element) throws InvalidDataException;

    public abstract void write(Element element) throws WriteExternalException;
  }

  public class TemplateLoadedOption extends LoadedOption {
    private CopyrightFileConfig myCopyrightFileConfig;

    public TemplateLoadedOption() {
      this(new CopyrightFileConfig());
    }

    public TemplateLoadedOption(CopyrightFileConfig copyrightFileConfig) {
      myCopyrightFileConfig = copyrightFileConfig;
    }

    @Override
    public CopyrightFileConfig getConfig() {
      return myCopyrightFileConfig;
    }

    @Override
    public void read(Element element) throws InvalidDataException {
      myCopyrightFileConfig.readExternal(element);
    }

    @Override
    public void write(Element element) throws WriteExternalException {
      myCopyrightFileConfig.writeExternal(element);
    }
  }

  public class ValidLoadedOption extends LoadedOption {
    private CopyrightFileConfig myCopyrightFileConfig;

    protected ValidLoadedOption(@NotNull CopyrightFileConfig options) {
      myCopyrightFileConfig = options;
    }

    @Override
    public CopyrightFileConfig getConfig() {
      return myCopyrightFileConfig;
    }

    @Override
    public void read(Element element) throws InvalidDataException{
      myCopyrightFileConfig.readExternal(element);
    }

    @Override
    public void write(Element element) throws WriteExternalException {
      myCopyrightFileConfig.writeExternal(element);
    }
  }

  public class UnknownLoadedOption extends LoadedOption {
    private Element myElement;

    @Override
    public CopyrightFileConfig getConfig() {
      return new CopyrightFileConfig();
    }

    @Override
    public void read(Element element) throws InvalidDataException {
      myElement = element;
    }

    @Override
    public void write(Element element) throws WriteExternalException {
      element.addContent(myElement.cloneContent());
    }
  }

  public static final String LANG_TEMPLATE = "__TEMPLATE__";

  private Map<String, LoadedOption> myConfigs = new TreeMap<String, LoadedOption>();

  @NotNull
  public CopyrightFileConfig getOptions(@NotNull FileType type) {
    LoadedOption copyrightFileConfig = myConfigs.get(type.getName());
    if (copyrightFileConfig == null) {
      UpdateCopyrightsProvider updateCopyrightsProvider = CopyrightUpdaters.INSTANCE.forFileType(type);
      if (updateCopyrightsProvider == null) {
        return CopyrightFileConfig.DEFAULT_SETTINGS_HOLDER;
      }
      return updateCopyrightsProvider.createDefaultOptions();
    }
    return copyrightFileConfig.getConfig();
  }

  @NotNull
  public CopyrightFileConfig getTemplateOptions() {
    LoadedOption copyrightFileConfig = myConfigs.get(LANG_TEMPLATE);
    if (copyrightFileConfig == null) {
      return CopyrightFileConfig.DEFAULT_SETTINGS_HOLDER;
    }
    return copyrightFileConfig.getConfig();
  }

  public void setTemplateOptions(@NotNull CopyrightFileConfig options) {
    myConfigs.put(LANG_TEMPLATE, new TemplateLoadedOption(options));
  }

  public void setOptions(@NotNull FileType fileType, @NotNull CopyrightFileConfig options) {
    myConfigs.put(fileType.getName(), new ValidLoadedOption(options));
  }

  @NotNull
  public CopyrightFileConfig getMergedOptions(@NotNull FileType fileType) {
    try {
      CopyrightFileConfig lang = getOptions(fileType).clone();
      CopyrightFileConfig temp = getTemplateOptions().clone();
      switch (lang.getFileTypeOverride()) {
        case CopyrightFileConfig.USE_TEMPLATE:
          temp.setFileLocation(lang.getFileLocation());
          temp.setFileTypeOverride(lang.getFileTypeOverride());
          lang = temp;
          break;
        case CopyrightFileConfig.USE_TEXT:
          break;
      }

      return lang;
    }
    catch (CloneNotSupportedException e) {
      // This shouldn't happen
    }

    return new CopyrightFileConfig();
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    List<Element> langs = element.getChildren("LanguageOptions");
    for (Element configElement : langs) {
      String name = configElement.getAttributeValue("name");

      final LoadedOption option;

      if(LANG_TEMPLATE.equals(name)) {
        option = new TemplateLoadedOption();
      }
      else {
        FileType fileTypeByName = FileTypeRegistry.getInstance().findFileTypeByName(name);
        if (fileTypeByName == null) {
          option = new UnknownLoadedOption();
        }
        else {
          UpdateCopyrightsProvider updateCopyrightsProvider = CopyrightUpdaters.INSTANCE.forFileType(fileTypeByName);
          if(updateCopyrightsProvider == null) {
            option = new UnknownLoadedOption();
          }
          else {
            option = new ValidLoadedOption(updateCopyrightsProvider.createDefaultOptions());
          }
        }
      }

      option.read(configElement);

      myConfigs.put(name, option);
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    for (Map.Entry<String, LoadedOption> entry : myConfigs.entrySet()) {
      Element elem = new Element("LanguageOptions");
      elem.setAttribute("name", entry.getKey());
      element.addContent(elem);

      entry.getValue().write(elem);
    }
  }
}