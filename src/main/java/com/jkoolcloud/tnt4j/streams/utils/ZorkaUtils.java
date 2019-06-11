/*
 * Copyright 2014-2019 JKOOL, LLC.
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

import com.jitlogic.zorka.common.tracedata.TraceRecord;

/**
 * Utility methods used by Zorka module.
 *
 * @version $Revision: 1 $
 */
public final class ZorkaUtils {

	private ZorkaUtils() {

	}

	/**
	 * Counts all children of trace record tree.
	 *
	 * @param tr
	 *            trace record to count children
	 * @return children count of trace record
	 */
	public static long countTraceRecord(TraceRecord tr) {
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
}
