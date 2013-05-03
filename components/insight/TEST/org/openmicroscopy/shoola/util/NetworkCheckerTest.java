/**
 *
 */
package org.openmicroscopy.shoola.util;

import static org.apache.commons.lang.SystemUtils.IS_JAVA_1_5;
import static org.apache.commons.lang.SystemUtils.isJavaVersionAtLeast;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.openmicroscopy.shoola.util.ui.UIUtilities;
import org.testng.annotations.Test;


/**
 *  Some integration tests for NetworkChecker behaviour.
 *
 *  These tests are inherently highly environment dependent, relying on:
 *  - java runtime version (5 vs 6)
 *  - OS version (Linux vs others)
 *  - internet access restrictions (proxy vs none)
 *  - and actual network availability!
 *
 *  The methods under test are the internal helper methods rather than the
 *  public API in order to test the "building blocks" - instead of the
 *  conditional OS and JRE detection logic which happens in the
 *  NetworkChecker's main entry point.
 */
@Test(groups = { "integration", "network" })
public class NetworkCheckerTest {

    //---------------     test data/parameters    ---------------//

    private final static float JAVA_1_6 = 1.6f;
    private final static String BLANK_STRING = "     ";

    /** Is the TestNG "client" connecting through a web proxy? */
    private final static boolean IS_WEB_PROXY_CONNECTION = false;
    private final static String HTTP_PROXY_HOST = "http.proxyHost";
    private final static String HTTP_PROXY_PORT = "http.proxyPort";

    // Karlsruher Institut für Technologie OMERO public demo server at ome2-copy.fzk.de
    //private final static String OMERO_FZK_DOT_DE = "ome2-copy.fzk.de";
    private final static String OMERO_FZK_DOT_DE_IPV4 = "141.52.175.71";


