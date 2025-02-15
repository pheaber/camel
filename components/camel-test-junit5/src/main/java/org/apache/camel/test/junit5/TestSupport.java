/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.test.junit5;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Route;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.PredicateAssertHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bunch of useful testing methods
 */
public abstract class TestSupport {

    protected static final String LS = System.lineSeparator();
    private static final Logger LOG = LoggerFactory.getLogger(TestSupport.class);
    protected Logger log = LoggerFactory.getLogger(getClass());

    // Builder methods for expressions used when testing
    // -------------------------------------------------------------------------

    /**
     * Returns a value builder for the given header
     */
    public static ValueBuilder header(String name) {
        return Builder.header(name);
    }

    /**
     * Returns a value builder for the given exchange property
     */
    public static ValueBuilder exchangeProperty(String name) {
        return Builder.exchangeProperty(name);
    }

    /**
     * Returns a predicate and value builder for the inbound body on an exchange
     */
    public static ValueBuilder body() {
        return Builder.body();
    }

    /**
     * Returns a predicate and value builder for the inbound message body as a
     * specific type
     */
    public static <T> ValueBuilder bodyAs(Class<T> type) {
        return Builder.bodyAs(type);
    }

    /**
     * Returns a value builder for the given system property
     */
    public static ValueBuilder systemProperty(String name) {
        return Builder.systemProperty(name);
    }

    /**
     * Returns a value builder for the given system property
     */
    public static ValueBuilder systemProperty(String name, String defaultValue) {
        return Builder.systemProperty(name, defaultValue);
    }

    // Assertions
    // -----------------------------------------------------------------------

    public static <T> T assertIsInstanceOf(Class<T> expectedType, Object value) {
        assertNotNull(value, "Expected an instance of type: " + expectedType.getName() + " but was null");
        assertTrue(expectedType.isInstance(value), "Object should be of type " + expectedType.getName() + " but was: " + value + " with the type: " + value.getClass().getName());
        return expectedType.cast(value);
    }

    public static void assertEndpointUri(Endpoint endpoint, String uri) {
        assertNotNull(endpoint, "Endpoint is null when expecting endpoint for: " + uri);
        assertEquals(endpoint.getEndpointUri(), "Endpoint uri for: " + endpoint, uri);
    }

    /**
     * Asserts the In message on the exchange contains the expected value
     */
    public static Object assertInMessageHeader(Exchange exchange, String name, Object expected) {
        return assertMessageHeader(exchange.getIn(), name, expected);
    }

    /**
     * Asserts the Out message on the exchange contains the expected value
     */
    public static Object assertOutMessageHeader(Exchange exchange, String name, Object expected) {
        return assertMessageHeader(exchange.getOut(), name, expected);
    }

    /**
     * Asserts that the given exchange has an OUT message of the given body
     * value
     *
     * @param exchange the exchange which should have an OUT message
     * @param expected the expected value of the OUT message
     * @throws InvalidPayloadException is thrown if the payload is not the
     *             expected class type
     */
    public static void assertInMessageBodyEquals(Exchange exchange, Object expected) throws InvalidPayloadException {
        assertNotNull(exchange, "Should have a response exchange!");

        Object actual;
        if (expected == null) {
            actual = exchange.getIn().getMandatoryBody();
            assertEquals(expected, actual, "in body of: " + exchange);
        } else {
            actual = exchange.getIn().getMandatoryBody(expected.getClass());
        }
        assertEquals(expected, actual, "in body of: " + exchange);

        LOG.debug("Received response: " + exchange + " with in: " + exchange.getIn());
    }

    /**
     * Asserts that the given exchange has an OUT message of the given body
     * value
     *
     * @param exchange the exchange which should have an OUT message
     * @param expected the expected value of the OUT message
     * @throws InvalidPayloadException is thrown if the payload is not the
     *             expected class type
     */
    public static void assertOutMessageBodyEquals(Exchange exchange, Object expected) throws InvalidPayloadException {
        assertNotNull(exchange, "Should have a response exchange!");

        Object actual;
        if (expected == null) {
            actual = exchange.getOut().getMandatoryBody();
            assertEquals(expected, actual, "output body of: " + exchange);
        } else {
            actual = exchange.getOut().getMandatoryBody(expected.getClass());
        }
        assertEquals(expected, actual, "output body of: " + exchange);

        LOG.debug("Received response: " + exchange + " with out: " + exchange.getOut());
    }

    public static Object assertMessageHeader(Message message, String name, Object expected) {
        Object value = message.getHeader(name);
        assertEquals(expected, value, "Header: " + name + " on Message: " + message);
        return value;
    }

    /**
     * Asserts that the given expression when evaluated returns the given answer
     */
    public static Object assertExpression(Expression expression, Exchange exchange, Object expected) {
        Object value;
        if (expected != null) {
            value = expression.evaluate(exchange, expected.getClass());
        } else {
            value = expression.evaluate(exchange, Object.class);
        }

        LOG.debug("Evaluated expression: " + expression + " on exchange: " + exchange + " result: " + value);

        assertEquals(expected, value, "Expression: " + expression + " on Exchange: " + exchange);
        return value;
    }

