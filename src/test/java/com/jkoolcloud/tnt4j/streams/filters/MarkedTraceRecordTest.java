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

package com.jkoolcloud.tnt4j.streams.filters;

import static com.jkoolcloud.tnt4j.streams.filters.MarkedTraceRecord.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

/**
 * @author akausinis
 * @version 1.0
 */
public class MarkedTraceRecordTest {

	SymbolRegistry symbolRegistry = new SymbolRegistry();

	private static MarkedTraceRecord tr(String className, String methodName, String methodSignature,
			SymbolRegistry symbolRegistry) {
		MarkedTraceRecord tr = new MarkedTraceRecord();
		tr.setClassId(symbolRegistry.symbolId(className));
		tr.setMethodId(symbolRegistry.symbolId(methodName));
		tr.setSignatureId(symbolRegistry.symbolId(methodSignature));
		return tr;
	}

	@Test
	public void flagTest() {
		TraceRecord tr1 = makeTestTraceRecord(symbolRegistry);
		TraceRecord tr2 = tr1.getChild(0);
		TraceRecord tr3 = tr2.getChild(0);

		setFlag(MarkedTraceRecord.METHOD_TIME_OUT_BAND, tr2);

		assertFalse(isFlagged(tr3));
		assertTrue(isFlagged(tr1));
		assertTrue(isFlagged(tr2));
		assertTrue(tr2.hasFlag(METHOD_TIME_OUT_BAND));
		assertTrue(tr1.hasFlag(CHILD_METHOD_OUT_BAND));

		assertFalse(tr1.hasFlag(METHOD_TIME_OUT_BAND));
		assertFalse(tr2.hasFlag(CHILD_METHOD_OUT_BAND));

	}

	public static TraceRecord makeTestTraceRecord(SymbolRegistry symbolRegistry) {
		MarkedTraceRecord tr1;
		MarkedTraceRecord tr2;
		MarkedTraceRecord tr3;

		tr1 = tr("com.mysql.jdbc.PreparedStatement", "execute", "()V", symbolRegistry); // NON-NLS
		tr1.setAttr(symbolRegistry.symbolId("SQL"), "select 1"); // NON-NLS
		tr2 = tr("com.mysql.jdbc.Statement", "executeUpdate", "()V", symbolRegistry); // NON-NLS
		tr3 = tr("com.mysql.jdbc.PreparedStatement", "execute", "(Ljava/lang/String;)V", symbolRegistry); // NON-NLS
		tr1.addChild(tr2);
		tr2.addChild(tr3);
		return tr1;
	}

}
