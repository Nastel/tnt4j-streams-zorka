/*
 * Copyright 2014-2018 JKOOL, LLC.
 *
 * This file is part of TNT4J-Streams-Zorka.
 *
 * TNT4J-Streams-Zorka is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TNT4J-Streams-Zorka is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TNT4J-Streams-Zorka.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jkoolcloud.tnt4j.streams.utils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.jitlogic.zorka.agent.AgentMain;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * Wrapper for Zorka main class to automatically attach to running Java VM's. As Zorka doesn't come with agentmain
 * feature having class it could't be loaded by VirtualMachine.loadAgent(). This class appends required libraries to
 * attaching JVM classpath and loads Zorka as Java agent.
 * 
 * @version $Revision: 1 $
 */
public final class ZorkaAttach {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ZorkaAttach.class);

	private ZorkaAttach() {
	}

	/**
	 * Main entry point for attaching Zorka agent to running JVM.
	 *
	 * @param args
	 *            command-line arguments. Supported arguments:
	 *            <table summary="TNT4J-Streams agent command line arguments">
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;zorkaAgentPath</td>
	 *            <td>(required) Zorka agent path</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;VMDescriptor</td>
	 *            <td>(required) Java VM name fragment/pid to attach to</td>
	 *            </tr>
	 *            </table>
	 */
	public static void main(String... args) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
				"ZorkaAttach.starting.main");
		if (args.length < 2) {
			System.out
					.println(StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, "ZorkaAttach.main.usage"));
			return;
		}
		// args[0] - agent jar path to attach
		// args[1] - VM name fragment/pid to attach to

		attach(args[0], args[1]);
	}

	private static void attach(String agentJarPath, String vmDescr) {
		List<VirtualMachineDescriptor> runningVMsList = VirtualMachine.list();

		boolean found = false;

		for (VirtualMachineDescriptor rVM : runningVMsList) {
			if ((rVM.displayName().contains(vmDescr) && !rVM.displayName().contains(ZorkaAttach.class.getSimpleName()))
					|| rVM.id().equalsIgnoreCase(vmDescr)) {
				try {
					VirtualMachine vm = VirtualMachine.attach(rVM.id());
					try {
						File pathFile = new File(
								ZorkaAttach.class.getProtectionDomain().getCodeSource().getLocation().getPath());
						String agentPath = pathFile.getAbsolutePath();
						LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
								"ZorkaAttach.attaching.agent", agentPath, rVM.displayName());
						vm.loadAgent(agentPath, agentJarPath);
					} finally {
						vm.detach();
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				found = true;
			}
		}

		if (!found) {
			System.out.println(StreamsResources.getStringFormatted(ZorkaConstants.RESOURCE_BUNDLE_NAME,
					"ZorkaAttach.no.jvm", vmDescr));
			System.out.println(
					StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, "ZorkaAttach.available.jvms"));
			System.out
					.println(StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, "ZorkaAttach.list.begin"));
			for (VirtualMachineDescriptor vmD : runningVMsList) {
				System.out.println(StreamsResources.getStringFormatted(ZorkaConstants.RESOURCE_BUNDLE_NAME,
						"ZorkaAttach.list.item", vmD.id(), vmD.displayName()));
			}
			System.out.println(StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, "ZorkaAttach.list.end"));
		}
	}

	/**
	 * Entry point for loading Zorka as java agent.
	 *
	 * @param zorkaHomePath
	 *            Zorka home dir path
	 * @param inst
	 *            JVM instrumentation
	 * @throws Exception
	 *             if exception occurs while loading java agent
	 *
	 * @see AgentMain#premain(String, Instrumentation)
	 */
	public static void agentmain(String zorkaHomePath, Instrumentation inst) throws Exception {
		String zorkaDirPath = new File(zorkaHomePath).getAbsolutePath();
		System.setProperty("zorka.home.dir", zorkaDirPath);
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
				"ZorkaAttach.zorka.path", zorkaDirPath);

		File zorkaPath = new File(zorkaDirPath);
		String[] classPathEntries = zorkaPath.list(new WildcardFileFilter("*.jar")); // NON-NLS
		if (classPathEntries != null) {
			for (String classPathEntry : classPathEntries) {
				File pathFile = new File(classPathEntry);
				extendClasspath(pathFile.toURI().toURL());
			}
		}
		AgentMain.premain(null, inst);
	}

	/**
	 * Loads required classpath entries to running JVM.
	 * 
	 * @param classPathEntriesURL
	 *            classpath entries URLs to attach to JVM
	 * @throws Exception
	 *             if exception occurs while extending system class loader's classpath
	 */
	private static void extendClasspath(URL... classPathEntriesURL) throws Exception {
		URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		method.setAccessible(true);
		for (URL classPathEntryURL : classPathEntriesURL) {
			try {
				method.invoke(classLoader, classPathEntryURL);
			} catch (Exception e) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME), "ZorkaAttach.could.not.load",
						classPathEntryURL, e);

			}
		}
	}
}
