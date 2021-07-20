package test_crm.tests;

import java.util.Arrays;
import java.util.Collections;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import org.junit.Test;

import audittrail.proxies.MemberType;
import audittrail.proxies.TypeOfLog;
import test_crm.proxies.Company;
import test_crm.proxies.Group;
import test_crm.tests.actual.ActualLog;
import test_crm.tests.expected.ExpectedLog;

import static test_crm.proxies.Company.MemberNames.Name;
import static test_crm.proxies.Company.MemberNames.CompanyNr;
import static test_crm.proxies.Company.MemberNames.InternNr;
import static test_crm.proxies.Company.MemberNames.Company_Group;

/**
 * This class tests adding, changing and deleting objects of Company. Company is
 * audited through inheritance. That is, it inherits from AudittrailSuperClass
 * and this should be enough to trigger its audit actions.
 * 
 * Company has 3 attributes and an association to a set of groups. Logging of
 * this association is also tested in this class.
 */
public class TestAuditInheritance extends TestAuditBase {

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
		company.commit();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Change, company).changeAttribute(Name, NAME, NAME2);

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

	private Company createBaseCompany(final Group... groups) throws CoreException {
		final Company company = new Company(context);

		company.setName(NAME);
		company.setCompanyNr(COMPANY_NR);

		company.setCompany_Group(Arrays.asList(groups));

		company.commit();
		return company;
	}

	private ExpectedLog createExpectedLog(final TypeOfLog typeOfLog, final Company company,
			final IMendixObject... groupObjects) {
		if (typeOfLog.equals(TypeOfLog.Add)) {
			return new ExpectedLog(typeOfLog, Company.entityName, admin, initialDate, Company.entityName)
					.addAttribute(Name, NAME).addAttribute(CompanyNr, COMPANY_NR)
					.addAttribute(InternNr, company.getInternNr())
					.addReferences(Company_Group, context, MemberType.ReferenceSet, groupObjects);
		} else {
			return new ExpectedLog(typeOfLog, Company.entityName, admin, initialDate, Company.entityName)
					.keepAttribute(Name, NAME).keepAttribute(CompanyNr, COMPANY_NR)
					.keepAttribute(InternNr, company.getInternNr())
					.keepReferences(Company_Group, context, MemberType.ReferenceSet, groupObjects);
		}
	}

	private static final String NAME = "Company";
	private static final String NAME2 = "Company2";
	private static final String COMPANY_NR = "123";
}
