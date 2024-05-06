package com.mendix.audittrail.tests;

import org.junit.Assert;
import org.junit.Test;
import test_crm.proxies.Company;

import java.util.Calendar;
import java.util.TimeZone;

public class TestTimezone extends TestAuditBase {
	
	private static Long createDate(Boolean isDST) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 1991);
		if (isDST) calendar.set(Calendar.MONTH, 6);
		else calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DATE, 4);
		calendar.set(Calendar.HOUR, 4);
		calendar.set(Calendar.MINUTE, 45);
		calendar.setTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"));
		return calendar.getTimeInMillis();
	}
	
	@Test
	public void testTimeZoneNotChanged() throws Exception {
		// Set initial timezone
		updateConstant("AuditTrail.ServerTimeZone", "Europe/Amsterdam");
		context.createSudoClone().getSession().setTimeZone("Europe/Amsterdam");

		final Company company = new Company(context);

		TimeZone timezoneBeforeCommit = context.getSession().getTimeZone();
		
		// Cause CreateLogObject.createAuditLogItems to be called
		company.commit();

		TimeZone timezoneAfterCommit = context.getSession().getTimeZone();
		
		Assert.assertEquals(timezoneBeforeCommit.getOffset(createDate(false)), timezoneAfterCommit.getOffset(createDate(false)));
		Assert.assertEquals(timezoneBeforeCommit.getOffset(createDate(true)), timezoneAfterCommit.getOffset(createDate(true)));
	}
}
