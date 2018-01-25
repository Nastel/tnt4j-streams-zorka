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

/**
 * TNT4J-Streams "Zorka" module constants.
 *
 * @version $Revision: 1 $
 */
public final class ZorkaConstants {
	/**
	 * Resource bundle name constant for TNT4J-Streams "zorka" module.
	 */
	public static final String RESOURCE_BUNDLE_NAME = "tnt4j-streams-zorka"; // NON-NLS

	// Zorka mappings
	/**
	 * The constant for bad reply.
	 */
	public static final String ZORKA_REPLY_BAD = "BAD";// NON-NLS
	/**
	 * The constant ZORKA_PROP_CLOCK. Value to set to clock field of trace record.
	 */
	public static final String ZORKA_PROP_CLOCK = "CLOCK"; // NON-NLS
	/**
	 * The constant ZORKA_PROP_MARKER. Value to set to marker field of trace record.
	 */
	public static final String ZORKA_PROP_MARKER = "MARKER"; // NON-NLS
	/**
	 * The constant ZORKA_PROP_SIGNATURE. Value to set to signature field of trace record.
	 */
	public static final String ZORKA_PROP_SIGNATURE = "SIGNATURE"; // NON-NLS
	/**
	 * The constant ZORKA_PROP_METHOD. Value to set to method field of trace record.
	 */
	public static final String ZORKA_PROP_METHOD = "METHOD"; // NON-NLS
	/**
	 * The constant ZORKA_PROP_CLASS. Value to set to class field of trace record.
	 */
	public static final String ZORKA_PROP_CLASS = "CLASS"; // NON-NLS
	/**
	 * The constant ZORKA_PROP_CALLS. Value to set to calls field of trace record.
	 */
	public static final String ZORKA_PROP_CALLS = "CALLS"; // NON-NLS
	/**
	 * The constant ZORKA_PROP_METHOD_TIME. Value to set to method time field of trace record.
	 */
	public static final String ZORKA_PROP_METHOD_TIME = "METHOD_TIME"; // NON-NLS
	/**
	 * The constant ZORKA_PROP_METHOD_FLAGS. Value to set to flags field of trace record.
	 */
	public static final String ZORKA_PROP_METHOD_FLAGS = "METHOD_FLAGS"; // NON-NLS
	/**
	 * The constant TNT4J_PROP_LEVEL. Level of method trace trace. Where 0 is parent n+1 - child.
	 */
	public static final String TNT4J_PROP_LEVEL = "Level"; // NON-NLS
	/**
	 * The constant TNT4J_PROP_EV_TYPE. Event type field see {@link com.jkoolcloud.tnt4j.core.OpType}.
	 */
	public static final String TNT4J_PROP_EV_TYPE = "EVENT_TYPE"; // NON-NLS
	/**
	 * The constant TRACE_MARKER. Marker to set for {@link com.jitlogic.zorka.common.tracedata.TraceRecord} entities.
	 */
	public static final String TRACE_MARKER = "TRACE"; // NON-NLS

	private ZorkaConstants() {
	}
}
