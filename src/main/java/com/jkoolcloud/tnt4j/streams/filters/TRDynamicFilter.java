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

package com.jkoolcloud.tnt4j.streams.filters;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.ZorkaConstants;
import com.jkoolcloud.tnt4j.utils.MathUtils;

/**
 * Dynamic {@link TraceRecord} filter using Bollinger bands to filter out trace records by method execution times.
 *
 * @version $Revision: 1 $
 *
 * @see MathUtils
 */
public class TRDynamicFilter implements TraceRecordFilter {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(TRDynamicFilter.class);

	private static final int AUTO_PERSIST_TIME_FACTOR = 50;
	private static final String METHOD_REGISTRY_F_NAME = "methodRegistry.xml"; // NON-NLS

	private MethodRegistryMap methodTimeBuffer;
	private SymbolRegistry symbolRegistry;
	private final int nPeriod;
	private boolean shuttingDown = false;
	private int persistLoop = AUTO_PERSIST_TIME_FACTOR;

	/**
	 * Constructs a new {@link TraceRecord} dynamic method time filter.
	 *
	 * @param nPeriod
	 *            the n period
	 * @param kTimes
	 *            the k times
	 * @param bbThreadRecalcTime
	 *            the time Bollinger bands recalculation time thread to sleep
	 * @param symbolRegistry
	 *            the symbol registry of {@link TraceRecord}
	 *
	 * @see MathUtils#getBBHigh(List, int, int)
	 * @see MathUtils#getBBLow(List, int, int)
	 * @see SymbolRegistry
	 */
	public TRDynamicFilter(final int nPeriod, final int kTimes, final long bbThreadRecalcTime,
			SymbolRegistry symbolRegistry) {
		super();
		this.nPeriod = nPeriod;
		this.symbolRegistry = symbolRegistry;
		restoreMethodRegistry();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				shuttingDown = true;
				persistMethodRegistry();
			}
		}));

		Thread bbCalculationThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!shuttingDown) {
					for (Map.Entry<Integer, MethodRegistry> entry : methodTimeBuffer.entrySet()) {
						MethodRegistry value = entry.getValue();
						if (value == null) {
							continue;
						}
						if (value.isRecalcNeeded()) {
							value.setLowerBB(MathUtils.getBBLow(value.getTimes(), kTimes, nPeriod));
							value.setUpperBB(MathUtils.getBBHigh(value.getTimes(), kTimes, nPeriod));
						}
					}
					if (persistLoop == 0) {
						persistMethodRegistry();
						persistLoop = AUTO_PERSIST_TIME_FACTOR;
					} else {
						persistLoop--;
					}

					Utils.sleep(bbThreadRecalcTime);
				}
			}
		});

		bbCalculationThread.setName("TRDynamicFilter BB calculation thread"); // NON-NLS
		bbCalculationThread.start();
	}

	@Override
	public TraceRecord filter(TraceRecord traceRecord) {
		return filterTraceRecord(markTraceRecord(traceRecord));
	}

	private static TraceRecord filterTraceRecord(TraceRecord trToCopyFrom) {
		TraceRecord trReturn = trToCopyFrom.copy();
		trReturn.setChildren(new ArrayList<TraceRecord>());
		if (trToCopyFrom.getChildren() == null) {
			return trReturn;
		}
		for (TraceRecord children : trToCopyFrom.getChildren()) {
			if (MarkedTraceRecord.isFlagged(children)) {
				trReturn.addChild(filterTraceRecord(children));
			}
		}
		return trReturn;
	}

	private TraceRecord markTraceRecord(TraceRecord tr) {
		int methodId = tr.getMethodId();
		int signatureId = tr.getSignatureId();

		MethodRegistry methodContext = methodTimeBuffer.get(methodId);
		if (methodContext == null) {
			methodContext = new MethodRegistry(symbolRegistry.symbolName(methodId),
					symbolRegistry.symbolName(signatureId), methodId, nPeriod);
			methodTimeBuffer.put(methodId, methodContext);
		}
		long time = tr.getTime();
		if (methodContext.isSymbolRegistryCheckNeeded()) {
			String symbolName = symbolRegistry.symbolName(methodId);
			String signature = symbolRegistry.symbolName(signatureId);
			if (!methodContext.checkSymbolName(methodId, symbolName)) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
						"TRDynamicFilter.recovering.symbol");
				methodContext = methodTimeBuffer.lookup(symbolName, signature);
				if (methodContext != null) {
					methodTimeBuffer.fixup(methodId, methodContext);
				} else {
					methodContext = new MethodRegistry(symbolName, symbolName, methodId, nPeriod);
				}
			}
		}

		methodContext.add(time);

		if (Utils.equals(methodContext.getLowerBB(), 0.0d) && Utils.equals(methodContext.getUpperBB(), 0.0d)) {
			tr.markFlag(MarkedTraceRecord.NO_REFERENCE_METHOD_TIME);
		} else if (time <= methodContext.getLowerBB() || time >= methodContext.getUpperBB()) {
			MarkedTraceRecord.setFlag(MarkedTraceRecord.METHOD_TIME_OUT_BAND, tr);
		}

		List<TraceRecord> superClassChildren = tr.getChildren();
		if (superClassChildren != null) {
			for (TraceRecord trChild : superClassChildren) {
				markTraceRecord(trChild);
			}
		}
		return tr;
	}

	/**
	 * Persists methods registry data to xml file over JAXB.
	 *
	 * @see JAXBContext
	 * @see Marshaller
	 */
	protected void persistMethodRegistry() {
		try {
			// #Binary implementation

			// FileOutputStream fileOut = new FileOutputStream(METHOD_REGISTRY_F_NAME);
			// ObjectOutputStream out = new ObjectOutputStream(fileOut);
			// out.writeObject(methodTimeBuffer);
			// out.close();
			// fileOut.close();

			// #JAXB implementation
			JAXBContext jaxb = JAXBContext.newInstance(MethodRegistryMap.class, Long.class);
			Marshaller marshaller = jaxb.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			// MethodRegistryMap mapDelegate = new MethodRegistryMap();
			// mapDelegate.setDelegate(methodTimeBuffer);

			marshaller.marshal(methodTimeBuffer, new FileOutputStream(METHOD_REGISTRY_F_NAME));

			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
					"TRDynamicFilter.registry.persisted", METHOD_REGISTRY_F_NAME);
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
					"TRDynamicFilter.registry.persisting.failed", METHOD_REGISTRY_F_NAME, exc);
		}

	}

	/**
	 * Restores methods registry data from xml file over JAXB.
	 *
	 * @see JAXBContext
	 * @see Unmarshaller
	 */
	protected void restoreMethodRegistry() {
		try {
			// #Binary implementation

			// FileInputStream fileIn = new FileInputStream(METHOD_REGISTRY_F_NAME);
			// ObjectInputStream in = new ObjectInputStream(fileIn);
			// methodTimeBuffer = (HashMap<Integer, MethodRegistry>) in.readObject();
			// in.close();
			// fileIn.close();

			// JAXB implementation
			JAXBContext jaxb = JAXBContext.newInstance(MethodRegistryMap.class, Long.class);
			Unmarshaller unmarshaller = jaxb.createUnmarshaller();
			methodTimeBuffer = (MethodRegistryMap) unmarshaller.unmarshal(new File(METHOD_REGISTRY_F_NAME));
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
					"TRDynamicFilter.registry.loaded", METHOD_REGISTRY_F_NAME);
		} catch (Exception exc) {
			methodTimeBuffer = new MethodRegistryMap();
			if ((new File(METHOD_REGISTRY_F_NAME)).exists()) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
						"TRDynamicFilter.registry.load.failed", METHOD_REGISTRY_F_NAME, exc);
			} else {
				LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(ZorkaConstants.RESOURCE_BUNDLE_NAME),
						"TRDynamicFilter.registry.not.found");
			}
			return;
		}

	}
}
