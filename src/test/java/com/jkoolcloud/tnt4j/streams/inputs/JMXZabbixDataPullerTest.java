/*
 * Copyright 2014-2017 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.inputs;

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.ZorkaStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class JMXZabbixDataPullerTest {

	private static final Integer TEST_PORT = 10057;
	private JMXZabbixDataPuller stream;

	@Before
	public void prepareStream() {
		stream = new JMXZabbixDataPuller();
		StreamThread st = mock(StreamThread.class);
		st.setName("FileLineStreamTestThreadName"); // NON-NLS
		stream.setOwnerThread(st);
	}

	@Test
	public void testProperties() {
		Map<String, String> properties = new HashMap<>(4);
		properties.put(StreamProperties.PROP_HOST, "localhost"); // NON-NLS
		properties.put(StreamProperties.PROP_PORT, String.valueOf(TEST_PORT));
		properties.put(ZorkaStreamProperties.PROP_JMX_QUERY,
				"\"java\",\"java.lang:type=Memory\",\"HeapMemoryUsage\",\"used\""); // NON-NLS
		properties.put(ZorkaStreamProperties.PROP_SCHEDULER_EXPR, "0/1 * * 1/1 * ? *"); // NON-NLS
		stream.setProperties(properties.entrySet());
		testPropertyList(stream, properties.entrySet());
	}

	@Test
	public void testInitialize() throws Exception {
		testProperties();
		stream.startStream();
		Thread.sleep(2000);
		stream.cleanup();
	}

	@Test
	public void testGetCommand() throws Exception {
		new Thread(new Runnable() {

			@Override
			public void run() {
				ServerSocket sc = null;
				Socket socket = null;
				PrintWriter output = null;

				try {
					sc = new ServerSocket(TEST_PORT);
					socket = sc.accept();
					Utils.close(sc);
					output = new PrintWriter(socket.getOutputStream());
					output.println("1234567890123TEST_MEMORY_USED"); // NON-NLS
					output.flush();

					Thread.sleep(800);
				} catch (Exception e) {
					fail("Exception occurred on JMX bean server side");
				} finally {
					Utils.close(output);
					Utils.close(socket);
					Utils.close(sc);
				}
			}
		}).start();

		testProperties();
		stream.startStream();

		Thread.sleep(500);
		final Map<String, String> nextItem = stream.getNextItem();
		stream.cleanup();
		assertFalse("JMX response is empty!", nextItem.isEmpty()); // NON-NLS
		assertEquals("Unexpected JMX bean value", "TEST_MEMORY_USED", nextItem.entrySet().iterator().next().getValue());// NON-NLS
	}

}
