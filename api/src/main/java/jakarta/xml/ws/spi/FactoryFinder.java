/*
 * Copyright (c) 2005, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package jakarta.xml.ws.spi;

import java.io.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.xml.ws.WebServiceException;

class FactoryFinder {

    private static final Logger logger;

    private static final ServiceLoaderUtil.ExceptionHandler<WebServiceException> EXCEPTION_HANDLER =
            new ServiceLoaderUtil.ExceptionHandler<WebServiceException>() {
                @Override
                public WebServiceException createException(Throwable throwable, String message) {
                    return new WebServiceException(message, throwable);
                }
            };

    private final static PrivilegedAction<String> propertyAction = () -> System.getProperty("jax-ws.debug");

    static {
        logger = Logger.getLogger("jakarta.xml.ws");
        try {
            if (AccessController.doPrivileged(propertyAction) != null) {
                // disconnect the logger from a bigger framework (if any)
                // and take the matters into our own hands
                logger.setUseParentHandlers(false);
                logger.setLevel(Level.ALL);
                ConsoleHandler handler = new ConsoleHandler();
                handler.setLevel(Level.ALL);
                logger.addHandler(handler);
            } else {
                // don't change the setting of this logger
                // to honor what other frameworks
                // have done on configurations.
            }
        } catch (Throwable t) {
            // just to be extra safe. in particular System.getProperty may throw
            // SecurityException.
            logger.log(Level.SEVERE, "Exception during loading the class", t);
        }
    }
    /**
     * Finds the implementation {@code Class} object for the given
     * factory name, or if that fails, finds the {@code Class} object
     * for the given fallback class name. The arguments supplied MUST be
     * used in order. If using the first argument is successful, the second
     * one will not be used.
     * <P>
     * This method is package private so that this code can be shared.
     *
     * @return the {@code Class} object of the specified message factory;
     *         may not be {@code null}
     *
     * @param factoryClass          the name of the factory to find, which is
     *                              a system property
     * @param fallbackClassName     the implementation class name, which is
     *                              to be used only if nothing else
     *                              is found; {@code null} to indicate that
     *                              there is no fallback class name
     * @exception WebServiceException if there is an error
     */
    @SuppressWarnings("unchecked")
    static <T> T find(Class<T> factoryClass, String fallbackClassName) {
        ClassLoader classLoader = ServiceLoaderUtil.contextClassLoader(EXCEPTION_HANDLER);

        T provider = ServiceLoaderUtil.firstByServiceLoader(factoryClass, logger, EXCEPTION_HANDLER);
        if (provider != null) return provider;

        String factoryId = factoryClass.getName();

        // try to read from $java.home/lib/jaxws.properties
        provider = (T) fromJDKProperties(factoryId, fallbackClassName, classLoader);
        if (provider != null) return provider;

        // Use the system property
        provider = (T) fromSystemProperty(factoryId, fallbackClassName, classLoader);
        if (provider != null) return provider;

        // handling Glassfish (platform specific default)
        if (isOsgi()) {
            provider = (T) lookupUsingOSGiServiceLoader(factoryId);
            if (provider != null) {
                return provider;
            }
        }

        if (fallbackClassName == null) {
            throw new WebServiceException(
                "Provider for " + factoryId + " cannot be found", null);
        }

        return (T) ServiceLoaderUtil.newInstance(fallbackClassName,
                fallbackClassName, classLoader, EXCEPTION_HANDLER);
    }

    private static Object fromSystemProperty(String factoryId,
                                             String fallbackClassName,
                                             ClassLoader classLoader) {
        try {
            String systemProp = System.getProperty(factoryId);
            if (systemProp != null) {
                logger.log(Level.FINE, "Found system property {0}", systemProp);
                return newInstance(systemProp, fallbackClassName, classLoader);
            }
        } catch (SecurityException ignored) {
            logger.log(Level.SEVERE, "Access is not allowed to the system property with key " + factoryId, ignored);
        }
        return null;
    }

    private static Object fromJDKProperties(String factoryId,
                                            String fallbackClassName,
                                            ClassLoader classLoader) {
        Path path = null;
        try {
            String JAVA_HOME = System.getProperty("java.home");
            path = Paths.get(JAVA_HOME, "conf", "jaxws.properties");

            // to ensure backwards compatibility
            if (!Files.exists(path)) {
                path = Paths.get(JAVA_HOME, "lib", "jaxws.properties");
            }

            if (Files.exists(path)) {
                logger.log(Level.FINE, "Found {0}", path);
                Properties props = new Properties();
                try (InputStream inStream = Files.newInputStream(path)) {
                    props.load(inStream);
                }
                String factoryClassName = props.getProperty(factoryId);
                return newInstance(factoryClassName, fallbackClassName, classLoader);
            }
        } catch (Exception ignored) {
            logger.log(Level.SEVERE, "Error reading Jakarta XML Web Services configuration from ["  + path +
                    "] file. Check it is accessible and has correct format.", ignored);
        }
        return null;
    }

    private static final String OSGI_SERVICE_LOADER_CLASS_NAME = "org.glassfish.hk2.osgiresourcelocator.ServiceLoader";

    private static boolean isOsgi() {
        try {
            Class.forName(OSGI_SERVICE_LOADER_CLASS_NAME);
            return true;
        } catch (ClassNotFoundException ignored) {
            logger.log(
                    Level.SEVERE,
                    "Class " + OSGI_SERVICE_LOADER_CLASS_NAME + " cannot be loaded",
                    ignored
            );
        }
        return false;
    }

    private static Object lookupUsingOSGiServiceLoader(String factoryId) {
        try {
            logger.fine("Trying to create the provider from the OSGi ServiceLoader");
            // Use reflection to avoid having any dependendcy on ServiceLoader class
            Class serviceClass = Class.forName(factoryId);
            Class[] args = new Class[]{serviceClass};
            Class target = Class.forName(OSGI_SERVICE_LOADER_CLASS_NAME);
            java.lang.reflect.Method m = target.getMethod("lookupProviderInstances", Class.class);
            java.util.Iterator iter = ((Iterable) m.invoke(null, (Object[]) args)).iterator();
            return iter.hasNext() ? iter.next() : null;
        } catch (Exception ignored) {
            // log and continue
            logger.log(
                    Level.SEVERE,
                    "Access to the system property with key " + factoryId + " is not allowed",
                    ignored
            );
            return null;
        }
    }

    private static Object newInstance(String className, String defaultImplClassName, ClassLoader classLoader){
        Object newInstance = ServiceLoaderUtil.newInstance(className,
                defaultImplClassName, classLoader, EXCEPTION_HANDLER);
        if (logger.isLoggable(Level.FINE)) {
            // extra check to avoid costly which operation if not logged
            Class<?> newInstanceClass = newInstance.getClass();
            logger.log(
                    Level.FINE,
                    "loaded {0} from {1}", new Object[]{newInstanceClass.getName(), which(newInstanceClass)}
            );
        }
        return newInstance;
    }

    /**
     * Get the URL for the Class from it's ClassLoader.
     *
     * Convenience method for {@link #which(Class, ClassLoader)}.
     *
     * Equivalent to calling: which(clazz, clazz.getClassLoader())
     *
     * @param clazz
     *          The class to search for
     * @return
     *          the URL for the class or null if it wasn't found
     */
    static URL which(Class clazz) {
        return which(clazz, getClassClassLoader(clazz));
    }

    /**
     * Search the given ClassLoader for an instance of the specified class and
     * return a string representation of the URL that points to the resource.
     *
     * @param clazz
     *          The class to search for
     * @param loader
     *          The ClassLoader to search.  If this parameter is null, then the
     *          system class loader will be searched
     * @return
     *          the URL for the class or null if it wasn't found
     */
    static URL which(Class clazz, ClassLoader loader) {

        String classnameAsResource = clazz.getName().replace('.', '/') + ".class";

        if (loader == null) {
            loader = getSystemClassLoader();
        }

        return loader.getResource(classnameAsResource);
    }

    private static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return (ClassLoader) java.security.AccessController.doPrivileged(
                    (PrivilegedAction) ClassLoader::getSystemClassLoader);
        }
    }

    @SuppressWarnings("unchecked")
    private static ClassLoader getClassClassLoader(final Class c) {
        if (System.getSecurityManager() == null) {
            return c.getClassLoader();
        } else {
            return (ClassLoader) java.security.AccessController.doPrivileged(
                    (PrivilegedAction) c::getClassLoader);
        }
    }

}