    /**
     * Asserts that the predicate returns the expected value on the exchange
     */
    public static void assertPredicateMatches(Predicate predicate, Exchange exchange) {
        assertPredicate(predicate, exchange, true);
    }

    /**
     * Asserts that the predicate returns the expected value on the exchange
     */
    public static void assertPredicateDoesNotMatch(Predicate predicate, Exchange exchange) {
        try {
            PredicateAssertHelper.assertMatches(predicate, "Predicate should match: ", exchange);
        } catch (AssertionError e) {
            LOG.debug("Caught expected assertion error: " + e);
        }
        assertPredicate(predicate, exchange, false);
    }

    /**
     * Asserts that the predicate returns the expected value on the exchange
     */
    public static boolean assertPredicate(final Predicate predicate, Exchange exchange, boolean expected) {
        if (expected) {
            PredicateAssertHelper.assertMatches(predicate, "Predicate failed: ", exchange);
        }
        boolean value = predicate.matches(exchange);

        LOG.debug("Evaluated predicate: " + predicate + " on exchange: " + exchange + " result: " + value);

        assertEquals(expected, value, "Predicate: " + predicate + " on Exchange: " + exchange);
        return value;
    }

    /**
     * Resolves an endpoint and asserts that it is found
     */
    public static Endpoint resolveMandatoryEndpoint(CamelContext context, String uri) {
        Endpoint endpoint = context.getEndpoint(uri);

        assertNotNull(endpoint, "No endpoint found for URI: " + uri);

        return endpoint;
    }

    /**
     * Resolves an endpoint and asserts that it is found
     */
    public static <T extends Endpoint> T resolveMandatoryEndpoint(CamelContext context, String uri, Class<T> endpointType) {
        T endpoint = context.getEndpoint(uri, endpointType);

        assertNotNull(endpoint, "No endpoint found for URI: " + uri);

        return endpoint;
    }

    /**
     * Creates an exchange with the given body
     */
    protected Exchange createExchangeWithBody(CamelContext camelContext, Object body) {
        Exchange exchange = new DefaultExchange(camelContext);
        Message message = exchange.getIn();
        message.setHeader("testClass", getClass().getName());
        message.setBody(body);
        return exchange;
    }

    public static <T> T assertOneElement(List<T> list) {
        assertEquals(1, list.size(), "Size of list should be 1: " + list);
        return list.get(0);
    }

    /**
     * Asserts that a list is of the given size
     */
    public static <T> List<T> assertListSize(List<T> list, int size) {
        return assertListSize("List", list, size);
    }

    /**
     * Asserts that a list is of the given size
     */
    public static <T> List<T> assertListSize(String message, List<T> list, int size) {
        assertEquals(size, list.size(), message + " should be of size: " + size + " but is: " + list);
        return list;
    }

    /**
     * Asserts that a list is of the given size
     */
    public static <T> Collection<T> assertCollectionSize(Collection<T> list, int size) {
        return assertCollectionSize("List", list, size);
    }

    /**
     * Asserts that a list is of the given size
     */
    public static <T> Collection<T> assertCollectionSize(String message, Collection<T> list, int size) {
        assertEquals(size, list.size(), message + " should be of size: " + size + " but is: " + list);
        return list;
    }

