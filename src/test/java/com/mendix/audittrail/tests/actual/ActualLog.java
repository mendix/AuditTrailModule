package com.mendix.audittrail.tests.actual;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import audittrail.proxies.Log;
import audittrail.proxies.LogLine;

public class ActualLog {
	private final Log log;
	private final List<ActualLogLine> logLines;

	public static ActualLog getLastLog(final IContext context, final long objectId) throws CoreException {
		final Log log = getLastLogForId(context, objectId);

		final List<ActualLogLine> logLines = getLogLinesForLog(context, log).stream()
				.map(ll -> ActualLogLine.getActualLogLine(context, ll)).collect(Collectors.toList());

		return new ActualLog(log, logLines);
	}

	private ActualLog(final Log log, final List<ActualLogLine> logLines) {
		this.log = log;
		this.logLines = logLines;
	}

	public Log getLog() {
		return this.log;
	}

	public List<ActualLogLine> getLogLines() {
		return Collections.unmodifiableList(this.logLines);
	}

	private static Log getLastLogForId(final IContext context, final long objectId) throws CoreException {
		// Using XPath to sort by date
		final String xpathQuery =
			String.format("//%1s[%2s = '%3d']", Log.entityName, Log.MemberNames.ReferenceId, objectId);
		final List<IMendixObject> objects =
			Core.createXPathQuery(xpathQuery)
				.setAmount(1)
				.setOffset(0)
				.addSort(Log.MemberNames.DateTime.name(), false)
				.execute(context);

		return Log.initialize(context, objects.get(0));
	}

	private static List<LogLine> getLogLinesForLog(final IContext context, final Log log) throws CoreException {
		return LogLine.load(context, String.format("[%1s/%2s/ID = %3d]", LogLine.MemberNames.LogLine_Log,
				Log.entityName, log.getMendixObject().getId().toLong()));
	}
}
