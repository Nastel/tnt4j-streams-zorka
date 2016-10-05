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
package com.jkoolcloud.tnt4j.streams.filters;

import com.jitlogic.zorka.common.tracedata.TraceRecord;

/**
 * Trace record helper class for marking methods to filter.
 *
 * @version $Revision: 1 $
 *
 * @see TRDynamicFilter
 */
class MarkedTraceRecord extends TraceRecord {
	private static final long serialVersionUID = -7759578796337483731L;

	/**
	 * The constant for flag indicating Bollinger bands values are unknown - data is not yet available.
	 */
	public static final int NO_REFERENCE_METHOD_TIME = 1 << 5;
	/**
	 * The constant for flag indicating method trace record children count is outside of Bollinger bands.
	 */
	public static final int CHILD_METHOD_OUT_BAND = 1 << 6;
	/**
	 * The constant for flag indicating method execution time is outside Bollinger bands.
	 */
	public static final int METHOD_TIME_OUT_BAND = 1 << 7;

	/**
	 * Is flag is set to keep method in trace.
	 *
	 * @param tr
	 *            the {@link TraceRecord} to check
	 * @return {@code true} if method shouldn't be filtered out
	 */
	public static boolean isFlagged(TraceRecord tr) {
		return tr.hasFlag(METHOD_TIME_OUT_BAND) || tr.hasFlag(CHILD_METHOD_OUT_BAND)
				|| tr.hasFlag(NO_REFERENCE_METHOD_TIME);
	}

	/**
	 * Sets flag and propagates it up within {@link TraceRecord} with CHILD_METHOD_OUT_BAND.
	 *
	 * @param flag
	 *            the flag
	 * @param instance
	 *            the instance of {@link TraceRecord} to set
	 */
	public static void setFlag(int flag, TraceRecord instance) {
		instance.markFlag(flag);
		propagateFlagUp(instance, CHILD_METHOD_OUT_BAND);
	}

	private static void propagateFlagUp(TraceRecord tr, int flag) {
		TraceRecord parent = tr.getParent();
		if (parent != null) {
			parent.markFlag(flag);
			propagateFlagUp(parent, flag);
		}

	}
}