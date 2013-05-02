/*
 * org.openmicroscopy.shoola.env.data.NetworkChecker
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2012 University of Dundee & Open Microscopy Environment.
 *  All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.util;

//Java imports
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.util.ui.UIUtilities;


/** 
 * Checks if the network is still up.
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @since 4.4
 */
@SuppressWarnings("unchecked")
public class NetworkChecker {

	@SuppressWarnings("rawtypes")
	static private Class NetworkInterfaceClass = null;
	static private Method getNetworkInterfacesMethod = null;
	static private Method isUpMethod = null;
	static private Method isLoopbackMethod = null;
	static private boolean useReflectiveCheck = false;

	/** Should we probe against host name and port as configured in
         *  the registry or the user credentials ? */
        static private boolean useIceConnectionParameters = true;

	static {
		//
		// Perform static lookup via reflection of the methods
		// needed to run Java 6 network checks.
		//
		try {
			NetworkInterfaceClass = Class.forName("java.net.NetworkInterface");
			getNetworkInterfacesMethod = NetworkInterfaceClass.getMethod("getNetworkInterfaces");
			isUpMethod = NetworkInterfaceClass.getMethod("isUp");
			isLoopbackMethod = NetworkInterfaceClass.getMethod("isLoopback");
			useReflectiveCheck = true;
		} catch (ClassNotFoundException e) {
			// Knowingly using System.err since 1) this will be primarily used on
			// Linux in the first instance and 2) we don't have access to a logger.
			System.err.println("NetworkInterface class not found: assuming Java 1.5");
		} catch (SecurityException e) {
			// This should not happen. Logging (at ERROR)
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// This should not happen. Logging (at ERROR)
			e.printStackTrace();
		}
	}

	/**
	 * The IP Address of the server the client is connected to
	 * or <code>null</code>.
	 */
	private String ipAddress;
	
	/** Creates a new instance.
	 */
	private NetworkChecker()
	{
		this(null);
	}
	
	/**
	 * Creates a new instance.
	 * 
	 * @param ipAddress The IP address of the server the client is connected to
	 * or <code>null</code>.
	 */
	private NetworkChecker(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}
	
	/**
	 * Creates a vanilla NetworkChecker instance.
	 *
	 * @return a new, default <code>NetworkChecker<code> instance
	 * @deprecated All checks should be made using host and port parameters
	 * retrieved from registry lookup. See #10824.
	 */
	public static NetworkChecker fromDefaults() {
	    return new NetworkChecker();
	}

	/**
         * Creates a new NetworkChecker instance from a remote IP address.
         *
         * @param ipAddress the remote address to probe for network availability checks.
         * @return a new <code>NetworkChecker<code> instance
         * @throws IllegalArgumentException if the required ipAdress parameter is null or blank
         */
        public static NetworkChecker fromIpAddress(String ipAddress) {
            if (null == ipAddress || ipAddress.trim().length() == 0) {
                throw new IllegalArgumentException("Invalid IP adress: " + ipAddress);
            }

            return new NetworkChecker(ipAddress);
        }

        /**
         * Creates a new NetworkChecker instance from a remote host name.
         *
         * @param hostName the remote host name to probe for network availability checks.
         * @return @return a new <code>NetworkChecker<code> instance
         * @throws IllegalArgumentException if the required hostName parameter is null or blank
         */
        public static NetworkChecker fromHostName(String hostName) {
            if (null == hostName || hostName.trim().length() == 0) {
                throw new IllegalArgumentException("Invalid host name: " + hostName);
            }

            String ip = resolveIpAddressFromHostName(hostName);

            return NetworkChecker.fromIpAddress(ip);
        }

        /**
         * Resolve a host name into an IP address.
         *
         * @param hostName the host name
         * @return the IP address for requested the host, or <code>null</code>
         * if the name resolution failed.
         */
        private static String resolveIpAddressFromHostName(String hostName) {
            // Host name to IP resolution logic factored out of code from:
            // - org.openmicroscopy.shoola.env.rnd.RenderingControlProxy#<ctor>
            // - org.openmicroscopy.shoola.env.data.OMEROGateway#createSession

            //TODO: add exception logging and/or fail fast rather than swallow error?
            String ip = null;
            try {
                ip = InetAddress.getByName(hostName).getHostAddress();
            } catch (Exception e) {
                //ignore
            }

            return ip;
        }

