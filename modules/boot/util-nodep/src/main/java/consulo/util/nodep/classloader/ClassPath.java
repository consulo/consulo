/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.util.nodep.classloader;

import consulo.util.nodep.LoggerRt;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;

public class ClassPath {
    private static final ResourceStringLoaderIterator ourResourceIterator = new ResourceStringLoaderIterator();
    private static final LoaderCollector ourLoaderCollector = new LoaderCollector();
    public static final String CLASSPATH_JAR_FILE_NAME_PREFIX = "classpath";

    private final Deque<URL> myUrls = new ArrayDeque<>();
    private final List<Loader> myLoaders = new ArrayList<>();

    private volatile boolean myAllUrlsWereProcessed;

    private final AtomicInteger myLastLoaderProcessed = new AtomicInteger();
    private final Map<URL, Loader> myLoadersMap = new HashMap<>();
    private final ClasspathCache myCache = new ClasspathCache();
    private final Set<URL> myURLsWithProtectionDomain;

    final boolean myCanLockJars; // true implies that the .jar file will not be modified in the lifetime of the JarLoader
    private final boolean myCanUseCache;
    private final boolean myAcceptUnescapedUrls;
    final boolean myPreloadJarContents;
    final boolean myCanHavePersistentIndex;
    private final Map<URL, Set<String>> myUrlsIndex;
    final boolean myLazyClassloadingCaches;
    final boolean myEnableJarIndex;
    private final CachePoolImpl myCachePool;
    private final UrlClassLoader.CachingCondition myCachingCondition;
    final boolean myLogErrorOnMissingJar;

    private final Set<String> myFullJarIndex;

    public ClassPath(List<URL> urls,
                     Map<URL, Set<String>> urlsIndex,
                     boolean canLockJars,
                     boolean canUseCache,
                     boolean acceptUnescapedUrls,
                     boolean preloadJarContents,
                     boolean canHavePersistentIndex,
                     boolean enableJarIndex,
                     CachePoolImpl cachePool,
                     UrlClassLoader.CachingCondition cachingCondition,
                     boolean logErrorOnMissingJar,
                     boolean lazyClassloadingCaches,
                     Set<URL> urlsWithProtectionDomain) {
        myUrlsIndex = urlsIndex;
        myLazyClassloadingCaches = lazyClassloadingCaches;
        myCanLockJars = canLockJars;
        myCanUseCache = canUseCache && !myLazyClassloadingCaches;
        myAcceptUnescapedUrls = acceptUnescapedUrls;
        myPreloadJarContents = preloadJarContents;
        myEnableJarIndex = enableJarIndex;
        myCachePool = cachePool;
        myCachingCondition = cachingCondition;
        myCanHavePersistentIndex = canHavePersistentIndex;
        myLogErrorOnMissingJar = logErrorOnMissingJar;
        myURLsWithProtectionDomain = urlsWithProtectionDomain;

        if (urlsIndex != null) {
            myFullJarIndex = new HashSet<>();

            for (Set<String> value : urlsIndex.values()) {
                myFullJarIndex.addAll(value);
            }
        }
        else {
            myFullJarIndex = null;
        }

        push(urls);
    }

    public void close() throws Exception {
        synchronized (myLoaders) {
            for (int i = 0; i < myLoaders.size(); i++) {
                Loader loader = myLoaders.get(i);
                if (loader == null) {
                    continue;
                }

                loader.close();
            }
        }
    }

    /**
     * @deprecated Adding additional urls to classpath at runtime could lead to hard-to-debug errors
     */
    @Deprecated
    void addURL(URL url) {
        push(Collections.singletonList(url));
    }

    private void push(List<URL> urls) {
        if (!urls.isEmpty()) {
            synchronized (myUrls) {
                for (int i = urls.size() - 1; i >= 0; i--) {
                    myUrls.push(urls.get(i));
                }
                myAllUrlsWereProcessed = false;
            }
        }
    }

