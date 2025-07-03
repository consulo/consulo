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
package consulo.language.editor.internal.intention;

import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.file.FileTypeManager;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author cdr
 */
public final class IntentionActionMetaData {
  private static final Logger LOG = Logger.getInstance(IntentionActionMetaData.class);
  @Nonnull
  private final IntentionAction myAction;

  private final String myDescriptionDirectoryName;
  @Nonnull
  public final String[] myCategory;

  private TextDescriptor[] myExampleUsagesBefore = null;
  private TextDescriptor[] myExampleUsagesAfter = null;
  private TextDescriptor myDescription = null;
  private URL myDirURL = null;

  private static final String BEFORE_TEMPLATE_PREFIX = "before";
  private static final String AFTER_TEMPLATE_PREFIX = "after";
  static final String EXAMPLE_USAGE_URL_SUFFIX = ".template";
  private static final String DESCRIPTION_FILE_NAME = "description.html";
  private static final String INTENTION_DESCRIPTION_FOLDER = "intentionDescriptions";

  public IntentionActionMetaData(@Nonnull IntentionAction action, @Nonnull String[] category, @Nonnull String descriptionDirectoryName) {
    myAction = action;
    myCategory = category;
    myDescriptionDirectoryName = descriptionDirectoryName;
  }

  public IntentionActionMetaData(@Nonnull final IntentionAction action,
                                 @Nonnull final String[] category,
                                 final TextDescriptor description,
                                 final TextDescriptor[] exampleUsagesBefore,
                                 final TextDescriptor[] exampleUsagesAfter) {
    myAction = action;
    myCategory = category;
    myExampleUsagesBefore = exampleUsagesBefore;
    myExampleUsagesAfter = exampleUsagesAfter;
    myDescription = description;
    myDescriptionDirectoryName = null;
  }

  @Nonnull
  public TextDescriptor[] getExampleUsagesBefore() {
    if (myExampleUsagesBefore == null) {
      try {
        myExampleUsagesBefore = retrieveURLs(getDirURL(), BEFORE_TEMPLATE_PREFIX, EXAMPLE_USAGE_URL_SUFFIX);
      }
      catch (MalformedURLException e) {
        LOG.error(e);
      }
    }
    return myExampleUsagesBefore;
  }

  @Nonnull
  public TextDescriptor[] getExampleUsagesAfter() {
    if (myExampleUsagesAfter == null) {
      try {
        myExampleUsagesAfter = retrieveURLs(getDirURL(), AFTER_TEMPLATE_PREFIX, EXAMPLE_USAGE_URL_SUFFIX);
      }
      catch (MalformedURLException e) {
        LOG.error(e);
      }
    }
    return myExampleUsagesAfter;
  }

  @Nonnull
  public TextDescriptor getDescription() {
    if (myDescription == null) {
      try {
        final URL dirURL = getDirURL();
        if (dirURL == null) {
          myDescription = new PlainTextDescriptor("", "text.txt");
        }
        else {
          URL descriptionURL = new URL(dirURL.toExternalForm() + "/" + DESCRIPTION_FILE_NAME);
          myDescription = new ResourceTextDescriptor(descriptionURL);
        }
      }
      catch (MalformedURLException e) {
        LOG.error(e);
      }
    }
    return myDescription;
  }

  @Nonnull
  private TextDescriptor[] retrieveURLs(@Nullable URL descriptionDirectory, @Nonnull String prefix, @Nonnull String suffix) throws MalformedURLException {
    if (descriptionDirectory == null) {
      return new TextDescriptor[0];
    }

    List<TextDescriptor> urls = new ArrayList<>();

    IntentionMetaData intentionMetaData = myAction.getClass().getAnnotation(IntentionMetaData.class);
    if (intentionMetaData != null) {
      for (String extension : intentionMetaData.fileExtensions()) {
        for (int i = 0; ; i++) {
          URL url = new URL(descriptionDirectory.toExternalForm() + "/" + prefix + "." + extension + (i == 0 ? "" : Integer.toString(i)) + suffix);
          try {
            InputStream inputStream = url.openStream();
            inputStream.close();
            urls.add(new ResourceTextDescriptor(url));
          }
          catch (IOException ioe) {
            break;
          }
        }
      }
    }
    else {
      final FileType[] fileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
      for (FileType fileType : fileTypes) {
        final String[] extensions = FileTypeManager.getInstance().getAssociatedExtensions(fileType);
        for (String extension : extensions) {
          for (int i = 0; ; i++) {
            URL url = new URL(descriptionDirectory.toExternalForm() + "/" + prefix + "." + extension + (i == 0 ? "" : Integer.toString(i)) + suffix);
            try {
              InputStream inputStream = url.openStream();
              inputStream.close();
              urls.add(new ResourceTextDescriptor(url));
            }
            catch (IOException ioe) {
              break;
            }
          }
        }
      }
    }

    if (urls.isEmpty()) {
      String[] children;
      Exception cause = null;
      try {
        URI uri = descriptionDirectory.toURI();
        children = uri.isOpaque() ? null : ObjectUtil.notNull(new File(uri).list(), ArrayUtil.EMPTY_STRING_ARRAY);
      }
      catch (URISyntaxException | IllegalArgumentException e) {
        cause = e;
        children = null;
      }
      LOG.error("URLs not found for available file types and prefix: '" +
                prefix +
                "', suffix: '" +
                suffix +
                "';" +
                " in directory: '" +
                descriptionDirectory +
                "'" +
                (children == null ? "" : "; directory contents: " + Arrays.asList(children)), cause);
      return new TextDescriptor[0];
    }
    return urls.toArray(new TextDescriptor[urls.size()]);
  }

  @Nullable
  private static URL getIntentionDescriptionDirURL(ClassLoader aClassLoader, String intentionFolderName) {
    final URL pageURL = aClassLoader.getResource(INTENTION_DESCRIPTION_FOLDER + "/" + intentionFolderName + "/" + DESCRIPTION_FILE_NAME);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Path:" + "intentionDescriptions/" + intentionFolderName);
      LOG.debug("URL:" + pageURL);
    }
    if (pageURL != null) {
      try {
        final String url = pageURL.toExternalForm();
        return URLUtil.internProtocol(new URL(url.substring(0, url.lastIndexOf('/'))));
      }
      catch (MalformedURLException e) {
        LOG.error(e);
      }
    }
    return null;
  }

  @Nullable
  private URL getDirURL() {
    if (myDirURL == null) {
      myDirURL = getIntentionDescriptionDirURL(myAction.getClass().getClassLoader(), myDescriptionDirectoryName);
    }

    if (myDirURL == null) {
      LOG.warn("Intention Description Dir URL is null: " + myAction.getClass().getSimpleName() + "; " + myDescriptionDirectoryName);
    }
    return myDirURL;
  }

  @Nonnull
  public PluginId getPluginId() {
    return PluginManager.getPluginId(myAction.getClass());
  }

  @Nonnull
  public String getActionText() {
    String text = myAction.getText();
    if (StringUtil.isEmptyOrSpaces(text)) {
      return myAction.getClass().getName();
    }
    return text;
  }

  @Nonnull
  public IntentionAction getAction() {
    return myAction;
  }

  @Override
  public String toString() {
    return myAction.getClass().getSimpleName();
  }
}
