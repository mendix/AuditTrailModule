package test_crm.tests;

import java.util.Date;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import org.junit.Before;

import system.proxies.User;
import test_crm.proxies.Group;

public abstract class TestAuditBase {
	protected IContext context;

	protected User admin;
	protected Group group, group2;
	protected IMendixObject groupObject, group2Object;
	protected Date initialDate;

	@Before
	public void beforeTesting() throws CoreException {
		this.context = Core.createSystemContext();

		this.initialDate = new Date();

		this.admin = User.load(this.context,
				String.format("[%1s = '%2s']", User.MemberNames.Name, Core.getConfiguration().getAdminUserName()))
				.get(0);

		this.group = createNewGroup(GROUP_NAME, GROUP_CODE);
		this.groupObject = this.group.getMendixObject();
		this.group2 = createNewGroup(GROUP_NAME2, GROUP_CODE2);
		this.group2Object = this.group2.getMendixObject();
	}

	private Group createNewGroup(final String name, final String code) throws CoreException {
		final Group newGroup = new Group(context);
		newGroup.setCode(code);
		newGroup.setName(name);
		newGroup.commit();

		return newGroup;
	}

	private static final String GROUP_NAME = "GROUP_NAME";
	private static final String GROUP_CODE = "CDE";
	private static final String GROUP_NAME2 = "GROUP_NAME2";
	private static final String GROUP_CODE2 = "CDF";
}