    public Resource getResource(String resourcePath) {
        long started = startTiming();
        try {
            // if we have full jar index, and its not our resource - skip fully
            if (myFullJarIndex != null && !myFullJarIndex.contains(resourcePath)) {
                return null;
            }

            String shortName = ClasspathCache.transformName(resourcePath);

            int i;
            if (myCanUseCache) {
                boolean allUrlsWereProcessed = myAllUrlsWereProcessed;
                i = allUrlsWereProcessed ? 0 : myLastLoaderProcessed.get();

                Resource prevResource = myCache.iterateLoaders(resourcePath, ourResourceIterator, resourcePath, this, shortName);
                if (prevResource != null || allUrlsWereProcessed) {
                    return prevResource;
                }
            }
            else {
                i = 0;
            }

            Loader loader;
            while ((loader = getLoader(i++)) != null) {
                if (!loader.containsPath(resourcePath)) {
                    continue;
                }

                if (myCanUseCache) {
                    if (!loader.containsName(resourcePath, shortName)) {
                        continue;
                    }
                }
                Resource resource = loader.getResource(resourcePath);
                if (resource != null) {
                    return resource;
                }
            }
        }
        finally {
            logTiming(this, started, resourcePath);
        }

        return null;
    }

    public Enumeration<URL> getResources(String name) {
        return new MyEnumeration(name);
    }

    private Loader getLoader(int i) {
        if (i < myLastLoaderProcessed.get()) { // volatile read
            return myLoaders.get(i);
        }

        return getLoaderSlowPath(i);
    }

    private synchronized Loader getLoaderSlowPath(int i) {
        while (myLoaders.size() < i + 1) {
            URL url;
            synchronized (myUrls) {
                if (myUrls.isEmpty()) {
                    if (myCanUseCache) {
                        myAllUrlsWereProcessed = true;
                    }

                    if (myUrlsIndex != null) {
                        myAllUrlsWereProcessed = true;
                    }

                    return null;
                }
                url = myUrls.pop();
            }

            if (myLoadersMap.containsKey(url)) {
                continue;
            }

            try {
                initLoaders(url, myLoaders.size());
            }
            catch (IOException e) {
                LoggerRt.getInstance(ClassPath.class).info("url: " + url, e);
            }
        }

        return myLoaders.get(i);
    }

    public List<URL> getBaseUrls() {
        List<URL> result = new ArrayList<>();
        for (Loader loader : myLoaders) {
            result.add(loader.getBaseURL());
        }
        return result;
    }

    private void initLoaders(URL url, int index) throws IOException {
        String path;

        if (myAcceptUnescapedUrls) {
            path = url.getFile();
        }
        else {
            try {
                path = url.toURI().getSchemeSpecificPart();
            }
            catch (URISyntaxException e) {
                LoggerRt.getInstance(ClassPath.class).error("url: " + url, e);
                path = url.getFile();
            }
        }

        if (path != null && "file".equals(url.getProtocol())) {
            File file = new File(path);
            Loader loader = createLoader(url, index, file, file.getName().startsWith(CLASSPATH_JAR_FILE_NAME_PREFIX));
            if (loader != null) {
                initLoader(url, loader);
            }
        }
    }

    private Loader createLoader(URL url, int index, File file, boolean processRecursively) throws IOException {
        if (file.isDirectory()) {
            return new FileLoader(url, index, this);
        }
        if (file.isFile()) {
            JarLoader loader;

            Set<String> jarIndex = null;
            if (myUrlsIndex != null) {
                jarIndex = myUrlsIndex.get(url);
            }

            if (myURLsWithProtectionDomain.contains(url)) {
                loader = new SecureJarLoader(url, index, this, jarIndex);
            }
            else {
                loader = new JarLoader(url, index, this, jarIndex);
            }
            if (processRecursively) {
                String[] referencedJars = loadManifestClasspath(loader);
                if (referencedJars != null) {
                    long s2 = ourLogTiming ? System.nanoTime() : 0;
                    List<URL> urls = new ArrayList<>(referencedJars.length);
                    for (String referencedJar : referencedJars) {
                        try {
                            urls.add(UrlClassLoader.internProtocol(new URI(referencedJar).toURL()));
                        }
                        catch (Exception e) {
                            LoggerRt.getInstance(ClassPath.class).warn("url: " + url + " / " + referencedJar, e);
                        }
                    }
                    push(urls);
                    if (ourLogTiming) {
                        System.out.println("Loaded all " + referencedJars.length + " urls " + (System.nanoTime() - s2) / 1000000 + "ms");
                    }
                }
            }
            return loader;
        }
        return null;
    }

