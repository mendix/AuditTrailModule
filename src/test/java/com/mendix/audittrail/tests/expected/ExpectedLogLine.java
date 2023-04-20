
package com.mendix.audittrail.tests.expected;

import static org.junit.Assert.assertEquals;

import com.mendix.core.CoreException;

import audittrail.proxies.LogLine;
import audittrail.proxies.MemberType;
import com.mendix.audittrail.tests.actual.ActualLogLine;

public abstract class ExpectedLogLine {
	private final String memberName;

	protected ExpectedLogLine(final String memberName) {
		this.memberName = memberName;
	}

	public void verify(final ActualLogLine actualLogLine) throws CoreException {
		final LogLine logLine = actualLogLine.getLogLine();
		assertEquals("Incorrect member type", this.getMemberType(), logLine.getMemberType());
		assertEquals("Incorrect member name", this.memberName, logLine.getMember());
	}

	public String getMemberName() {
		return this.memberName;
	}

	abstract public MemberType getMemberType();

	abstract public boolean hasMemberChanged();
}
