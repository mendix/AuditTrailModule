package com.mendix.audittrail.tests.expected;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mendix.core.CoreException;

import audittrail.proxies.LogLine;
import audittrail.proxies.MemberType;
import com.mendix.audittrail.tests.actual.ActualLogLine;

public class ExpectedLogLineAttribute extends ExpectedLogLine {
	private final String oldValue;
	private final String newValue;

	public ExpectedLogLineAttribute(final Enum<?> memberName, final Object oldValue, final Object newValue) {
		super(memberName.toString());
		this.oldValue = oldValue.toString();
		this.newValue = newValue.toString();
	}

	public void verify(final ActualLogLine actualLogLine) throws CoreException {
		super.verify(actualLogLine);
		final LogLine logLine = actualLogLine.getLogLine();
		assertEquals("Incorrect oldValue", this.oldValue, logLine.getOldValue());
		assertEquals("Incorrect newValue", this.newValue, logLine.getNewValue());
		assertTrue("Attribute with logged reference.", actualLogLine.getReferenceLogs().isEmpty());
	}

	@Override
	public MemberType getMemberType() {
		return MemberType.Attribute;
	}

	@Override
	public boolean hasMemberChanged() {
		return !this.oldValue.equals(this.newValue);
	}
}