    private void initLoader(URL url, Loader loader) throws IOException {
        if (myCanUseCache) {
            ClasspathCache.LoaderData data = myCachePool == null ? null : myCachePool.getCachedData(url);
            if (data == null) {
                data = loader.buildData();
                if (myCachePool != null && myCachingCondition != null && myCachingCondition.shouldCacheData(url)) {
                    myCachePool.cacheData(url, data);
                }
            }
            myCache.applyLoaderData(data, loader);

            boolean lastOne;
            synchronized (myUrls) {
                lastOne = myUrls.isEmpty();
            }

            if (lastOne) {
                myAllUrlsWereProcessed = true;
            }
        }
        myLoaders.add(loader);
        myLoadersMap.put(url, loader);
        myLastLoaderProcessed.incrementAndGet(); // volatile write
    }

    Attributes getManifestData(URL url) {
        return myCanUseCache && myCachePool != null ? myCachePool.getManifestData(url) : null;
    }

    void cacheManifestData(URL url, Attributes manifestAttributes) {
        if (myCanUseCache && myCachePool != null && myCachingCondition != null && myCachingCondition.shouldCacheData(url)) {
            myCachePool.cacheManifestData(url, manifestAttributes);
        }
    }

    private class MyEnumeration implements Enumeration<URL> {
        private static final int DEFAULT_IMPL = 0;
        private static final int CACHE = 1;
        private static final int FULL_INDEX = 2;

        private int myType = DEFAULT_IMPL;

        private int myIndex;
        private Resource myRes;
        private final String myName;
        private final String myShortName;
        private final List<Loader> myLoaders;

        MyEnumeration(String name) {
            myName = name;
            myShortName = ClasspathCache.transformName(name);
            List<Loader> loaders = null;

            if (myCanUseCache && myAllUrlsWereProcessed) {
                myType = CACHE;

                Collection<Loader> loadersSet = new LinkedHashSet<>();
                myCache.iterateLoaders(name, ourLoaderCollector, loadersSet, this, myShortName);

                if (name.endsWith("/")) {
                    myCache.iterateLoaders(name.substring(0, name.length() - 1), ourLoaderCollector, loadersSet, this, myShortName);
                }
                else {
                    myCache.iterateLoaders(name + "/", ourLoaderCollector, loadersSet, this, myShortName);
                }

                loaders = new ArrayList<>(loadersSet);
            }

            if (myUrlsIndex != null && myAllUrlsWereProcessed) {
                myType = FULL_INDEX;

                Collection<Loader> loadersSet = new LinkedHashSet<>();
                Loader loader;
                int i = 0;
                while ((loader = getLoader(i++)) != null) {
                    if (loader.containsPath(myName) || loader.containsPackage(myName)) {
                        loadersSet.add(loader);
                    }
                }

                loaders = new ArrayList<>(loadersSet);
            }

            myLoaders = loaders;
        }

        private boolean next() {
            if (myRes != null) {
                return true;
            }

            long started = startTiming();
            try {
                Loader loader;
                switch (myType) {
                    case DEFAULT_IMPL:
                        while ((loader = getLoader(myIndex++)) != null) {
                            if (myCanUseCache && !loader.containsName(myName, myShortName)) {
                                myRes = null;
                                continue;
                            }

                            myRes = loader.getResource(myName);
                            if (myRes != null) {
                                return true;
                            }
                        }
                        break;
                    case CACHE:
                        while (myIndex < myLoaders.size()) {
                            loader = myLoaders.get(myIndex++);
                            if (!loader.containsName(myName, myShortName)) {
                                myRes = null;
                                continue;
                            }
                            myRes = loader.getResource(myName);
                            if (myRes != null) {
                                return true;
                            }
                        }
                        break;
                    case FULL_INDEX:
                        while (myIndex < myLoaders.size()) {
                            loader = myLoaders.get(myIndex++);

                            myRes = loader.getResource(myName);
                            if (myRes != null) {
                                return true;
                            }
                        }
                        break;
                }
            }
            finally {
                logTiming(ClassPath.this, started, myName);
            }

            return false;
        }