    //---------------     NetworkChecker.fromHostName() tests    ---------------//

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#fromHostName(java.lang.String)}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromHostNameShouldRejectNullParameters() {
        NetworkChecker.fromHostName(null);
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#fromHostName(java.lang.String)}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromHostNameShouldRejectBlankParameters() {
        NetworkChecker.fromHostName(BLANK_STRING);
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#fromHostName(java.lang.String,java.lang.String)}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromHostNameAndPortShouldRejectNullParameters() {
        NetworkChecker.fromHostName(OMERO_FZK_DOT_DE_IPV4, -1);
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#fromHostName(java.lang.String,java.lang.String)}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromHostNameAndPortShouldRejectBlankParameters() {
        NetworkChecker.fromHostName(OMERO_FZK_DOT_DE_IPV4, 0);
    }


    //---------------     NetworkChecker.fromIpAddress() tests    ---------------//

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#fromIpAddress(java.lang.String)}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromIpAddressShouldRejectNullParameters() {
        NetworkChecker.fromIpAddress(null);
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#fromIpAddress(java.lang.String)}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromIpAddressShouldRejectBlankParameters() {
        NetworkChecker.fromIpAddress(BLANK_STRING);
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#fromIpAddress(java.lang.String,java.lang.String)}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromIpAddressAndPortShouldRejectNullParameters() {
        NetworkChecker.fromIpAddress(OMERO_FZK_DOT_DE_IPV4, -1);
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#fromIpAddress(java.lang.String,java.lang.String)}.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromIpAddressAndPortShouldRejectBlankParameters() {
        NetworkChecker.fromIpAddress(OMERO_FZK_DOT_DE_IPV4, 0);
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#remoteEndpointSocketCheck()}.
     */
    @Test(enabled = IS_WEB_PROXY_CONNECTION,
          groups = { "os-linux", "with-proxy", "tcp-check" } )
    public void testRemoteEndpointSocketCheckShouldTimeoutOnLinuxBehindProxy() {
        NetworkChecker checker = NetworkChecker.fromIpAddress(OMERO_FZK_DOT_DE_IPV4);

        if (UIUtilities.isLinuxOS()) {
            boolean isNetworkUp = checker.remoteEndpointSocketCheck();
            assertFalse(
                    isNetworkUp,
                    "Remote TCP check behind proxy should block on Linux");
        }
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#remoteEndpointHttpCheck()}.
     *
     * Must _not_ make use of -Dhttp.proxyHost and -Dhttp.ProxyPort
     */
    @Test(enabled = IS_WEB_PROXY_CONNECTION,
          groups = { "os-linux", "with-proxy", "jvm-proxy-off", "http-check" } )
    public void testRemoteEndpointHttpCheckShouldTimeoutOnLinuxBehindProxy() {
        NetworkChecker checker = NetworkChecker.fromIpAddress(OMERO_FZK_DOT_DE_IPV4);

        if (UIUtilities.isLinuxOS()) {
            String httpProxyHostSystemProp = System.getProperty(HTTP_PROXY_HOST);
            assertNull(
                    "Precondition not met: this test should be run behind a web proxy without JVM options",
                    httpProxyHostSystemProp);

            boolean isNetworkUp = checker.remoteEndpointHttpCheck();
            assertFalse(
                    isNetworkUp,
                    "Remote HTTP check behind proxy with no JVM option should block on Linux");
        }
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#remoteEndpointHttpCheck()}.
     *
     * Must make use of -Dhttp.proxyHost and -Dhttp.ProxyPort
     */
    @Test(enabled = IS_WEB_PROXY_CONNECTION,
          groups = { "os-linux", "with-proxy", "jvm-proxy-on", "http-check" } )
    public void testRemoteEndpointHttpCheckShouldConnectOnLinuxBehindProxy() {
        NetworkChecker checker = NetworkChecker.fromIpAddress(OMERO_FZK_DOT_DE_IPV4);

        if (UIUtilities.isLinuxOS()) {
            String httpProxyHostSystemProp = System.getProperty(HTTP_PROXY_HOST);
            String httpProxyPortSystemProp = System.getProperty(HTTP_PROXY_PORT);
            assertNotNull(
                    "Precondition not met: this test should be run behind a web proxy with JVM host option",
                    httpProxyHostSystemProp);
            assertNotNull(
                    "Precondition not met: this test should be run behind a web proxy with JVM port option",
                    httpProxyPortSystemProp);

            boolean isNetworkUp = checker.remoteEndpointHttpCheck();
            assertTrue(
                    isNetworkUp,
                    "Remote HTTP check behind proxy with JVM options should succeed on Linux");
        }
    }

    //---------------     NetworkChecker.fromDefaults() tests    ---------------//

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#reflectiveCheck()}.
     * @throws UnknownHostException
     */
    @Test(groups = { "jre-15" } )
    public void testDefaultCheckerReflectiveCheckShouldBeTrueOnAnyOSJava5()
            throws UnknownHostException {

        NetworkChecker checker = NetworkChecker.fromDefaults();

        if (IS_JAVA_1_5) {
            boolean isNetworkUp = checker.reflectiveCheck();
            assertTrue(
                    isNetworkUp,
                    "Java 5 reflective check should emulate 'always connected'");
        }
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#reflectiveCheck()}.
     * @throws UnknownHostException
     */
    @Test(groups = { "jre-16" } )
    public void testDefaultCheckerReflectiveCheckShouldBeTrueOnAnyOSJava6Plus()
            throws UnknownHostException {

        NetworkChecker checker = NetworkChecker.fromDefaults();

        if (isJavaVersionAtLeast(JAVA_1_6)) {
            boolean isNetworkUp = checker.reflectiveCheck();
            // assuming that the machine running the test reports true here...
            // might be worth adding a protected getter on useReflectiveCheck to have
            // a more reliable assertion
            assertTrue(
                    isNetworkUp,
                    "Java 6 reflective check should perform actual check");
        }
    }

    /**
     * Test method for {@link org.openmicroscopy.shoola.util.NetworkChecker#remoteEndpointCheck()}.
     * @throws UnknownHostException
     * @throws MalformedURLException
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void testDefaultCheckerRemoteCheckShouldNotBeAvailableOnAnyOS() {

        NetworkChecker checker = NetworkChecker.fromDefaults();

        // The default NetworkChecker instance has no remote IP/port to connect to.
        // Attempting to call this internal method directly should report an
        // invalid internal state (going through the NetworkChecker's
        // public API should never trigger this call)
        checker.remoteEndpointCheck();
    }
}
