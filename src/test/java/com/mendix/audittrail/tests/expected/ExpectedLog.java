package com.mendix.audittrail.tests.expected;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import audittrail.proxies.Log;
import audittrail.proxies.MemberType;
import audittrail.proxies.TypeOfLog;
import audittrail.proxies.TypeOfReferenceLog;
import system.proxies.User;
import com.mendix.audittrail.tests.actual.ActualLog;
import com.mendix.audittrail.tests.actual.ActualLogLine;

public class ExpectedLog {
	// The model is inconsistent with the enum name and calls the following
	// "logType"
	private final TypeOfLog typeOfLog;
	// The model calls the following "logObject", but it is only the name of the
	// entity to which this log refers to
	private final String entityTypeName;
	private final User logUser;
	private final Date afterDate;
	private final String superClassName;

	private final Map<String, ExpectedLogLine> logLines = new HashMap<String, ExpectedLogLine>();

	public ExpectedLog(final TypeOfLog typeOfLog, final String entityTypeName, final User logUser,
			final Date afterDate) {
		this(typeOfLog, entityTypeName, logUser, afterDate, null);
	}

	public ExpectedLog(final TypeOfLog typeOfLog, final String entityTypeName, final User logUser, final Date afterDate,
			final String superClassName) {
		this.typeOfLog = typeOfLog;
		this.entityTypeName = entityTypeName;
		this.logUser = logUser;
		this.afterDate = afterDate;
		this.superClassName = superClassName;
	}

	public void verify(final ActualLog actualLog) throws CoreException {
		final Log log = actualLog.getLog();
		assertEquals("Incorrect number of changed members.", this.getNumberOfChangedMembers(),
				log.getNumberOfChangedMembers());
		assertEquals("Incorrect type of change.", this.typeOfLog, log.getLogType());
		assertEquals("Wrong type of entity for log entry.", this.entityTypeName, log.getLogObject());

		assertEquals("Incorrect log description.", this.createDescriptionTest(log.getDateTime()), log.getDescription());
		assertEquals("Incorrect user for log entry.", this.logUser, log.getLog_User());

		assertTrue("Time of change before test was run.", log.getDateTime().after(this.afterDate));
		assertTrue("Time of change after this assertion.", log.getDateTime().before(new Date()));

		// We cannot check for the superclass if the object has been deleted
		if (this.superClassName == null || this.typeOfLog.equals(TypeOfLog.Delete)) {
			assertNull("There should be no reference to a superclass for this log.", log.getLog_AudittrailSuperClass());
		} else {
			assertEquals("Incorrect superclass for log.", this.superClassName,
					log.getLog_AudittrailSuperClass().getMendixObject().getType());
		}

		// Now verify log lines are all there
		assertThat(actualLog.getLogLines().stream().map(al -> al.getLogLineMemberName()).collect(Collectors.toList()),
				containsInAnyOrder(this.logLines.keySet().toArray(new String[0])));

		// And verify that the log lines have the correct values
		for (final ActualLogLine actualLogLine : actualLog.getLogLines()) {
			this.logLines.get(actualLogLine.getLogLineMemberName()).verify(actualLogLine);
		}
	}

	public ExpectedLog addAttribute(final Enum<?> attribute, final Object value) {
		this.addExpectedLogLine(new ExpectedLogLineAttribute(attribute, EMPTY_VALUE, value.toString()));
		return this;
	}

	public ExpectedLog keepAttribute(final Enum<?> attribute, final Object value) {
		this.addExpectedLogLine(new ExpectedLogLineAttribute(attribute, value.toString(), value.toString()));
		return this;
	}

	public ExpectedLog changeAttribute(final Enum<?> attribute, final Object oldValue, final Object newValue) {
		this.addExpectedLogLine(new ExpectedLogLineAttribute(attribute, oldValue.toString(), newValue.toString()));
		return this;
	}

	public ExpectedLog addReferences(final Enum<?> attribute, final IContext context, final MemberType referenceType,
			final IMendixObject... objects) {
		addReferenceLogs(attribute, TypeOfReferenceLog.Added, context, referenceType, objects);
		return this;
	}

	public ExpectedLog keepReferences(final Enum<?> attribute, final IContext context, final MemberType referenceType,
			final IMendixObject... objects) {
		addReferenceLogs(attribute, TypeOfReferenceLog.No_Change, context, referenceType, objects);
		return this;
	}

	public ExpectedLog deleteReferences(final Enum<?> attribute, final IContext context, final MemberType referenceType,
			final IMendixObject... objects) {
		addReferenceLogs(attribute, TypeOfReferenceLog.Deleted, context, referenceType, objects);
		return this;
	}

	private void addReferenceLogs(final Enum<?> attribute, final TypeOfReferenceLog typeOfReferenceLog,
			final IContext context, final MemberType referenceType, final IMendixObject... objects) {
		final String attributeName = attribute.toString();

		this.logLines.computeIfAbsent(attributeName,
				x -> new ExpectedLogLineReference(context, referenceType, attribute));
		ExpectedLogLineReference logLine = (ExpectedLogLineReference) this.logLines.get(attributeName);

		Arrays.asList(objects).stream().forEach(obj -> logLine.addLog(typeOfReferenceLog, obj));
	}

	private void addExpectedLogLine(final ExpectedLogLine logLine) {
		final String keyName = logLine.getMemberName();

		if (this.logLines.containsKey(keyName))
			this.logLines.replace(keyName, logLine);
		else
			this.logLines.put(keyName, logLine);
	}

	private String createDescriptionTest(final Date actualLogDateTime) {
		final StringBuilder builder = new StringBuilder();

		final int numberOfChangedMembers = this.getNumberOfChangedMembers();
		builder.append(numberOfChangedMembers);
		if (numberOfChangedMembers == 1)
			builder.append(" attribute changed by  ");
		else
			builder.append(" attributes changed by  ");

		if (this.logUser == null) {
			builder.append("[unknown]");
		} else {
			builder.append(this.logUser.getName());
		}
		builder.append(" on ");
		// Assuming we're not using US locale for testing
		final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss '(UTC)'");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

		builder.append(dateFormatter.format(actualLogDateTime));

		return builder.toString();
	}

	private Integer getNumberOfChangedMembers() {
		// If the record was added, all lines are new!
		if (this.typeOfLog == TypeOfLog.Add)
			return this.logLines.size();
		else
			return (int) this.logLines.values().stream().filter(mem -> mem.hasMemberChanged()).count();
	}

	private final static String EMPTY_VALUE = "";
}