	/**
	 * Run the standard Java 1.6 check using reflection. If this is not 1.6 or later, then
	 * exit successfully, printing this assumption to System.err. If any odd reflection error
	 * occurs, then act as if the network is up, even though we don't know. Finally, if the
	 * reflection code works properly, throw an {@link UnknownHostException} if no up, non-loopback
	 * network is found.
	 *
	 * @throws UnknownHostException
	 */
	@SuppressWarnings({ "rawtypes"})
	public boolean reflectiveCheck() throws UnknownHostException {

		if (!useReflectiveCheck) {
			return true;
		}

		try {
			Enumeration interfaces = (Enumeration) getNetworkInterfacesMethod.invoke(null);
			if (interfaces != null) {
				while (interfaces.hasMoreElements()) {
					Object ni = interfaces.nextElement();
					Boolean isUp = (Boolean) isUpMethod.invoke(ni);
					Boolean isLoopback = (Boolean) isLoopbackMethod.invoke(ni);
					if (isUp != null && isLoopback != null && (isUp && !isLoopback)) {
						// TODO: add logging here for the successful check.
						return true;
					}
				}
			}
		} catch (Exception e) {
			// This should not happen. It likely implies that something has happened in
			// our reflection code, since no checked exceptions are thrown from the 1.6 code.
			System.err.println("Failed during reflection. Assuming network is up.");
			e.printStackTrace();
			return true;
		}

		// If we reach here and no exception has been thrown then we assume that
		// there is no network.
		return false;
	}

	/**
	 * Run the network check using a connection to a remote host.
	 *
	 * @return true if the remote connection was successful.
	 * @throws UnknownHostException If the remote connection attempt failed.
	 */
	public boolean remoteEndpointCheck() throws UnknownHostException {
	    boolean networkup = false;

	    // FIXME: stub implementation - always return true under Linux
	    if (!useReflectiveCheck) {
                return true;
            }

	    return networkup;
	}

	/**
	 * Returns <code>true</code> if the network is still up, otherwise
	 * throws an <code>UnknownHostException</code>.
	 * 
	 * @return See above.
	 * @throws Exception Thrown if the network is down.
	 */
	public boolean isNetworkup()
		throws Exception
	{

		boolean networkup = false;
		List<String> ips = new ArrayList<String>();
		if (UIUtilities.isLinuxOS()) {
		    if (useIceConnectionParameters) {
                        // NetworkChecker should have been initialized with
                        // the same connection parameters used by the client
                        // to connect to the OMERO server: try and validate
                        // network availability against that server instead
                        // of going out to a unique external URL (and potential
                        // point of failure).
                        networkup = remoteEndpointCheck();
                    } else {
			// Not trying any connection on linux to prevent hangs.
			// On Java 1.6+, reflectiveCheck will perform a proper check.
			// On Java 1.5 and before, it will simply return true.
			networkup = reflectiveCheck();
                    }
		} else {
			Enumeration<NetworkInterface> interfaces =
					NetworkInterface.getNetworkInterfaces();
			if (interfaces != null) {
				NetworkInterface ni;
				InetAddress ia;
				while (interfaces.hasMoreElements()) {
					ni = interfaces.nextElement();
					Enumeration<InetAddress> e = ni.getInetAddresses();
					if (!ni.getDisplayName().startsWith("lo")) {
						while (e.hasMoreElements()) {
							ia = (InetAddress) e.nextElement();
							if (!ia.isAnyLocalAddress() &&
									!ia.isLoopbackAddress()) {
								//if (!ia.isSiteLocalAddress()) {
								if (!ia.getHostName().equals(
										ia.getHostAddress())) {
									networkup = true;
									break;
								}
								//}
							}
						}
						if (networkup) break;
					} else {
						String ip;
						while (e.hasMoreElements()) {
							ia = (InetAddress) e.nextElement();
							ip = ia.getHostAddress();
							if (!ips.contains(ip))
								ips.add(ip);
						}
					}
				}
			}
		}
		if (!networkup) {
			if (ipAddress != null) {
				if (ips.contains(ipAddress)) return true;
			}
			throw new UnknownHostException("Network is down.");
		}
		return networkup;
	}
}
