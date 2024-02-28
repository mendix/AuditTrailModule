package com.mendix.audittrail.tests;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.MendixRuntimeException;

import org.junit.Test;

import audittrail.actions.CreateLogRecordOfObject;
import audittrail.proxies.Log;
import audittrail.proxies.MemberType;
import audittrail.proxies.TypeOfLog;
import test_crm.proxies.Company;
import com.mendix.audittrail.tests.actual.ActualLog;
import com.mendix.audittrail.tests.expected.ExpectedLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static test_crm.proxies.Company.MemberNames.Founded;
import static test_crm.proxies.Company.MemberNames.Name;
import static test_crm.proxies.Company.MemberNames.Company_Group;

/**
 * This class tests adding, changing and deleting objects of Company. Company is
 * audited through inheritance. That is, it inherits from AudittrailSuperClass
 * and this should be enough to trigger its audit actions.
 * 
 * Company has 3 attributes and an association to a set of groups. Logging of
 * this association is also tested in this class.
 */
public class TestAuditInheritance extends TestAuditWithData {

	// Testing records without reference

	@Test
	public void testCreateRecord() throws CoreException {
		// Create new record
		final Company company = createBaseCompany();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add, company);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testChangeRecord() throws CoreException {
		// Create new record
		final Company company = createBaseCompany();

		company.setName(NAME2);
		company.setDec(new java.math.BigDecimal("0.00000000")); // should not be considered changed
		company.setFounded(FOUNDED_DATE);
		company.commit();

		// Assert log was created
		final ExpectedLog expectedLog =
			createExpectedLog(TypeOfLog.Change, company)
				.changeAttribute(Name, NAME, NAME2)
				.changeAttribute(Founded, "", "11/04/1991 (UTC)");

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());

		expectedLog.verify(actualLog);
	}

	@Test
	public void testDeleteRecord() throws CoreException {
		// Create new record
		final Company company = createBaseCompany();

		company.delete();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Delete, company);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	// Testing records with reference

	@Test
	public void testCreateRecordWithReference() throws CoreException {
		// Create new record
		final Company company = createBaseCompany(group, group2);

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add, company, groupObject, group2Object);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testChangeRecordWithReference() throws CoreException {
		// Create new record
		final Company company = createBaseCompany(group, group2);

		company.setName(NAME2);
		company.commit();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Change, company, groupObject, group2Object)
				.changeAttribute(Name, NAME, NAME2);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testDeleteRecordWithReference() throws CoreException {
		// Create new record
		final Company company = createBaseCompany(group, group2);

		company.delete();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Delete, company, groupObject, group2Object);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	// Testing reference changes

	@Test
	public void testRemoveReference() throws CoreException {
		// Create new record
		final Company company = createBaseCompany(group, group2);

		company.setCompany_Group(Collections.emptyList());
		company.commit();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Change, company, groupObject, group2Object)
				.deleteReferences(Company_Group, context, MemberType.ReferenceSet, groupObject, group2Object);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testChangeReference() throws CoreException {
		// Create new record
		final Company company = createBaseCompany(group);

		company.setCompany_Group(Collections.singletonList(group2));
		company.commit();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Change, company, groupObject)
				.deleteReferences(Company_Group, context, MemberType.ReferenceSet, groupObject)
				.addReferences(Company_Group, context, MemberType.ReferenceSet, group2Object);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testOnCommitAction() throws Exception {
		// Arrange
		final Company company = new Company(context);
		company.setName(NAME);
		company.setCompanyNr(COMPANY_NR);

		// Act: do not commit the company, but call the on-commit action directly
		MendixRuntimeException exception = assertThrows(
				MendixRuntimeException.class,
				() -> { new CreateLogRecordOfObject(context, company.getMendixObject()).executeAction(); }
		);

		// Assert
		String actualError = exception.getCause().getMessage();
		String expectedError = "Autocommitted objects detected at end of transaction for system session for entities:";
		assertTrue(String.format("expected: '%s'; actual: '%s'", actualError, expectedError), actualError.startsWith(expectedError));
	}

	@Test
	public void testNoLogOnRollback() throws Exception {
		// Arrange
		int logsBefore = Core.createXPathQuery(String.format("//%1s", Log.entityName)).execute(context).size();

		// Act: commit the company inside a transaction and then roll back
		context.startTransaction();
		final Company company = createBaseCompany(group);
		context.rollbackTransaction();

		// Assert
		int logsAfter = Core.createXPathQuery(String.format("//%1s", Log.entityName)).execute(context).size();
		assertEquals("No logs should be added for a rolled back commit", logsBefore, logsAfter);
	}

	private static Date createDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 1991);
		calendar.set(Calendar.MONTH, 10);
		calendar.set(Calendar.DATE, 4);
		calendar.set(Calendar.HOUR, 2);
		return calendar.getTime();
	}
	
	private static final String NAME2 = "Company2";
	private static final Date FOUNDED_DATE = createDate();
}
