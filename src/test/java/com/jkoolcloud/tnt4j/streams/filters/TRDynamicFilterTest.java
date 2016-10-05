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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

/**
 * @author akausinis
 * @version 1.0
 */
public class TRDynamicFilterTest {

	private static final int TR_COUNT = 20;

	@Test
	public void testStatePersisting() {
		SymbolRegistry symbolRegistry = new SymbolRegistry();
		TRDynamicFilter filter = new TRDynamicFilter(TR_COUNT, 3, 3000, symbolRegistry);

		for (int i = 0; i <= TR_COUNT; i++) {
			TraceRecord tr = MarkedTraceRecordTest.makeTestTraceRecord(symbolRegistry);
			filter.filter(tr);
		}

		filter.persistMethodRegistry();
		filter.restoreMethodRegistry();

		assertNotNull(filter);
	}
}
