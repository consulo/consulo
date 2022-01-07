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
package com.intellij.openapi.paths;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import consulo.logging.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import java.util.HashMap;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class WebReferencesAnnotatorBase extends ExternalAnnotator<WebReferencesAnnotatorBase.MyInfo[], WebReferencesAnnotatorBase.MyInfo[]> {
  private static final Logger LOG = Logger.getInstance(WebReferencesAnnotatorBase.class);

  private final Map<String, MyFetchCacheEntry> myFetchCache = new HashMap<String, MyFetchCacheEntry>();
  private final Object myFetchCacheLock = new Object();
  private static final long FETCH_CACHE_TIMEOUT = 10000;

  protected static final WebReference[] EMPTY_ARRAY = new WebReference[0];

  @Nonnull
  protected abstract WebReference[] collectWebReferences(@Nonnull PsiFile file);

  @Nullable
  protected static WebReference lookForWebReference(@Nonnull PsiElement element) {
    return lookForWebReference(Arrays.asList(element.getReferences()));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private static WebReference lookForWebReference(Collection<PsiReference> references) {
    for (PsiReference reference : references) {
      if (reference instanceof WebReference) {
        return (WebReference)reference;
      }
      else if (reference instanceof PsiDynaReference) {
        final WebReference webReference = lookForWebReference(((PsiDynaReference)reference).getReferences());
        if (webReference != null) {
          return webReference;
        }
      }
    }
    return null;
  }

  @Override
  public MyInfo[] collectInformation(@Nonnull PsiFile file) {
    final WebReference[] references = collectWebReferences(file);
    final MyInfo[] infos = new MyInfo[references.length];

    for (int i = 0; i < infos.length; i++) {
      final WebReference reference = references[i];
      infos[i] = new MyInfo(PsiAnchor.create(reference.getElement()), reference.getRangeInElement(), reference.getValue());
    }
    return infos;
  }

  @Override
  public MyInfo[] doAnnotate(MyInfo[] infos) {
    final MyFetchResult[] fetchResults = new MyFetchResult[infos.length];
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
      final MyFetchResult result = fetchResults[i];

      // if all hosts are not available, internet connection may be disabled, so it's better to not report warnings for unknown hosts
      if (result == MyFetchResult.OK || (!containsAvailableHosts && result == MyFetchResult.UNKNOWN_HOST)) {
        infos[i].myResult = true;
      }
    }

    return infos;
  }

  @Override
  public void apply(@Nonnull PsiFile file, MyInfo[] infos, @Nonnull AnnotationHolder holder) {
    if (infos.length == 0) {
      return;
    }

    final HighlightDisplayLevel displayLevel = getHighlightDisplayLevel(file);

    for (MyInfo info : infos) {
      if (!info.myResult) {
        final PsiElement element = info.myAnchor.retrieve();
        if (element != null) {
          final int start = element.getTextRange().getStartOffset();
          final TextRange range = new TextRange(start + info.myRangeInElement.getStartOffset(), start + info.myRangeInElement.getEndOffset());
          final String message = getErrorMessage(info.myUrl);

          final Annotation annotation;

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

  @Nonnull
  protected abstract String getErrorMessage(@Nonnull String url);

  @Nonnull
  protected IntentionAction[] getQuickFixes() {
    return IntentionAction.EMPTY_ARRAY;
  }

  @Nonnull
  protected abstract HighlightDisplayLevel getHighlightDisplayLevel(@Nonnull PsiElement context);

  @Nonnull
  private MyFetchResult checkUrl(String url) {
    synchronized (myFetchCacheLock) {
      final MyFetchCacheEntry entry = myFetchCache.get(url);
      final long currentTime = System.currentTimeMillis();

      if (entry != null && currentTime - entry.getTime() < FETCH_CACHE_TIMEOUT) {
        return entry.getFetchResult();
      }

      final MyFetchResult fetchResult = doCheckUrl(url);
      myFetchCache.put(url, new MyFetchCacheEntry(currentTime, fetchResult));
      return fetchResult;
    }
  }

  private static MyFetchResult doCheckUrl(String url) {
    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
    requestConfigBuilder.setConnectTimeout(3000);
    requestConfigBuilder.setSocketTimeout(3000);
    requestConfigBuilder.setConnectionRequestTimeout(3000);
    requestConfigBuilder.setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY);

    try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfigBuilder.build()).build()) {
      return httpClient.execute(new HttpGet(url), response -> {
        int code = response.getStatusLine().getStatusCode();
        return code == HttpStatus.SC_OK || code == HttpStatus.SC_REQUEST_TIMEOUT ? MyFetchResult.OK : MyFetchResult.NONEXISTENCE;
      });
    }
    catch (UnknownHostException e) {
      LOG.info(e);
      return MyFetchResult.UNKNOWN_HOST;
    }
    catch (IOException e) {
      LOG.info(e);
      return MyFetchResult.OK;
    }
    catch (IllegalArgumentException e) {
      LOG.debug(e);
      return MyFetchResult.OK;
    }
  }

  private static class MyFetchCacheEntry {
    private final long myTime;
    private final MyFetchResult myFetchResult;

    private MyFetchCacheEntry(long time, @Nonnull MyFetchResult fetchResult) {
      myTime = time;
      myFetchResult = fetchResult;
    }

    public long getTime() {
      return myTime;
    }

    @Nonnull
    public MyFetchResult getFetchResult() {
      return myFetchResult;
    }
  }

  private static enum MyFetchResult {
    OK, UNKNOWN_HOST, NONEXISTENCE
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
