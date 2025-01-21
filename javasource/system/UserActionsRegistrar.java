package system;

import com.mendix.core.actionmanagement.IActionRegistrator;

public class UserActionsRegistrar
{
  public void registerActions(IActionRegistrator registrator)
  {
    registrator.registerUserAction(audittrail.actions.CreateLogRecordChangesBeforeDelete.class);
    registrator.registerUserAction(audittrail.actions.CreateLogRecordOfObject.class);
    registrator.registerUserAction(audittrail.actions.GetDiff.class);
    registrator.registerUserAction(system.actions.VerifyPassword.class);
  }
}
