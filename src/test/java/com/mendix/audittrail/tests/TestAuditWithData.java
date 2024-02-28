package com.mendix.audittrail.tests;

import audittrail.proxies.MemberType;
import audittrail.proxies.TypeOfLog;
import com.mendix.audittrail.tests.expected.ExpectedLog;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import test_crm.proxies.Company;
import test_crm.proxies.Group;

import java.util.Arrays;

import static test_crm.proxies.Company.MemberNames.*;
import static test_crm.proxies.Company.MemberNames.Company_Group;

/**
 * Helper class for creating tests with a company objects
 */
public abstract class TestAuditWithData extends TestAuditBase {
	protected Company createBaseCompany(final Group... groups) throws CoreException {
		final Company company = new Company(context);

		company.setName(NAME);
		company.setCompanyNr(COMPANY_NR);

		company.setCompany_Group(Arrays.asList(groups));

		company.commit();
		return company;
	}

	protected ExpectedLog createExpectedLog(final TypeOfLog typeOfLog, final Company company,
											final IMendixObject... groupObjects) {
		if (typeOfLog.equals(TypeOfLog.Add)) {
			return new ExpectedLog(typeOfLog, Company.entityName, admin, initialDate, Company.entityName)
					.addAttribute(Name, NAME).addAttribute(CompanyNr, COMPANY_NR)
					.addAttribute(InternNr, company.getInternNr())
					.addAttribute(Dec, 0).addAttribute(Number, 0)
					.addAttribute(Founded, "")
					.addReferences(Company_Group, context, MemberType.ReferenceSet, groupObjects);
		} else {
			return new ExpectedLog(typeOfLog, Company.entityName, admin, initialDate, Company.entityName)
					.keepAttribute(Name, NAME).keepAttribute(CompanyNr, COMPANY_NR)
					.keepAttribute(InternNr, company.getInternNr())
					.keepAttribute(Dec, 0).keepAttribute(Number, 0)
					.keepAttribute(Founded, "")
					.keepReferences(Company_Group, context, MemberType.ReferenceSet, groupObjects);
		}
	}

	protected static final String NAME = "Company";
	protected static final String COMPANY_NR = "123";
}
