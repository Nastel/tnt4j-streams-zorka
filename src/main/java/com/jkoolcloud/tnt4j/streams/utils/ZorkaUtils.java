/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
