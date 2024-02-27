package com.mendix.audittrail.tests;

import static test_crm.proxies.Company.MemberNames.CompanyNr;
import static test_crm.proxies.Company.MemberNames.Company_Group;
import static test_crm.proxies.Company.MemberNames.Dec;
import static test_crm.proxies.Company.MemberNames.Founded;
import static test_crm.proxies.Company.MemberNames.InternNr;
import static test_crm.proxies.Company.MemberNames.Name;
import static test_crm.proxies.Company.MemberNames.Number;

import com.mendix.audittrail.tests.actual.ActualLog;
import com.mendix.audittrail.tests.expected.ExpectedLog;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import audittrail.proxies.MemberType;
import audittrail.proxies.TypeOfLog;
import org.junit.Test;
import test_crm.proxies.Company;
import test_crm.proxies.Group;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.Mockito.*;

public class TestAuditConstants extends TestAuditBase {
	@Test
	public void testLogUseDecimalScientificNotationIsTrue() throws CoreException {
		context = constructMockContext(context, true);
		// Create new record
		final Company company = createBaseCompany();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add, company).addAttribute(Dec, "1E+3");

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testLogUseDecimalScientificNotationIsFalse() throws CoreException {
		context = constructMockContext(context, false);
		// Create new record
		final Company company = createBaseCompany();

		// Assert log was created
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add, company).addAttribute(Dec, "1000");

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}
	
	private IContext constructMockContext(IContext context, Boolean constantValue) {
		IContext spyContext = spy(context);
		com.mendix.core.conf.Configuration configSpy = spy(spyContext.getSystemConfiguration());
		when(spyContext.getSystemConfiguration()).thenReturn(configSpy);
		when(spyContext.clone()).thenReturn(spyContext);
		when(configSpy.getConstantValue("AuditTrail.LogUseDecimalScientificNotation")).thenReturn(constantValue);
		return spyContext;
	}

	private Company createBaseCompany(final Group... groups) throws CoreException {
		final Company company = new Company(context);
		
		company.setName(NAME);
		company.setCompanyNr(COMPANY_NR);
		company.setDec(BigDecimal.valueOf(1000));

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
					.addAttribute(Dec, "1E+3").addAttribute(Number, 0)
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
	private static final String NAME = "Company";
	private static final String COMPANY_NR = "123";
	
	 
}
