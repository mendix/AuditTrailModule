package test_crm.tests.actual;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;

import audittrail.proxies.LogLine;
import audittrail.proxies.ReferenceLog;

public class ActualLogLine {
	private final LogLine logLine;
	private final List<ActualReferenceLog> references;

	protected static ActualLogLine getActualLogLine(final IContext context, final LogLine logLine) {
		List<ActualReferenceLog> references = null;
		try { // Catch the exception so we can use map with this static method
			references = getReferenceLogsForLogLine(context, logLine).stream()
					.map(rl -> ActualReferenceLog.getActualReferenceLog(context, rl)).collect(Collectors.toList());
		} catch (CoreException e) {
			fail(e.getMessage());
		}
		return new ActualLogLine(logLine, references);
	}

	private ActualLogLine(final LogLine logLine, final List<ActualReferenceLog> references) {
		this.logLine = logLine;
		this.references = references;
	}

	public LogLine getLogLine() {
		return logLine;
	}

	public List<ActualReferenceLog> getReferenceLogs() {
		return Collections.unmodifiableList(references);
	}

	public String getLogLineMemberName() {
		return this.logLine.getMember();
	}

	private static List<ReferenceLog> getReferenceLogsForLogLine(final IContext context, final LogLine logLine)
			throws CoreException {
		return ReferenceLog.load(context,
				String.format("[%1s/%2s/ID = %3d]", ReferenceLog.MemberNames.ReferenceLog_LogLine, LogLine.entityName,
						logLine.getMendixObject().getId().toLong()));
	}
}