        @Override
        public boolean hasMoreElements() {
            return next();
        }

        @Override
        public URL nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            else {
                Resource resource = myRes;
                myRes = null;
                return resource.getURL();
            }
        }
    }

    private static class ResourceStringLoaderIterator extends ClasspathCache.LoaderIterator<Resource, String, ClassPath> {
        @Override
        Resource process(Loader loader, String s, ClassPath classPath, String shortName) {
            if (!loader.containsName(s, shortName)) {
                return null;
            }
            Resource resource = loader.getResource(s);
            if (resource != null && ourResourceLoadingLogger != null) {
                long resourceSize;
                try {
                    resourceSize = resource instanceof MemoryResource ? resource.getBytes().length : -1;
                }
                catch (IOException e) {
                    resourceSize = -1;
                }
                ourResourceLoadingLogger.logResource(s, loader.getBaseURL(), resourceSize);
            }
            return resource;
        }
    }

    private static class LoaderCollector extends ClasspathCache.LoaderIterator<Object, Collection<Loader>, Object> {
        @Override
        Object process(Loader loader, Collection<Loader> parameter, Object parameter2, String shortName) {
            parameter.add(loader);
            return null;
        }
    }

    public interface ResourceLoadingLogger {
        void logResource(String url, URL baseLoaderURL, long resourceSize);
    }

    private static final ResourceLoadingLogger ourResourceLoadingLogger;

    static {
        String className = System.getProperty("intellij.class.resources.loading.logger");
        ResourceLoadingLogger resourceLoadingLogger = null;
        if (className != null) {
            try {
                resourceLoadingLogger = (ResourceLoadingLogger) Class.forName(className).newInstance();
            }
            catch (Throwable e) {
                LoggerRt.getInstance(ClassPath.class).error("Failed to instantiate resource loading logger " + className, e);
            }
        }
        ourResourceLoadingLogger = resourceLoadingLogger;
    }

    static final boolean ourLogTiming = Boolean.getBoolean("idea.print.classpath.timing");
    private static final AtomicLong ourTotalTime = new AtomicLong();
    private static final AtomicInteger ourTotalRequests = new AtomicInteger();
    private static final ThreadLocal<Boolean> ourDoingTiming = new ThreadLocal<>();

    private static long startTiming() {
        if (!ourLogTiming) {
            return 0;
        }
        if (ourDoingTiming.get() != null) {
            return 0;
        }
        ourDoingTiming.set(Boolean.TRUE);
        return System.nanoTime();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void logTiming(ClassPath path, long started, String msg) {
        if (!ourLogTiming) {
            return;
        }
        if (started == 0) {
            return;
        }
        ourDoingTiming.set(null);

        long time = System.nanoTime() - started;
        long totalTime = ourTotalTime.addAndGet(time);
        int totalRequests = ourTotalRequests.incrementAndGet();
        if (time > 10000000L) {
            System.out.println(time / 1000000 + " ms for " + msg);
        }
        if (totalRequests % 10000 == 0) {
            System.out.println(path.getClass().getClassLoader() + ", requests:" + ourTotalRequests + ", time:" + (totalTime / 1000000) + "ms");
        }
    }

    static {
        if (ourLogTiming) {
            Runtime.getRuntime().addShutdownHook(new Thread("Shutdown hook for tracing classloading information") {
                @Override
                public void run() {
                    System.out.println("Classloading requests:" + ClassPath.class.getClassLoader() + "," + ourTotalRequests + ", time:" + (ourTotalTime
                        .get() / 1000000) + "ms");
                }
            });
        }
    }

    private static String[] loadManifestClasspath(JarLoader loader) {
        try {
            String classPath = loader.getClassPathManifestAttribute();

            if (classPath != null) {
                String[] urls = classPath.split(" ");
                if (urls.length > 0 && urls[0].startsWith("file:")) {
                    return urls;
                }
            }
        }
        catch (Exception ignore) {
        }
        return null;
    }
}