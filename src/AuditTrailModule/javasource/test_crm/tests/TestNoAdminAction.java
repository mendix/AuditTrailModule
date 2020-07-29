package test_crm.tests;

import com.mendix.core.CoreException;

import org.junit.Before;
import org.junit.Ignore;

/**
 * This class tests adding, changing and deleting objects of Company by using
 * the TestAuditInheritance class. However, here we remove the admin user who
 * performs the action to make sure we are able to log these actions.
 * 
 * Because of this, the database gets in an inconsistent state and this test is
 * usually commented out.
 */
@Ignore("Unable to continue running the tests if we delete the admin user")
public class TestNoAdminAction extends TestAuditInheritance {
	@Before
	public void beforeTesting() throws CoreException {
		super.beforeTesting();
		this.admin.delete();
		this.admin = null;
	}
}