    /**
     * A helper method to create a list of Route objects for a given route
     * builder
     */
    public static List<Route> getRouteList(RouteBuilder builder) throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(builder);
        context.start();
        List<Route> answer = context.getRoutes();
        context.stop();
        return answer;
    }

    /**
     * Asserts that the text contains the given string
     *
     * @param text the text to compare
     * @param containedText the text which must be contained inside the other
     *            text parameter
     */
    public static void assertStringContains(String text, String containedText) {
        assertNotNull(text, "Text should not be null!");
        assertTrue(text.contains(containedText), "Text: " + text + " does not contain: " + containedText);
    }

    /**
     * Recursively delete a directory, useful to zapping test data
     *
     * @param file the directory to be deleted
     * @return <tt>false</tt> if error deleting directory
     */
    public static boolean deleteDirectory(String file) {
        return deleteDirectory(new File(file));
    }

    /**
     * Recursively delete a directory, useful to zapping test data
     *
     * @param file the directory to be deleted
     * @return <tt>false</tt> if error deleting directory
     */
    public static boolean deleteDirectory(File file) {
        int tries = 0;
        int maxTries = 5;
        boolean exists = true;
        while (exists && (tries < maxTries)) {
            recursivelyDeleteDirectory(file);
            tries++;
            exists = file.exists();
            if (exists) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        return !exists;
    }

    private static void recursivelyDeleteDirectory(File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File child : files) {
                recursivelyDeleteDirectory(child);
            }
        }
        boolean success = file.delete();
        if (!success) {
            LOG.warn("Deletion of file: " + file.getAbsolutePath() + " failed");
        }
    }

    /**
     * create the directory
     *
     * @param file the directory to be created
     */
    public static void createDirectory(String file) {
        File dir = new File(file);
        dir.mkdirs();
    }

    /**
     * To be used for folder/directory comparison that works across different
     * platforms such as Window, Mac and Linux.
     */
    public static void assertDirectoryEquals(String expected, String actual) {
        assertDirectoryEquals(null, expected, actual);
    }

    /**
     * To be used for folder/directory comparison that works across different
     * platforms such as Window, Mac and Linux.
     */
    public static void assertDirectoryEquals(String message, String expected, String actual) {
        // must use single / as path separators
        String expectedPath = expected.replace('\\', '/');
        String actualPath = actual.replace('\\', '/');

        if (message != null) {
            assertEquals(message, expectedPath, actualPath);
        } else {
            assertEquals(expectedPath, actualPath);
        }
    }

    /**
     * To be used to check is a directory is found in the file system
     */
    public static void assertDirectoryExists(String filename) {
        File file = new File(filename);
        assertTrue(file.exists(), "Directory " + filename + " should exist");
        assertTrue(file.isDirectory(), "Directory " + filename + " should be a directory");
    }

    /**
     * To be used to check is a file is found in the file system
     */
    public static void assertFileExists(String filename) {
        File file = new File(filename);
        assertTrue(file.exists(), "File " + filename + " should exist");
        assertTrue(file.isFile(), "File " + filename + " should be a file");
    }

    /**
     * To be used to check is a file is <b>not</b> found in the file system
     */
    public static void assertFileNotExists(String filename) {
        File file = new File(filename);
        assertFalse(file.exists(), "File " + filename + " should not exist");
    }

    /**
     * Is this OS the given platform.
     * <p/>
     * Uses <tt>os.name</tt> from the system properties to determine the OS.
     *
     * @param platform such as Windows
     * @return <tt>true</tt> if its that platform.
     */
    public static boolean isPlatform(String platform) {
        String osName = System.getProperty("os.name").toLowerCase(Locale.US);
        return osName.contains(platform.toLowerCase(Locale.US));
    }

    /**
     * Is this Java by the given vendor.
     * <p/>
     * Uses <tt>java.vendor</tt> from the system properties to determine the
     * vendor.
     *
     * @param vendor such as IBM
     * @return <tt>true</tt> if its that vendor.
     */
    public static boolean isJavaVendor(String vendor) {
        String javaVendor = System.getProperty("java.vendor").toLowerCase(Locale.US);
        return javaVendor.contains(vendor.toLowerCase(Locale.US));
    }

    /**
     * Is this Java 1.5
     *
     * @return <tt>true</tt> if its Java 1.5, <tt>false</tt> if its not (for
     *         example Java 1.6 or better)
     * @deprecated will be removed in the future as Camel requires JDK1.8+
     */
    @Deprecated
    public static boolean isJava15() {
        return getJavaMajorVersion() == 5;
    }

    /**
     * Is this Java 1.6
     *
     * @return <tt>true</tt> if its Java 1.6, <tt>false</tt> if its not (for
     *         example Java 1.7 or better)
     * @deprecated will be removed in the future as Camel requires JDK1.8+
     */
    @Deprecated
    public static boolean isJava16() {
        return getJavaMajorVersion() == 6;

    }

    /**
     * Is this Java 1.7
     *
     * @return <tt>true</tt> if its Java 1.7, <tt>false</tt> if its not (for
     *         example Java 1.6 or older)
     * @deprecated will be removed in the future as Camel requires JDK1.8+
     */
    @Deprecated
    public static boolean isJava17() {
        return getJavaMajorVersion() == 7;

    }

    /**
     * Is this Java 1.8
     *
     * @return <tt>true</tt> if its Java 1.8, <tt>false</tt> if its not (for
     *         example Java 1.7 or older)
     */
    public static boolean isJava18() {
        return getJavaMajorVersion() == 8;

    }

    /**
     * Is this Java 1.9
     *
     * @return <tt>true</tt> if its Java 1.9, <tt>false</tt> if its not (for
     *         example Java 1.8 or older)
     */
    public static boolean isJava19() {
        return getJavaMajorVersion() == 9;

    }

    /**
     * Returns the current major Java version e.g 8.
     * <p/>
     * Uses <tt>java.specification.version</tt> from the system properties to
     * determine the major version.
     * 
     * @return the current major Java version.
     */
    public static int getJavaMajorVersion() {
        String javaSpecVersion = System.getProperty("java.specification.version");
        if (javaSpecVersion.contains(".")) { // before jdk 9
            return Integer.parseInt(javaSpecVersion.split("\\.")[1]);
        } else {
            return Integer.parseInt(javaSpecVersion);
        }
    }

}
