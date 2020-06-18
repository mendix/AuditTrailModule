package test_crm.tests.actual;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;

import audittrail.proxies.ReferenceLog;
import audittrail.proxies.ReferenceLogLine;

public class ActualReferenceLog {
	private final ReferenceLog referenceLog;
	private final List<ReferenceLogLine> referrenceLogLines;

	public static ActualReferenceLog getActualReferenceLog(final IContext context, final ReferenceLog referenceLog) {
		List<ReferenceLogLine> referenceLogLines = null;
		try { // Catch the exception so we can use map with this static method
			referenceLogLines = getReferenceLogLineForReferenceLog(context, referenceLog);
		} catch (CoreException e) {
			fail(e.getMessage());
		}
		return new ActualReferenceLog(referenceLog, referenceLogLines);

	}

	private ActualReferenceLog(final ReferenceLog referenceLog, List<ReferenceLogLine> referenceLogLines) {
		this.referenceLog = referenceLog;
		this.referrenceLogLines = referenceLogLines;
	}

	public ReferenceLog getReferenceLog() {
		return referenceLog;
	}

	public List<ReferenceLogLine> getReferenceLogLines() {
		return Collections.unmodifiableList(this.referrenceLogLines);
	}

	public String getReferenceAttributeId() {
		return this.referenceLog.getAttributeID();
	}

	private static List<ReferenceLogLine> getReferenceLogLineForReferenceLog(final IContext context,
			final ReferenceLog referenceLog) throws CoreException {
		return ReferenceLogLine.load(context,
				String.format("[%1s/%2s/ID = %3d]", ReferenceLogLine.MemberNames.ReferenceLogLine_ReferenceLog,
						ReferenceLog.entityName, referenceLog.getMendixObject().getId().toLong()));
	}
}
