package com.mendix.audittrail.tests;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import org.junit.Test;

import audittrail.proxies.MemberType;
import audittrail.proxies.TypeOfLog;
import test_crm.proxies.ContactPerson;
import com.mendix.audittrail.tests.actual.ActualLog;
import com.mendix.audittrail.tests.expected.ExpectedLog;

import static test_crm.proxies.ContactPerson.MemberNames.Firstname;
import static test_crm.proxies.ContactPerson.MemberNames.Surname;
import static test_crm.proxies.ContactPerson.MemberNames.ContactPerson_Group;

/**
 * This class tests adding, changing and deleting objects of ContactPerson.
 * ContactPerson is audited through explicit creation of before-commit and
 * before-delete triggers in the entity to be audited.
 * 
 * ContactPerson has 2 attributes and an association to a single Group. Logging
 * of this association is also tested in this class.
 */
public class TestAuditTrigger extends TestAuditBase {

	// Testing records without reference

	@Test
	public void testCreateRecord() throws CoreException {
		// Create new record
		final ContactPerson contactPerson = createBasePerson(false);

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add);

		final ActualLog actualLog = ActualLog.getLastLog(context, contactPerson.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testChangeRecord() throws CoreException {
		// Create new record
		final ContactPerson contactPerson = createBasePerson(false);

		// Change record
		contactPerson.setSurname(context, SURNAME2);
		contactPerson.commit();

		// Verify that the record has been created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Change).changeAttribute(Surname, SURNAME, SURNAME2);

		final ActualLog actualLog = ActualLog.getLastLog(context, contactPerson.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testDeleteRecord() throws CoreException {
		// Create new record
		final ContactPerson contactPerson = createBasePerson(false);

		// Delete record
		contactPerson.delete();

		// Verify that the record has been created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Delete);

		final ActualLog actualLog = ActualLog.getLastLog(context, contactPerson.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	// Testing records with reference

	@Test
	public void testCreateRecordWithReference() throws CoreException {
		// Create new record
		final ContactPerson contactPerson = createBasePerson(true);

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add, groupObject);

		final ActualLog actualLog = ActualLog.getLastLog(context, contactPerson.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testChangeRecordWithReference() throws CoreException {
		// Create new record
		final ContactPerson contactPerson = createBasePerson(true);

		// Change record
		contactPerson.setSurname(context, SURNAME2);
		contactPerson.commit();

		// Verify that the record has been created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Change, groupObject).changeAttribute(Surname,
				SURNAME, SURNAME2);

		final ActualLog actualLog = ActualLog.getLastLog(context, contactPerson.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testDeleteRecordWithReference() throws CoreException {
		// Create new record
		final ContactPerson contactPerson = createBasePerson(true);

		// Delete record
		contactPerson.delete();

		// Verify that the record has been created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Delete, groupObject);

		final ActualLog actualLog = ActualLog.getLastLog(context, contactPerson.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	// Testing reference changes

	@Test
	public void testRemoveReference() throws CoreException {
		// Create new record
		final ContactPerson contactPerson = createBasePerson(true);

		// Change record
		contactPerson.setContactPerson_Group(context, null);
		contactPerson.commit();

		// Verify that the record has been created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Change, groupObject)
				.deleteReferences(ContactPerson_Group, context, MemberType.ReferenceSet, groupObject);

		final ActualLog actualLog = ActualLog.getLastLog(context, contactPerson.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testChangeReference() throws CoreException {
		// Create new record
		final ContactPerson contactPerson = createBasePerson(true);

		// Change record
		contactPerson.setContactPerson_Group(context, this.group2);
		contactPerson.commit();

		// Verify that the record has been created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Change, groupObject)
				.deleteReferences(ContactPerson_Group, context, MemberType.ReferenceSet, groupObject)
				.addReferences(ContactPerson_Group, context, MemberType.ReferenceSet, group2Object);

		final ActualLog actualLog = ActualLog.getLastLog(context, contactPerson.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	private ContactPerson createBasePerson(final boolean withGroup) throws CoreException {
		final ContactPerson contactPerson = new ContactPerson(context);

		contactPerson.setFirstname(context, FIRSTNAME);
		contactPerson.setSurname(context, SURNAME);

		if (withGroup)
			contactPerson.setContactPerson_Group(this.group);

		contactPerson.commit();
		return contactPerson;
	}

	private ExpectedLog createExpectedLog(final TypeOfLog typeOfLog, final IMendixObject... groupObject) {
		if (typeOfLog.equals(TypeOfLog.Add)) {
			return new ExpectedLog(typeOfLog, ContactPerson.entityName, admin, initialDate)
					.addAttribute(Firstname, FIRSTNAME).addAttribute(Surname, SURNAME)
					.addReferences(ContactPerson_Group, context, MemberType.Reference, groupObject);
		} else {
			return new ExpectedLog(typeOfLog, ContactPerson.entityName, admin, initialDate)
					.keepAttribute(Firstname, FIRSTNAME).keepAttribute(Surname, SURNAME)
					.keepReferences(ContactPerson_Group, context, MemberType.Reference, groupObject);
		}
	}

	private static final String FIRSTNAME = "Firstname";
	private static final String SURNAME = "Surname";
	private static final String SURNAME2 = "Surname2";
}
