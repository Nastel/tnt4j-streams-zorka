/*
 * Copyright 2014-2016 JKOOL, LLC.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;
import org.junit.Test;

import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.zico.ZicoClientConnector;
import com.jitlogic.zorka.common.zico.ZicoDataLoader;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.ZorkaConstants;

/**
 * @author akausinis
 * @version 1.0
 */
@Ignore
public class ZorkaConnectorTest {

	// TODO move this thing to stress tests

	private AtomicLong records = new AtomicLong(0), bytes = new AtomicLong(0);
	private AtomicInteger errors = new AtomicInteger(0), passes = new AtomicInteger(0);

	int submissions = 0;

	private Executor executor = Executors.newFixedThreadPool(12);

	private void load(File dir, String file, String hostname) {
		System.out.println("Starting: " + new File(dir, file));
		long t1 = System.nanoTime();
		try {
			ZicoDataLoader loader = new ZicoDataLoader("127.0.0.1", 8640, hostname, "");
			loader.load(new File(dir, file).getPath());

			records.addAndGet(loader.getRecords());
			bytes.addAndGet(loader.getBytes());

			long t = (System.nanoTime() - t1) / 1000000;
			long recsps = 1000L * loader.getRecords() / t;
			long bytesps = 1000L * loader.getBytes() / t;

			System.out.println("File " + dir + '/' + file + " finished: t=" + t + " records=" + loader.getRecords()
					+ " (" + recsps + " recs/s)" + " bytes=" + loader.getBytes() + '(' + bytesps + " bytes/s).");

		} catch (Exception e) {
			errors.incrementAndGet();
		}
		passes.incrementAndGet();
	}

	@Test
	public void testLoadDataFile() throws Exception {
		ZicoDataLoader loader = new ZicoDataLoader("127.0.0.1", 8640, System.getProperty("load.host", "test"), "");
		loader.load(System.getProperty("load.file", "./test/trace.ztr"));
		Thread.sleep(10000000);
	}

	private Set<String> VERBOTEN = ZorkaUtil.set(".", "src/main");

	@Test
	@Ignore
	public void testLoadMultipleDataFiles() throws Exception {
		File rootdir = new File("/tmp/traces");
		for (final String d : rootdir.list()) {
			final File dir = new File(rootdir, d);
			if (!VERBOTEN.contains(d) && dir.isDirectory()) {
				for (final String f : dir.list()) {
					if (f.matches("^trace.ztr.*")) {
						System.out.println("Submitting: " + new File(dir, f));
						submissions++;
						executor.execute(new Runnable() {
							@Override
							public void run() {
								load(dir, f, d + f);
							}
						});
					}
				}
			}
		}

		long t1 = System.nanoTime();
		while (submissions > passes.get()) {
			Thread.sleep(500);
		}
		long t = (System.nanoTime() - t1) / 1000000;
		long recsps = 1000L * records.get() / t;
		long bytesps = 1000L * bytes.get() / t;

		System.out.println("Overall execution time: " + t + "ms");
		System.out.println("Overall Records processed: " + records.get() + '(' + recsps + " recs/s)");
		System.out.println("Overall Bytes processed: " + bytes.get() + '(' + bytesps + " bytes/s");
	}

	@Test(timeout = 1000)
	public void testSendSimpleSymbolMessage() throws Exception {

		ZicoClientConnector conn = new ZicoClientConnector("127.0.0.1", 8640);
		conn.connect();

		conn.hello("test", "aaa");
		final TraceRecord traceRecord = new TraceRecord();
		conn.submit(new Symbol(1, "test"));
		Thread.sleep(10000000);
		Utils.close(conn);
	}

	@Test
	public void testRB() {
		String keyModule = "ZorkaConnector.received.null.hello.packet";
		String keyCore = "ActivityField.field.type.name.empty";

		String rbs1 = StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("Zorka resource bundle entry not found", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("Zorka resource bundle entry found in core", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", keyCore, rbs1);
		rbs1 = StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in zorka", keyCore, rbs1);
	}

}
