package com.mendix.audittrail.tests.expected;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import audittrail.proxies.LogLine;
import audittrail.proxies.MemberType;
import audittrail.proxies.TypeOfReferenceLog;
import com.mendix.audittrail.tests.actual.ActualLogLine;
import com.mendix.audittrail.tests.actual.ActualReferenceLog;

public class ExpectedLogLineReference extends ExpectedLogLine {
	final private IContext context;
	final private MemberType memberType;
	final private Map<String, ExpectedReferenceLog> referenceChanges = new HashMap<String, ExpectedReferenceLog>();

	public ExpectedLogLineReference(final IContext context, final MemberType memberType, final Enum<?> memberName) {
		super(memberName.toString());
		this.memberType = memberType;
		this.context = context;
	}

	public ExpectedLogLineReference addLog(final TypeOfReferenceLog typeOfReferenceLog, final IMendixObject reference) {
		this.referenceChanges.put(Long.toString(reference.getId().toLong()),
				new ExpectedReferenceLog(this.context, reference, typeOfReferenceLog));
		return this;
	}

	public void verify(final ActualLogLine actualLogLine) throws CoreException {
		super.verify(actualLogLine);
		final LogLine logLine = actualLogLine.getLogLine();
		assertTrue("oldValue should be empty", logLine.getOldValue().isEmpty());
		assertTrue("newValue shoule be empty", logLine.getNewValue().isEmpty());

		// Now verify that reference logs are all there
		assertThat(
				actualLogLine.getReferenceLogs().stream().map(rl -> rl.getReferenceAttributeId())
						.collect(Collectors.toList()),
				containsInAnyOrder(this.referenceChanges.keySet().toArray(new String[0])));

		// And verify that the reference logs have the correct values
		for (final ActualReferenceLog actualReferenceLog : actualLogLine.getReferenceLogs()) {
			this.referenceChanges.get(actualReferenceLog.getReferenceAttributeId()).verify(actualReferenceLog);
		}
	}

	@Override
	public MemberType getMemberType() {
		return this.memberType;
	}

	@Override
	public boolean hasMemberChanged() {
		return this.referenceChanges.values().stream()
				.anyMatch(ref -> ref.getOperation() != TypeOfReferenceLog.No_Change);
	}
}
