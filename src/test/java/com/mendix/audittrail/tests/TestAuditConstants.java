package com.mendix.audittrail.tests;

import audittrail.proxies.TypeOfLog;
import com.mendix.audittrail.tests.actual.ActualLog;
import com.mendix.audittrail.tests.expected.ExpectedLog;
import com.mendix.core.CoreException;
import org.junit.Test;
import test_crm.proxies.Company;

import java.math.BigDecimal;

import static test_crm.proxies.Company.MemberNames.Dec;

public class TestAuditConstants extends TestAuditWithData {
	@Test
	public void testLogUseDecimalScientificNotationIsTrue() throws CoreException {
		updateConstant("AuditTrail.LogUseDecimalScientificNotation", true);
		// Create new record
		final Company company = createBaseCompanyAndSetDecimalValue();

		// Expect scientific decimal format in log
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add, company).addAttribute(Dec, DEC_STRING_SCIENTIFIC);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testLogUseDecimalScientificNotationIsFalse() throws CoreException {
		updateConstant("AuditTrail.LogUseDecimalScientificNotation", false);
		// Create new record
		final Company company = createBaseCompanyAndSetDecimalValue();

		// Expect full decimal format in log
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add, company).addAttribute(Dec, DEC_STRING_FULL);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	@Test
	public void testLogUseDecimalScientificNotationIsNotSet() throws CoreException {
		// Create new record
		final Company company = createBaseCompanyAndSetDecimalValue();

		// Expect scientific decimal format in log
		final ExpectedLog expectedLog = createExpectedLog(TypeOfLog.Add, company).addAttribute(Dec, DEC_STRING_SCIENTIFIC);

		final ActualLog actualLog = ActualLog.getLastLog(context, company.getMendixObject().getId().toLong());
		expectedLog.verify(actualLog);
	}

	 protected Company createBaseCompanyAndSetDecimalValue() throws CoreException {
		final Company company = new Company(context);
		
		company.setName(NAME);
		company.setCompanyNr(COMPANY_NR);
		company.setDec(DEC);

		company.commit();
		return company;
	}

	private static final BigDecimal DEC = BigDecimal.valueOf(1000);
	private static final String DEC_STRING_SCIENTIFIC = "1E+3";
	private static final String DEC_STRING_FULL = "1000";
	 
}
