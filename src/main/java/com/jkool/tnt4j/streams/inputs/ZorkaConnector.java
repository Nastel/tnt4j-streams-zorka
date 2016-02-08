/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
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
import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;
import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.ZorkaConstants;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a Zorka traces data processor as activity stream, where each trace
 * data package is assumed to represent a single activity or event which should
 * be recorded. Zico service (Zorka traces producer) to listen is defined using
 * "Host" and "Port" properties in stream configuration.
 * <p>
 * This activity stream requires parsers that can support {@code Map} data. On
 * trace data package reception, trace fields are packed into {@code Map} data
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
public class ZorkaConnector extends AbstractBufferedStream<Object> implements ZicoDataProcessor {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ZorkaConnector.class);

	private static final int CONNECTION_TIMEOUT = 10 * 1000;
	private static final int MAX_THREADS = 5;
	private static final int DEFAULT_PORT = 8640;
	private static final String DEFAULT_HOSTNAME = "localhost"; // NON-NLS

	// for persisting symbols inf ile System use PersistentSymbolRegistry
	private SymbolRegistry symbolRegistry = null;

	private String host = DEFAULT_HOSTNAME;
	private Integer socketPort = DEFAULT_PORT;

	private ZicoService zicoService = null;
	private boolean inputEnd = false;

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
		if (StreamsConfig.PROP_HOST.equalsIgnoreCase(name)) {
			return host;
		}
		if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
			return socketPort;
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

			if (StreamsConfig.PROP_HOST.equalsIgnoreCase(name)) {
				host = value;
			} else if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
				socketPort = Integer.valueOf(value);
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
	 * {@code Symbol}, it gets registered in symbols registry. If received
	 * object is {@code TraceRecord}, data gets transformed to {@code Map}
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
		// processTraceRecursive(rec, rec.getChildren()); //NOTE: not required
		final Map<String, Object> translatedTrace = translateSymbols(rec.getAttrs());
		addDefaultTraceAttributes(translatedTrace, rec);
		addInputToBuffer(translatedTrace);
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

	private static void processTraceRecursive(TraceRecord rec, List<TraceRecord> children) {
		if (children == null)
			return;

		for (TraceRecord child : children) {
			if (child.getAttrs() != null) {
				rec.getAttrs().putAll(child.getAttrs());
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_ZORKA,
						"ZorkaConnector.decorating.child"));
			}
			processTraceRecursive(rec, child.getChildren());
		}
	}

	private Map<String, Object> addDefaultTraceAttributes(Map<String, Object> translatedTrace,
			TraceRecord masterRecord) {
		translatedTrace.put("CLOCK", masterRecord.getClock()); // NON-NLS
		translatedTrace.put("METHOD_TIME", masterRecord.getTime()); // NON-NLS
		translatedTrace.put("CALLS", masterRecord.getCalls()); // NON-NLS
		translatedTrace.put("CLASS", symbolRegistry.symbolName(masterRecord.getClassId())); // NON-NLS
		translatedTrace.put("METHOD", symbolRegistry.symbolName(masterRecord.getMethodId())); // NON-NLS
		translatedTrace.put("SIGNATURE", symbolRegistry.symbolName(masterRecord.getSignatureId())); // NON-NLS
		translatedTrace.put("MARKER", symbolRegistry.symbolName(masterRecord.getMarker().getTraceId())); // NON-NLS

		return translatedTrace;
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
	 * Implements {@code ZicoDataProcessor} method. Does nothing.
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

}
