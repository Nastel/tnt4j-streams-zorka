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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.ZorkaConstants;

/**
 * Implements a Zabbix queries data processor as activity stream, where each query data package is assumed to represent
 * a single activity or event which should be recorded. Zabbix service to query is defined using "Host" and "Port"
 * properties in stream configuration. "JMXQuery" property defines desired to stream JMX beans and attributes.
 * "CronSchedExpr" property defines Cron scheduler expression to invoke Zabbix queries.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data. On trace data package reception, trace
 * fields are packed into {@link Map} data structure.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>Host - host name of machine running Zico service to listen. Default value - 'localhost'. (Optional)</li>
 * <li>Port - port number of machine running Zico service to listen. Default value - '10056'. (Optional)</li>
 * <li>JMXQuery - Zabbix JMX query expression to get desired JMX beans attributes. (Required)</li>
 * <li>CronSchedExpr - Cron expression to define Zabbix queries invocation scheduler. Default value - 'every 15sec'.
 * (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class JMXZabbixDataPuller extends AbstractBufferedStream<Map<String, String>> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JMXZabbixDataPuller.class);

	private static final String DEFAULT_QUARTZ_EXPRESSION = "0/15 * * 1/1 * ? *"; // NON-NLS
	private static final String DEFAULT_HOSTNAME = "localhost"; // NON-NLS
	private static final Integer DEFAULT_PORT = 10056;
	private static final int SOCKET_READ_TIMEOUT = 1350;

	private Scheduler scheduler;
	private Trigger trigger;

	private boolean inputEnd = false;

	private String host = DEFAULT_HOSTNAME;
	private Integer socketPort = DEFAULT_PORT;
	private String jmxQueryString;
	private String jmxSchedulerExpression = DEFAULT_QUARTZ_EXPRESSION;
	private List<String> jmxQueries = new ArrayList<String>();

	private static final String JOB_PROP_STREAM_KEY = "streamObj"; // NON-NLS
	private static final String JOB_PROP_HOST_KEY = "jmx_host"; // NON-NLS
	private static final String JOB_PROP_PORT_KEY = "jmx_port"; // NON-NLS
	private static final String JOB_PROP_JMX_QUERIES_KEY = "queriesObj"; // NON-NLS
	private static final String JMX_QUERIES_DELIMITER = "|"; // NON-NLS

	/**
	 * Constructs an empty JMXZabbixDataPuller. Requires configuration settings to set filter of JMX beans and
	 * attributes for streaming.
	 */
	public JMXZabbixDataPuller() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
				host = value;
			} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
				socketPort = Integer.valueOf(value);
			} else if (ZorkaConstants.PROP_JXM_QUERY.equalsIgnoreCase(name)) {
				jmxQueryString = value;
			} else if (ZorkaConstants.PROP_SCHEDULER_EXPR.equalsIgnoreCase(name)) {
				jmxSchedulerExpression = value;
			}

		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return host;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return socketPort;
		}
		if (ZorkaConstants.PROP_JXM_QUERY.equalsIgnoreCase(name)) {
			return jmxQueryString;
		}
		if (ZorkaConstants.PROP_SCHEDULER_EXPR.equalsIgnoreCase(name)) {
			return jmxSchedulerExpression;
		}

		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Starts scheduler and schedules Cron expression defined job to invoke Zabbix queries.
	 */
	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (StringUtils.isEmpty(jmxQueryString)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", ZorkaConstants.PROP_JXM_QUERY));
		}

		this.scheduler = StdSchedulerFactory.getDefaultScheduler();
		this.scheduler.start();

		JobDataMap jobAttrs = new JobDataMap();
		jobAttrs.put(JOB_PROP_STREAM_KEY, this);
		jobAttrs.put(JOB_PROP_HOST_KEY, host);
		jobAttrs.put(JOB_PROP_PORT_KEY, socketPort);
		StringTokenizer tokenizer = new StringTokenizer(jmxQueryString, JMX_QUERIES_DELIMITER);
		while (tokenizer.hasMoreElements()) {
			jmxQueries.add(tokenizer.nextToken().trim());
		}
		jobAttrs.put(JOB_PROP_JMX_QUERIES_KEY, jmxQueries);

		CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(jmxSchedulerExpression);

		JobDetail job = JobBuilder.newJob(ZabbixCallJob.class).usingJobData(jobAttrs).build();
		trigger = TriggerBuilder.newTrigger().withIdentity(job.getKey() + "Trigger").startNow() // NON-NLS
				.withSchedule(scheduleBuilder).build();
		scheduler.scheduleJob(job, trigger);
	}

	@Override
	protected void cleanup() {
		if (scheduler != null) {
			try {
				scheduler.shutdown(true);
			} catch (SchedulerException exc) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME,
						"JMXZabbixDataPuller.error.closing.scheduler"), exc);
			}
		}

		inputEnd = true;

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		return inputEnd || !trigger.mayFireAgain();
	}

	@Override
	protected long getActivityItemByteSize(Map<String, String> activityItem) {
		return 0; // TODO
	}

	/**
	 * Scheduler job to invoke Zabbix query calls.
	 */
	public static class ZabbixCallJob implements Job {

		/**
		 * Constructs a new ZabbixCallJob.
		 */
		public ZabbixCallJob() {
		}

		@Override
		@SuppressWarnings("unchecked")
		public void execute(JobExecutionContext context) throws JobExecutionException {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			JMXZabbixDataPuller stream = (JMXZabbixDataPuller) dataMap.get(JOB_PROP_STREAM_KEY);
			String host = dataMap.getString(JOB_PROP_HOST_KEY);
			Integer socketPort = dataMap.getInt(JOB_PROP_PORT_KEY);
			List<String> jmxQueries = (List<String>) dataMap.get(JOB_PROP_JMX_QUERIES_KEY);

			final Map<String, String> inputData = new HashMap<String, String>();
			Socket echoSocket = null;
			PrintWriter out = null;
			BufferedReader in = null;
			InputStream is = null;
			for (String query : jmxQueries) {
				try {
					echoSocket = new Socket(host, socketPort);
					echoSocket.setSoTimeout(SOCKET_READ_TIMEOUT);
					out = new PrintWriter(echoSocket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
					is = mkIS("ZBXD", 1, 0x0c, 0, 0, 0, 0, 0, 0, 0, "zorka.jmx[", query, "]", 0x0a); // NON-NLS
					IOUtils.copy(is, out, Utils.UTF8);
					out.flush();
					final String response = in.readLine();
					if (response != null) {
						inputData.put(query, response.substring(13));
					}
				} catch (IOException exc) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(ZorkaConstants.RESOURCE_BUNDLE_NAME,
							"JMXZabbixDataPuller.request.error"), host, socketPort, query);
				} finally {
					Utils.close(is);
					Utils.close(out);
					Utils.close(in);
					Utils.close(echoSocket);
				}
			}
			stream.addInputToBuffer(inputData);
		}

		private static InputStream mkIS(Object... args) {
			byte buf[] = new byte[2048];
			int pos = 0;
			for (Object arg : args) {
				if (arg instanceof String)
					for (char ch : ((String) arg).toCharArray())
						buf[pos++] = ((byte) ch);
				else if (arg instanceof Integer)
					buf[pos++] = ((byte) (int) (Integer) arg);
				else if (arg instanceof Byte)
					buf[pos++] = ((Byte) arg);
			}

			return new ByteArrayInputStream(buf, 0, pos);
		}
	}
}
