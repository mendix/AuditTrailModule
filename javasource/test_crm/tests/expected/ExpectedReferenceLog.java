package test_crm.tests.expected;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IMendixObjectMember;

import audittrail.proxies.ReferenceLog;
import audittrail.proxies.TypeOfReferenceLog;
import test_crm.tests.actual.ActualReferenceLog;

public class ExpectedReferenceLog {
	private final TypeOfReferenceLog operation;
	private final Long referenceLogId;
	private final Map<String, String> memberValues;

	public ExpectedReferenceLog(final IContext context, final IMendixObject referencedObject,
			final TypeOfReferenceLog operation) {
		this.referenceLogId = referencedObject.getId().toLong();
		this.operation = operation;

		this.memberValues = new HashMap<String, String>();
		for (final IMendixObjectMember<?> obj : referencedObject.getPrimitives(context)) {
			memberValues.put(obj.getName(), obj.getValue(context).toString());
		}
	}

	public void verify(final ActualReferenceLog actualLogLine) {
		final ReferenceLog reference = actualLogLine.getReferenceLog();

		assertEquals("ID mismatch when comparing reference logs.", this.referenceLogId.toString(),
				reference.getAttributeID());
		assertEquals("Incorrect reference log operation for reference to object of ID " + reference.getAttributeID(),
				this.operation, reference.getOperation());

		final Map<String, String> actualReferenceLogLine = actualLogLine.getReferenceLogLines().stream()
				.collect(Collectors.toMap(ll -> ll.getMember(), ll -> ll.getValue()));

		assertThat(actualReferenceLogLine.entrySet().toArray(),
				arrayContainingInAnyOrder(memberValues.entrySet().toArray()));
	}

	public TypeOfReferenceLog getOperation() {
		return this.operation;
	}
}
