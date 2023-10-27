package com.mendix.audittrail.tests;

import java.util.Date;
import java.util.List;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import org.junit.After;
import org.junit.Before;

import system.proxies.Language;
import system.proxies.TimeZone;
import system.proxies.User;
import system.proxies.UserRole;

/**
 * This class tests adding, changing and deleting objects of Company by using
 * the TestAuditInheritance class. However, here we remove the admin user who
 * performs the action to make sure we are able to log these actions. We also
 * remove its sessions, so we get kicked out of the application after the test
 * runs.
 * 
 * After the test runs, another admin user with the same name and password
 * "P@ssw0rd" is created.
 */
public class TestNoAdminAction extends TestAuditInheritance {
	private List<UserRole> adminRoles;
	private Language adminLanguage;
	private TimeZone adminTz;

	@Before
	public void beforeTesting() throws CoreException {
		super.beforeTesting();
		this.adminRoles = this.admin.getUserRoles();
		this.adminLanguage = this.admin.getUser_Language();
		this.adminTz = this.admin.getUser_TimeZone();
		
		List<IMendixObject> sessions = Core.retrieveXPathQuery(this.context,
			String.format("//System.Session[System.Session_User/System.User/ID='%1$d']", this.admin.getMendixObject().getId().toLong()));
		Core.delete(this.context, sessions);
		this.admin.delete();
		this.admin = null;
	}

	@After
	public void afterTesting() throws CoreException {
		// We recreate the admin user here so all tests can still run until the end,
		// even if we are not logged in as that user.
		final IContext systemContext = Core.createSystemContext();
		final User newAdmin = User.load(systemContext, Core.instantiate(systemContext, "Administration.Account").getId());
		newAdmin.setActive(true);
		newAdmin.setBlocked(false);
		newAdmin.setFailedLogins(0);
		newAdmin.setIsAnonymous(false);
		newAdmin.setLastLogin(new Date());
		newAdmin.setName(Core.getConfiguration().getAdminUserName());
		newAdmin.setPassword("P@ssw0rd");
		newAdmin.setUserRoles(this.adminRoles);
		newAdmin.setUser_Language(this.adminLanguage);
		newAdmin.setUser_TimeZone(this.adminTz);
		newAdmin.setWebServiceUser(false);
		newAdmin.commit();
	}
}
