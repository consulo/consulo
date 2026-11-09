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
package consulo.ide.impl.idea.openapi.paths;

import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.http.HttpMethod;
import consulo.http.HttpRequestBuilderFactory;
import consulo.http.HttpStatusException;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.ExternalAnnotator;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.impl.psi.path.WebReference;
import consulo.language.psi.PsiAnchor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.path.PsiDynaReference;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class WebReferencesAnnotatorBase extends ExternalAnnotator<WebReferencesAnnotatorBase.MyInfo[], WebReferencesAnnotatorBase.MyInfo[]> {
  private static final Logger LOG = Logger.getInstance(WebReferencesAnnotatorBase.class);
  private static final Set<Integer> NON_EXISTENT_RESOURCE_STATUS_CODES = Set.of(HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_GONE);

  private final Map<String, MyFetchCacheEntry> myFetchCache = new HashMap<String, MyFetchCacheEntry>();
  private final Object myFetchCacheLock = new Object();
  private static final long FETCH_CACHE_TIMEOUT = 10000;

  protected static final WebReference[] EMPTY_ARRAY = new WebReference[0];

  
  protected abstract WebReference[] collectWebReferences(PsiFile file);

  protected static @Nullable WebReference lookForWebReference(PsiElement element) {
    return lookForWebReference(Arrays.asList(element.getReferences()));
  }

  @SuppressWarnings("unchecked")
  private static @Nullable WebReference lookForWebReference(Collection<PsiReference> references) {
    for (PsiReference reference : references) {
      if (reference instanceof WebReference) {
        return (WebReference)reference;
      }
      else if (reference instanceof PsiDynaReference) {
        WebReference webReference = lookForWebReference(((PsiDynaReference)reference).getReferences());
        if (webReference != null) {
          return webReference;
        }
      }
    }
    return null;
  }

  @Override
  public MyInfo[] collectInformation(PsiFile file) {
    WebReference[] references = collectWebReferences(file);
    MyInfo[] infos = new MyInfo[references.length];

    for (int i = 0; i < infos.length; i++) {
      WebReference reference = references[i];
      infos[i] = new MyInfo(PsiAnchor.create(reference.getElement()), reference.getRangeInElement(), reference.getValue());
    }
    return infos;
  }

  @Override
  public MyInfo[] doAnnotate(MyInfo[] infos) {
    MyFetchResult[] fetchResults = new MyFetchResult[infos.length];
    for (int i = 0; i < fetchResults.length; i++) {
      fetchResults[i] = checkUrl(infos[i].myUrl);
    }

    boolean containsAvailableHosts = false;

    for (MyFetchResult fetchResult : fetchResults) {
      if (fetchResult != MyFetchResult.UNKNOWN_HOST) {
        containsAvailableHosts = true;
      }
    }

    for (int i = 0; i < fetchResults.length; i++) {
      MyFetchResult result = fetchResults[i];

      // if all hosts are not available, internet connection may be disabled, so it's better to not report warnings for unknown hosts
      if (result == MyFetchResult.OK || (!containsAvailableHosts && result == MyFetchResult.UNKNOWN_HOST)) {
        infos[i].myResult = true;
      }
    }

    return infos;
  }

  @Override
  public void apply(PsiFile file, MyInfo[] infos, AnnotationHolder holder) {
    if (infos.length == 0) {
      return;
    }

    HighlightDisplayLevel displayLevel = getHighlightDisplayLevel(file);

    for (MyInfo info : infos) {
      if (!info.myResult) {
        PsiElement element = info.myAnchor.retrieve();
        if (element != null) {
          int start = element.getTextRange().getStartOffset();
          TextRange range = new TextRange(start + info.myRangeInElement.getStartOffset(), start + info.myRangeInElement.getEndOffset());
          String message = getErrorMessage(info.myUrl);

          Annotation annotation;

          if (displayLevel == HighlightDisplayLevel.ERROR) {
            annotation = holder.createErrorAnnotation(range, message);
          }
          else if (displayLevel == HighlightDisplayLevel.WARNING) {
            annotation = holder.createWarningAnnotation(range, message);
          }
          else if (displayLevel == HighlightDisplayLevel.WEAK_WARNING) {
            annotation = holder.createInfoAnnotation(range, message);
          }
          else {
            annotation = holder.createWarningAnnotation(range, message);
          }

          for (IntentionAction action : getQuickFixes()) {
            annotation.registerFix(action);
          }
        }
      }
    }
  }

  
  protected abstract String getErrorMessage(String url);

  
  protected IntentionAction[] getQuickFixes() {
    return IntentionAction.EMPTY_ARRAY;
  }

  
  protected abstract HighlightDisplayLevel getHighlightDisplayLevel(PsiElement context);

  
  private MyFetchResult checkUrl(String url) {
    synchronized (myFetchCacheLock) {
      MyFetchCacheEntry entry = myFetchCache.get(url);
      long currentTime = System.currentTimeMillis();

      if (entry != null && currentTime - entry.getTime() < FETCH_CACHE_TIMEOUT) {
        return entry.getFetchResult();
      }

      MyFetchResult fetchResult = doCheckUrl(url);
      myFetchCache.put(url, new MyFetchCacheEntry(currentTime, fetchResult));
      return fetchResult;
    }
  }

  private static MyFetchResult doCheckUrl(String url) {
    if (url.startsWith("mailto")) {
      return MyFetchResult.OK;
    }

    try {
      Application.get().getInstance(HttpRequestBuilderFactory.class)
        .newBuilder(url, HttpMethod.GET)
        .productNameAsUserAgent()
        .connectTimeout(3000)
        .readTimeout(3000)
        .tryConnect();
    }
    catch (UnknownHostException e) {
      LOG.info(e);
      return MyFetchResult.UNKNOWN_HOST;
    }
    catch (HttpStatusException e) {
      LOG.info(e);
      return NON_EXISTENT_RESOURCE_STATUS_CODES.contains(e.getStatusCode()) ? MyFetchResult.NONEXISTENCE : MyFetchResult.OK;
    }
    catch (IOException e) {
      LOG.info(e);
    }
    catch (IllegalArgumentException e) {
      LOG.debug(e);
    }
    return MyFetchResult.OK;
  }

  private static class MyFetchCacheEntry {
    private final long myTime;
    private final MyFetchResult myFetchResult;

    private MyFetchCacheEntry(long time, MyFetchResult fetchResult) {
      myTime = time;
      myFetchResult = fetchResult;
    }

    public long getTime() {
      return myTime;
    }

    
    public MyFetchResult getFetchResult() {
      return myFetchResult;
    }
  }

  private static enum MyFetchResult {
    OK,
    UNKNOWN_HOST,
    NONEXISTENCE
  }

  protected static class MyInfo {
    final PsiAnchor myAnchor;
    final String myUrl;
    final TextRange myRangeInElement;

    volatile boolean myResult;

    private MyInfo(PsiAnchor anchor, TextRange rangeInElement, String url) {
      myAnchor = anchor;
      myRangeInElement = rangeInElement;
      myUrl = url;
    }
  }
}
