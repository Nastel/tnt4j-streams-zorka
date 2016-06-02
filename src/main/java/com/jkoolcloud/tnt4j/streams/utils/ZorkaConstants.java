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

	/**
	 * Property name defining Zorka traces filtering to produce max number of
	 * events for method stack trace.
	 */
	public static final String PROP_MAX_TRACE_EVENTS = "MaxTraceEvents"; // NON-NLS

	/**
	 * Constant for name of built-in Zorka connector {@value} property.
	 */
	public static final String PROP_JXM_QUERY = "JMXQuery"; // NON-NLS

	/**
	 * Constant for name of built-in Zorka connector {@value} property.
	 */
	public static final String PROP_SCHEDULER_EXPR = "CronSchedExpr"; // NON-NLS

	private ZorkaConstants() {

	}
}
