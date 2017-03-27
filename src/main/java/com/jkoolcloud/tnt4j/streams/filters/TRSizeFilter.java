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

import java.util.ArrayList;

import org.apache.commons.collections4.MapUtils;

import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.ZorkaUtils;

/**
 * {@link TraceRecord} filter for filtering method traces by maximum number of trace records to send to JKool Cloud.
 * Filter checks {@link TraceRecord} for most time consuming methods and filter out branches that's not relevant for
 * parent method time.
 *
 * @version $Revision: 1 $
 */
public class TRSizeFilter implements TraceRecordFilter {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(TRSizeFilter.class);

	private int maxTraceEvents;

	/**
	 * Constructs a new TRSizeFilter.
	 *
	 * @param maxTraceEvents
	 *            number of maximum trace events
	 */
	public TRSizeFilter(int maxTraceEvents) {
		this.maxTraceEvents = maxTraceEvents;
	}

	@Override
	public TraceRecord filter(TraceRecord rec) {
		final long recCount = ZorkaUtils.countTraceRecord(rec);
		if (maxTraceEvents == 0 || recCount <= maxTraceEvents) {
			return rec;
		}
		final Long wholeTraceTime = rec.getTime();
		final Float percentageOffset = (float) ((double) maxTraceEvents / (double) recCount);
		TraceRecord filteredRec = cloneTraceRecord(rec, wholeTraceTime, percentageOffset);

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

}
