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

import java.io.IOException;
import java.net.Socket;
import java.text.ParseException;
import java.util.*;

import org.apache.commons.collections4.MapUtils;

import com.jitlogic.zico.core.ZicoService;
import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.ZorkaConstants;
import com.jkoolcloud.tnt4j.uuid.JUGFactoryImpl;
import com.jkoolcloud.tnt4j.uuid.UUIDFactory;

/**
 * Implements a Zorka traces data processor as activity stream, where each trace data package is assumed to represent a
 * single activity or event which should be recorded. Zico service (Zorka traces producer) to listen is defined using
 * "Host" and "Port" properties in stream configuration.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data. On trace data package reception, trace
 * fields are packed into {@link Map} data structure.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>Host - host name of machine running Zico service to listen. Default value - '0.0.0.0'. (Optional)</li>
 * <li>Port - port number of machine running Zico service to listen. Default value - '8640'. (Optional)</li>
 * <li>MaxTraceEvents - maximum number of events to stream for single stack trace. Default value - '100'. Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see ActivityMapParser
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

	private static final String ZORKA_REPLY_BAD = "BAD";// NON-NLS

	private static final String ZORKA_PROP_CLOCK = "CLOCK"; // NON-NLS
	private static final String ZORKA_PROP_MARKER = "MARKER"; // NON-NLS
	private static final String ZORKA_PROP_SIGNATURE = "SIGNATURE"; // NON-NLS
	private static final String ZORKA_PROP_METHOD = "METHOD"; // NON-NLS
	private static final String ZORKA_PROP_CLASS = "CLASS"; // NON-NLS
	private static final String ZORKA_PROP_CALLS = "CALLS"; // NON-NLS
	private static final String ZORKA_PROP_METHOD_TIME = "METHOD_TIME"; // NON-NLS

	private static final String TNT4J_PROP_TRACKING_ID = "TrackingID"; // NON-NLS
	private static final String TNT4J_PROP_PARENT_ID = "ParentID"; // NON-NLS
	private static final String TNT4J_PROP_LEVEL = "Level"; // NON-NLS
	private static final String TNT4J_PROP_EV_TYPE = "EVENT_TYPE"; // NON-NLS

	private static final String TRACE_MARKER = "TRACE"; // NON-NLS

	private static final String TRACK_TYPE_ACTIVITY = "ACTIVITY"; // NON-NLS
	private static final String TRACK_TYPE_EVENT = "EVENT"; // NON-NLS

	// for persisting symbols inf ile System use PersistentSymbolRegistry
	private SymbolRegistry symbolRegistry = null;

	private String host = DEFAULT_HOSTNAME;
	private Integer socketPort = DEFAULT_PORT;
	private Integer maxTraceEvents = MAX_TRACE_EVENTS;

	private ZicoService zicoService = null;
	private boolean inputEnd = false;

	private UUIDFactory uuidGenerator = new JUGFactoryImpl();

	/**
	 * Constructs an empty ZorkaConnector. Requires configuration settings to set input stream source.
	 */
	public ZorkaConnector() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

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
			} else if (ZorkaConstants.PROP_MAX_TRACE_EVENTS.equalsIgnoreCase(name)) {
				maxTraceEvents = Integer.valueOf(value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return host;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return socketPort;
		}
		if (ZorkaConstants.PROP_MAX_TRACE_EVENTS.equalsIgnoreCase(name)) {
			return maxTraceEvents;
		}
		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		symbolRegistry = new SymbolRegistry();
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
					logger().log(OpLevel.ERROR, StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME,
							"ZorkaConnector.received.null.hello.packet"));
					throw new ZicoException(ZicoPacket.ZICO_BAD_REQUEST, StreamsResources
							.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, "ZorkaConnector.null.hello.packet"));
				}
				if (hello.getHostname() == null) {
					logger().log(OpLevel.ERROR, StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME,
							"ZorkaConnector.received.null.hostname"));
					throw new ZicoException(ZicoPacket.ZICO_BAD_REQUEST, StreamsResources
							.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, "ZorkaConnector.null.hostname"));
				}

				if (ZORKA_REPLY_BAD.equals(hello.getAuth())) {
					throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, StreamsResources
							.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, "ZorkaConnector.login.failed"));
				}
				return ZorkaConnector.this;
			}
		};

		zicoService = new ZicoService(zdf, host, socketPort, MAX_THREADS, CONNECTION_TIMEOUT);
	}

	@Override
	protected void start() throws Exception {
		super.start();

		zicoService.start();

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.stream.start"),
				getClass().getSimpleName(), getName());
	}

	/**
	 * Performs processing of received trace data object. If received object is {@link Symbol}, it gets registered in
	 * symbols registry. If received object is {@link TraceRecord}, data gets transformed to {@link Map} structure and
	 * added to buffer.
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
		processTraceRecursive(rec, rec.getChildren(), null, 1);
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

	private void processTraceRecursive(TraceRecord parentRec, List<TraceRecord> children, String parentUUID,
			int level) {

		final Map<Integer, Object> attrs = parentRec.getAttrs();
		if (parentRec.getMarker() != null && parentRec.getParent() == null) {
			final Map<String, Object> markerActivity = new HashMap<String, Object>();
			final Map<String, Object> zorkaActivityRecord = translateSymbols(attrs);
			markerActivity.putAll(zorkaActivityRecord);

			addDefaultTraceAttributes(markerActivity, parentRec);
			markerActivity.put(ZORKA_PROP_MARKER, symbolRegistry.symbolName(parentRec.getMarker().getTraceId()));

			final String activityID = uuidGenerator.newUUID();
			markerActivity.put(TNT4J_PROP_TRACKING_ID, activityID);
			markerActivity.put(TNT4J_PROP_PARENT_ID, parentUUID);
			markerActivity.put(TNT4J_PROP_EV_TYPE, TRACK_TYPE_ACTIVITY);
			parentUUID = activityID;
			addInputToBuffer(markerActivity);

			final Map<String, Object> markerEvent = new HashMap<String, Object>(markerActivity);

			final String eventID;
			if (markerEvent.get(TNT4J_PROP_TRACKING_ID) == null) {
				eventID = uuidGenerator.newUUID();
				markerEvent.put(TNT4J_PROP_TRACKING_ID, eventID);
			} else {
				eventID = markerEvent.get(TNT4J_PROP_TRACKING_ID).toString();
			}

			markerEvent.put(TNT4J_PROP_TRACKING_ID, eventID);
			markerEvent.put(TNT4J_PROP_PARENT_ID, parentUUID);
			markerEvent.put(TNT4J_PROP_EV_TYPE, TRACK_TYPE_EVENT);
			addInputToBuffer(markerEvent);
		}

		final Map<String, Object> translatedTrace = new HashMap<String, Object>();
		addDefaultTraceAttributes(translatedTrace, parentRec);

		if (attrs != null) {
			final Map<String, Object> attributeEvent = new HashMap<String, Object>(translateSymbols(attrs));
			attributeEvent.put(TNT4J_PROP_PARENT_ID, parentUUID);
			if (attributeEvent.get(TNT4J_PROP_TRACKING_ID) == null) {
				attributeEvent.put(TNT4J_PROP_TRACKING_ID, uuidGenerator.newUUID());
			}
			if (attributeEvent.containsKey(TNT4J_PROP_EV_TYPE)) {
				addInputToBuffer(attributeEvent);
			}
		}

		// ** FOR TRACE TREE ** ACTIVITIES NEEDED IN EUM
		// String eventID = uuidGenerator.newUUID();
		// translatedTrace.put(TNT4J_PROP_TRACKING_ID, eventID);
		translatedTrace.put(TNT4J_PROP_PARENT_ID, parentUUID);
		translatedTrace.put(TNT4J_PROP_LEVEL, level);

		addInputToBuffer(translatedTrace);
		if (children == null)
			return;

		// parentUUID = eventID;
		for (TraceRecord child : children) {
			processTraceRecursive(child, child.getChildren(), parentUUID, level + 1);
		}
	}

	private Map<String, Object> addDefaultTraceAttributes(Map<String, Object> translatedTrace,
			TraceRecord masterRecord) {
		long clock = masterRecord.getClock();
		translatedTrace.put(ZORKA_PROP_CLOCK, clock <= 0 ? System.currentTimeMillis() : clock);
		translatedTrace.put(ZORKA_PROP_METHOD_TIME, masterRecord.getTime());
		translatedTrace.put(ZORKA_PROP_CALLS, masterRecord.getCalls());
		translatedTrace.put(ZORKA_PROP_CLASS, symbolRegistry.symbolName(masterRecord.getClassId()));
		translatedTrace.put(ZORKA_PROP_METHOD, symbolRegistry.symbolName(masterRecord.getMethodId()));
		translatedTrace.put(ZORKA_PROP_SIGNATURE, symbolRegistry.symbolName(masterRecord.getSignatureId()));
		translatedTrace.put(ZORKA_PROP_MARKER, TRACE_MARKER);

		return translatedTrace;
	}

	private TraceRecord filterOversizeTrace(TraceRecord rec) {
		final long recCount = countTraceRecord(rec);
		if (maxTraceEvents == 0 || recCount <= maxTraceEvents) {
			return rec;
		}
		final Long wholeTraceTime = rec.getTime();
		final Float percentageOffset = (float) ((double) this.maxTraceEvents / (double) recCount);
		TraceRecord filteredRec = cloneTraceRecord(rec, wholeTraceTime, percentageOffset);

		final long reducedTrCount = countTraceRecord(filteredRec);
		logger().log(OpLevel.INFO,
				StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME, "ZorkaConnector.reduced.trace"),
				recCount, reducedTrCount, maxTraceEvents);
		return filteredRec;
	}

	private static TraceRecord cloneTraceRecord(TraceRecord trToCopyFrom, Long wholeTraceTime, Float percentageOffset) {
		TraceRecord trReturn = trToCopyFrom.copy();
		trReturn.setChildren(new ArrayList<TraceRecord>());
		if (trToCopyFrom.getChildren() == null) {
			return trReturn;
		}
		for (TraceRecord children : trToCopyFrom.getChildren()) {
			Long methodTime = children.getTime();
			Float percentageMethod = (float) ((double) methodTime / (double) wholeTraceTime);
			if (percentageMethod >= percentageOffset || MapUtils.isNotEmpty(children.getAttrs())
					|| children.getMarker() != null) {
				trReturn.addChild(cloneTraceRecord(children, wholeTraceTime, percentageOffset));
			}
		}
		return trReturn;
	}

	private static long countTraceRecord(TraceRecord tr) {
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

	@Override
	@SuppressWarnings("unchecked")
	protected ActivityInfo applyParsers(Object data) throws IllegalStateException, ParseException {
		Map<String, Object> dataMap = (Map<String, Object>) data;
		String[] tags = new String[] { String.valueOf(dataMap.get(ZORKA_PROP_MARKER)) };
		return applyParsers(tags, data);
	}

	/**
	 * Implements {@link ZicoDataProcessor} method. Does nothing.
	 */
	@Override
	public void commit() {
		// operation not required and does nothing now.
	}

	@Override
	protected boolean isInputEnded() {
		return inputEnd;
	}

	@Override
	protected void cleanup() {
		if (zicoService != null) {
			zicoService.stop();
			inputEnd = true;
		}
		super.cleanup();
	}

	@Override
	protected long getActivityItemByteSize(Map<String, ?> itemMap) {
		return 0; // TODO
	}

}
