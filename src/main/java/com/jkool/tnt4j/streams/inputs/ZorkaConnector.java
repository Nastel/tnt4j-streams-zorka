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

package com.jkool.tnt4j.streams.inputs;

import java.io.IOException;
import java.net.Socket;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jitlogic.zico.core.ZicoService;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;
import com.jkool.tnt4j.streams.configure.StreamProperties;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.ZorkaConstants;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.uuid.JUGFactoryImpl;
import com.nastel.jkool.tnt4j.uuid.UUIDFactory;

/**
 * <p>
 * Implements a Zorka traces data processor as activity stream, where each trace
 * data package is assumed to represent a single activity or event which should
 * be recorded. Zico service (Zorka traces producer) to listen is defined using
 * "Host" and "Port" properties in stream configuration.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data. On
 * trace data package reception, trace fields are packed into {@link Map} data
 * structure.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>Host - host name of machine running Zico service to listen. (Optional)
 * </li>
 * <li>Port - port number of machine running Zico service to listen. (Optional)
 * </li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see com.jkool.tnt4j.streams.parsers.ActivityMapParser
 * @see com.jitlogic.zorka.common.zico.ZicoDataProcessor
 * @see com.jitlogic.zico.core.ZicoService
 */
public class ZorkaConnector extends AbstractBufferedStream<Map<String, ?>> implements ZicoDataProcessor {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ZorkaConnector.class);

	private static final int CONNECTION_TIMEOUT = 10 * 1000;
	private static final int MAX_THREADS = 5;
	private static final int DEFAULT_PORT = 8640;
	private static final String DEFAULT_HOSTNAME = "0.0.0.0"; // NON-NLS
	private static final int MAX_TRACE_EVENTS = 100;

	// for persisting symbols inf ile System use PersistentSymbolRegistry
	private SymbolRegistry symbolRegistry = null;

	private String host = DEFAULT_HOSTNAME;
	private Integer socketPort = DEFAULT_PORT;
	private Integer maxTraceEvents = MAX_TRACE_EVENTS;

	private ZicoService zicoService = null;
	private boolean inputEnd = false;

	private UUIDFactory uuidGenerator = new JUGFactoryImpl();

	/**
	 * Construct empty ZorkaConnector. Requires configuration settings to set
	 * input stream source.
	 */
	public ZorkaConnector() {
		this(LOGGER);
	}

	/**
	 * Constructs a new ZorkaConnector.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected ZorkaConnector(EventSink logger) {
		super(logger);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return host;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return socketPort;
		}
		if (ZorkaConstants.MAX_TRACE_EVENTS.equalsIgnoreCase(name)) {
			return maxTraceEvents;
		}
		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}
		super.setProperties(props);
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
				host = value;
			} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
				socketPort = Integer.valueOf(value);
			} else if (ZorkaConstants.MAX_TRACE_EVENTS.equalsIgnoreCase(name)) {
				maxTraceEvents = Integer.valueOf(value);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Exception {
		super.initialize();
		symbolRegistry = new SymbolRegistry();
		// traceDataStore = new TraceRecordStore()
		// traceIn
		ZicoDataProcessorFactory zdf = new ZicoDataProcessorFactory() {

			/**
			 * Performs Zico Service connection "handshake" validation.
			 *
			 * @param socket
			 *            Zico service socket
			 * @param hello
			 *            hello request data packet
			 * 
			 * @return Zico data processor to be used by Zico service
			 *
			 * @throws IOException
			 *             if connection "handshake" validation failed
			 */
			@Override
			public ZicoDataProcessor get(Socket socket, HelloRequest hello) throws IOException {
				if (hello == null) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_ZORKA,
							"ZorkaConnector.received.null.hello.packet"));
					throw new ZicoException(ZicoPacket.ZICO_BAD_REQUEST, StreamsResources
							.getString(ZorkaConstants.RESOURCE_BUNDLE_ZORKA, "ZorkaConnector.null.hello.packet"));
				}
				if (hello.getHostname() == null) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_ZORKA,
							"ZorkaConnector.received.null.hostname"));
					throw new ZicoException(ZicoPacket.ZICO_BAD_REQUEST, StreamsResources
							.getString(ZorkaConstants.RESOURCE_BUNDLE_ZORKA, "ZorkaConnector.null.hostname"));
				}

				if ("BAD".equals(hello.getAuth())) { // NON-NLS
					throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, StreamsResources
							.getString(ZorkaConstants.RESOURCE_BUNDLE_ZORKA, "ZorkaConnector.login.failed"));
				}
				return ZorkaConnector.this;
			}
		};

		zicoService = new ZicoService(zdf, host, socketPort, MAX_THREADS, CONNECTION_TIMEOUT);
		zicoService.start();
	}

	/**
	 * Performs processing of received trace data object. If received object is
	 * {@link Symbol}, it gets registered in symbols registry. If received
	 * object is {@link TraceRecord}, data gets transformed to {@link Map}
	 * structure and added to buffer.
	 *
	 * @param obj
	 *            trace data object
	 *
	 * @throws IOException
	 *             if error occurs while processing received trace data object
	 */
	@Override
	public void process(Object obj) throws IOException {
		if (obj instanceof Symbol) {
			processSymbol((Symbol) obj);
			return;
		}

		if (obj instanceof TraceRecord) {
			processTrace((TraceRecord) obj);
		}
	}

	private void processSymbol(Symbol symbol) {
		if (symbolRegistry == null) {
			symbolRegistry = new SymbolRegistry();
		}

		symbolRegistry.put(symbol.getId(), symbol.getName());
	}

	private void processTrace(TraceRecord rec) {

		rec = filterOversizeTrace(rec);

		processTraceRecursive(rec, rec.getChildren(), null);

		// final Map<String, Object> translatedTrace =
		// translateSymbols(rec.getAttrs());
		// addDefaultTraceAttributes(translatedTrace, rec);
		// addInputToBuffer(translatedTrace);

	}

	private Map<String, Object> translateSymbols(Map<Integer, Object> attributeMap) {
		Map<String, Object> translation = new HashMap<String, Object>();
		if (attributeMap != null) {
			for (Map.Entry<Integer, Object> attrEntry : attributeMap.entrySet()) {
				final String symbolName = symbolRegistry.symbolName(attrEntry.getKey());
				final Object attribute = attrEntry.getValue();
				translation.put(symbolName, attribute);
			}
		}
		return translation;
	}

	private void processTraceRecursive(TraceRecord parentRec, List<TraceRecord> children, String parentUUID) {
		final Map<String, Object> translatedTrace = new HashMap<String, Object>(); // (rec.getAttrs());
		addDefaultTraceAttributes(translatedTrace, parentRec);
		if (parentRec.getParent() == null) {
			translatedTrace.putAll(translateSymbols(parentRec.getAttrs()));
		}
		String eventID = uuidGenerator.newUUID();
		translatedTrace.put("TrackingID", eventID); // NON-NLS
		translatedTrace.put("ParentID", parentUUID); // NON-NLS
		addInputToBuffer(translatedTrace);
		if (children == null)
			return;

		parentUUID = eventID;
		for (TraceRecord child : children) {
			// if (child.getAttrs() != null) {
			// rec.getAttrs().putAll(child.getAttrs());
			// LOGGER.log(OpLevel.DEBUG,
			// StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_ZORKA,
			// "ZorkaConnector.decorating.child"));
			//
			// }
			processTraceRecursive(child, child.getChildren(), parentUUID);
		}
	}

	private Map<String, Object> addDefaultTraceAttributes(Map<String, Object> translatedTrace,
			TraceRecord masterRecord) {
		long clock = masterRecord.getClock();
		translatedTrace.put("CLOCK", clock <= 0 ? System.currentTimeMillis() : clock); // NON-NLS
		translatedTrace.put("METHOD_TIME", masterRecord.getTime()); // NON-NLS
		translatedTrace.put("CALLS", masterRecord.getCalls()); // NON-NLS
		translatedTrace.put("CLASS", symbolRegistry.symbolName(masterRecord.getClassId())); // NON-NLS
		translatedTrace.put("METHOD", symbolRegistry.symbolName(masterRecord.getMethodId())); // NON-NLS
		translatedTrace.put("SIGNATURE", symbolRegistry.symbolName(masterRecord.getSignatureId())); // NON-NLS
		translatedTrace.put("MARKER", masterRecord.getMarker() == null ? "TRACE" // NON-NLS
				: symbolRegistry.symbolName(masterRecord.getMarker().getTraceId()));

		return translatedTrace;
	}

	private TraceRecord filterOversizeTrace(TraceRecord rec) {
		final long recCount = countTraceRecord(rec);
		if (maxTraceEvents == 0 && recCount <= maxTraceEvents) {
			return rec;
		}
		final Long wholeTraceTime = rec.getTime();
		final Float percentageOffset = (float) ((double) this.maxTraceEvents / (double) recCount);
		TraceRecord filteredRec = cloneTraceRecord(rec, wholeTraceTime, percentageOffset);

		final long reducedTrCount = countTraceRecord(filteredRec);
		LOGGER.log(OpLevel.INFO, "Reduced trace from " + recCount + " to " + reducedTrCount + " record to comply to "
				+ maxTraceEvents + " max trace vent count");
		return filteredRec;
	}

	private TraceRecord cloneTraceRecord(TraceRecord trToCopyFrom, Long wholeTraceTime, Float percentageOffset) {
		TraceRecord trReturn = new TraceRecord();

		trReturn.setAttrs(trToCopyFrom.getAttrs());
		trReturn.setCalls(trToCopyFrom.getCalls());
		trReturn.setClassId(trToCopyFrom.getClassId());
		trReturn.setErrors(trToCopyFrom.getErrors());
		trReturn.setException(trToCopyFrom.getException());
		trReturn.setFlags(trToCopyFrom.getFlags());
		trReturn.setMarker(new TraceMarker(trToCopyFrom.getTraceId(), trToCopyFrom.getClock()));
		trReturn.setMethodId(trToCopyFrom.getMethodId());
		trReturn.setParent(trToCopyFrom.getParent());
		trReturn.setSignatureId(trToCopyFrom.getSignatureId());
		trReturn.setTime(trToCopyFrom.getTime());
		if (trToCopyFrom.getChildren() == null) {
			return trReturn;
		}
		for (TraceRecord children : trToCopyFrom.getChildren()) {
			Long methodTime = children.getTime();
			Float percentageMethod = (float) ((double) methodTime / (double) wholeTraceTime);
			if (percentageMethod >= percentageOffset) {
				trReturn.addChild(cloneTraceRecord(children, wholeTraceTime, percentageOffset));
			}
		}
		return trReturn;
	}

	private long countTraceRecord(TraceRecord tr) {
		long count = 0;
		if (tr.getChildren() == null) {
			return 1;
		}
		for (TraceRecord traceRecord : tr.getChildren()) {
			if (traceRecord != null) {
				count += countTraceRecord(traceRecord);
			}
		}

		count++;
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected ActivityInfo applyParsers(Object data) throws IllegalStateException, ParseException {
		Map<String, Object> dataMap = (Map<String, Object>) data;
		String[] tags = new String[] { String.valueOf(dataMap.get("MARKER")) }; // NON-NLS
		return applyParsers(tags, data);
	}

	/**
	 * Implements {@link ZicoDataProcessor} method. Does nothing.
	 */
	@Override
	public void commit() {
		// operation not required and does nothing now.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isInputEnded() {
		return inputEnd;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		if (zicoService != null) {
			zicoService.stop();
			inputEnd = true;
		}
		super.cleanup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected long getActivityItemByteSize(Map<String, ?> itemMap) {
		return 0; // TODO
	}

}
