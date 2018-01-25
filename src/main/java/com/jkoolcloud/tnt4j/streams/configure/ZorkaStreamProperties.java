/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.configure;

/**
 * Lists predefined property names used by TNT4-Streams Zorka input streams.
 *
 * @version $Revision: 1 $
 */
public interface ZorkaStreamProperties extends StreamProperties {

	/**
	 * Property name defining Zorka traces filtering to produce max number of events for method stack trace.
	 */
	String PROP_MAX_TRACE_EVENTS = "MaxTraceEvents"; // NON-NLS

	/**
	 * Constant for name of built-in Zorka connector {@value} property.
	 */
	String PROP_JMX_QUERY = "JMXQuery"; // NON-NLS

	/**
	 * Constant for name of built-in Zorka connector {@value} property.
	 */
	String PROP_SCHEDULER_EXPR = "CronSchedExpr"; // NON-NLS

	/**
	 * Constant for property filtering Zorka trace by method time
	 * {@link com.jkoolcloud.tnt4j.streams.filters.TRDynamicFilter}. Bollinger K times an N-period standard deviation
	 * above the EMA(nPeriod).
	 */
	String PROP_BB_K_TIMES = "Bollinger_K_times"; // NON-NLS

	/**
	 * Constant for property filtering Zorka trace by method time
	 * {@link com.jkoolcloud.tnt4j.streams.filters.TRDynamicFilter}. Bollinger nPeriod an N-period moving average (EMA).
	 */
	String PROP_BB_N_PERIOD = "Bollinger_N_period"; // NON-NLS

	/**
	 * Constant for property filtering Zorka trace by method time
	 * {@link com.jkoolcloud.tnt4j.streams.filters.TRDynamicFilter}. Bollinger bands recalculation time.
	 */
	String PROP_BB_RECALCULATION_TIME = "BollingerRecalculationPeriod"; // NON-NLS
}
