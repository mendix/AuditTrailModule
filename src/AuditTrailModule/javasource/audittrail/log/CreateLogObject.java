package audittrail.log;

import java.lang.IllegalArgumentException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

import system.proxies.User;
import audittrail.proxies.AudittrailSuperClass;
import audittrail.proxies.Configuration;
import audittrail.proxies.Log;
import audittrail.proxies.LogLine;
import audittrail.proxies.MemberType;
import audittrail.proxies.ReferenceLog;
import audittrail.proxies.ReferenceLogLine;
import audittrail.proxies.TypeOfLog;
import audittrail.proxies.TypeOfReferenceLog;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.core.objectmanagement.member.MendixObjectReference;
import com.mendix.core.objectmanagement.member.MendixObjectReferenceSet;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixIdentifier;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IMendixObjectMember;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation.AssociationOwner;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation.AssociationType;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;

public class CreateLogObject {
	private static HashMap<String, String> associationMapping = new LinkedHashMap<String, String>();
	private static boolean isInitialized = false;

	private static Boolean CreateLogObjectWithoutMemberChanges = null;
	private static Boolean IncludeCalculatedAttributes = null;
	private static Boolean LogAllMembersOnCreate = null;
	private static Boolean IncludeOnlyChangedAttributes = null;
	private static Boolean LogServerTimeZoneDateNotation = null;
	private static Boolean LogSessionTimeZoneDateNotation = null;
	private static String ServerTimeZone = null;
	private static String LogLineDateFormat = null;
	private static ILogNode _logNode = Core.getLogger("AuditTrail");

	private static synchronized void _initialize() {
		if (!isInitialized) {
			IContext context = Core.createSystemContext();
			IMendixObject config = Core.instantiate(context, Configuration.getType());

			CreateLogObject.CreateLogObjectWithoutMemberChanges = config.getValue(context,
					Configuration.MemberNames.CreateLogObjectWithoutMemberChanges.toString());
			CreateLogObject.IncludeCalculatedAttributes = config.getValue(context,
					Configuration.MemberNames.IncludeCalculatedAttributes.toString());
			CreateLogObject.LogAllMembersOnCreate = config.getValue(context,
					Configuration.MemberNames.LogAllMembersOnCreate.toString());
			CreateLogObject.IncludeOnlyChangedAttributes = config.getValue(context,
					Configuration.MemberNames.IncludeOnlyChangedAttributes.toString());
			CreateLogObject.LogLineDateFormat = config.getValue(context,
					Configuration.MemberNames.LogLineDateFormat.toString());
			CreateLogObject.LogServerTimeZoneDateNotation = config.getValue(context,
					Configuration.MemberNames.LogServerTimeZoneDateNotation.toString());
			CreateLogObject.LogSessionTimeZoneDateNotation = config.getValue(context,
					Configuration.MemberNames.LogSessionTimeZoneDateNotation.toString());
			CreateLogObject.ServerTimeZone = config.getValue(context,
					Configuration.MemberNames.ServerTimeZone.toString());

			try {
				Core.rollback(context, config);
			} catch (CoreException e) {
			}

			isInitialized = true;
		}
	}

	private static synchronized String getAssociationName(String otherObjectType) {
		return associationMapping.get(otherObjectType);
	}

	private static synchronized void setAssociationName(String otherObjectType, String associationName) {
		associationMapping.put(otherObjectType, associationName);
	}

	public static IMendixObject CreateAuditLogItems(IMendixObject inputObject, IContext context) throws CoreException {
		TypeOfLog log = inputObject.isNew() ? TypeOfLog.Add : TypeOfLog.Change;

		return CreateLogObject.CreateAuditLogItems(inputObject, context, log);
	}

	public static IMendixObject CreateAuditLogItems(IMendixObject auditableObject, IContext context, TypeOfLog logType)
			throws CoreException {
		_initialize();

		if (auditableObject == null)
			throw new CoreException(
					"The object you are trying to audit is empty, please pass a valid as a parameter into the Java Action");

		if (_logNode.isDebugEnabled())
			_logNode.debug("Evaluating audit log for object: " + auditableObject.getType() + "("
					+ auditableObject.getId().toLong() + "), state: " + auditableObject.getState() + "/" + logType);

		IContext sudoContext = Core.createSystemContext();
		IMendixObject logObject = Core.instantiate(sudoContext, Log.getType());
		;
		IMendixIdentifier userObjectId = null;

		try {
			userObjectId = context.getSession().getUserId();
		} catch (Exception e) {
			try {
				List<IMendixObject> administrators = Core.retrieveXPathQuery(sudoContext, "//" + User.getType() + "["
						+ User.MemberNames.Name + "='" + Core.getConfiguration().getAdminUserName() + "']");
				userObjectId = administrators.get(0).getId();
			} catch (CoreException e1) {
				_logNode.error("MxAdmin not found");
			}
		}

		logObject.setValue(sudoContext, Log.MemberNames.DateTime.toString(), new Date());
		logObject.setValue(sudoContext, Log.MemberNames.LogObject.toString(), auditableObject.getType());
		logObject.setValue(sudoContext, Log.MemberNames.Log_User.toString(), userObjectId);
		logObject.setValue(sudoContext, Log.MemberNames.LogType.toString(), logType.toString());
		logObject.setValue(sudoContext, Log.MemberNames.ReferenceId.toString(),
				String.valueOf(auditableObject.getId().toLong()));
		String association = null;

		// Set the association for the AuditableObject inheriting from superclass
		if (Core.isSubClassOf(AudittrailSuperClass.getType(), auditableObject.getType())) {
			logObject.setValue(sudoContext, Log.MemberNames.Log_AudittrailSuperClass.toString(),
					auditableObject.getId());
		} else {
			// Retrieve the custom created association to AuditbleObject, look it up when
			// not found
			association = getAssociationName(auditableObject.getType());

			// Try to look up parent of the association
			if (association == null) {
				IMetaObject imObject = Core.getMetaObject(auditableObject.getType());
				for (IMetaAssociation ass : imObject.getMetaAssociationsParent()) {
					if (Core.isSubClassOf(Log.getType(), ass.getChild().getId())
							&& ass.getType() == AssociationType.REFERENCESET) {
						association = ass.getName();

						// Ticket 56528
						MendixObjectReferenceSet logReferenceSet = (MendixObjectReferenceSet) auditableObject
								.getMember(sudoContext, association);
						logReferenceSet.addValue(sudoContext, logObject.getId());

						setAssociationName(auditableObject.getType(), association);
						break;
					}
				}
				// If not found, try to look up the child of the association
				if (association == null) {
					for (IMetaAssociation ass : imObject.getMetaAssociationsChild()) {
						if (Core.isSubClassOf(Log.getType(), ass.getParent().getId())
								&& (ass.getType() == AssociationType.REFERENCESET
										|| ass.getOwner() != AssociationOwner.BOTH)) {
							association = ass.getName();
							logObject.setValue(sudoContext, association, auditableObject.getId());

							setAssociationName(auditableObject.getType(), association);
							break;
						}
					}
				}
			}

			// When we already have used the association we only need to check on which
			// entity we need to set the association
			else {
				try {
					if (auditableObject.hasMember(association)) {
						((MendixObjectReferenceSet) auditableObject.getMember(context, association))
								.addValue(sudoContext, logObject.getId());
					} else if (logObject.hasMember(association)) {
						logObject.setValue(sudoContext, association, auditableObject.getId());
					} else
						throw new CoreException("Unable to find a reference set between " + Log.getType() + " and "
								+ auditableObject.getType() + " (cached attempt)");
				} catch (IllegalArgumentException e) {
					_logNode.error(
							"Could not find association in audit trail super class: " + auditableObject.getType(), e);
				}

			}
			_logNode.trace("Setting the association: " + association);

			if (association == null)
				throw new CoreException("Unable to find a reference set between " + Log.getType() + " and "
						+ auditableObject.getType());

		}

		if ((createLogLines(auditableObject, logObject, sudoContext, context, logType, association) > 0)
				|| CreateLogObjectWithoutMemberChanges || logType == TypeOfLog.Delete) {
			Core.commit(sudoContext, logObject);
			return logObject;
		} else {
			_logNode.debug(
					"No log lines created (no attributes changed), and configurition prevents empty log records to be created. Removing this tmp Log object for: "
							+ logObject.getValue(sudoContext, Log.MemberNames.ReferenceId.toString()));
			Core.delete(sudoContext, logObject);
			return null;
		}
	}

	private static int createLogLines(IMendixObject inputObject, IMendixObject logObject, IContext sudoContext,
			IContext currentContext, TypeOfLog logType, String skipAssociation) throws CoreException {
		boolean isNew = false;
		if (logType != TypeOfLog.Delete && inputObject.isNew()) {
			// The object is new
			logObject.setValue(sudoContext, Log.MemberNames.LogType.toString(), TypeOfLog.Add.toString());

			/*
				* Set the isNew boolean to the same value as the LogAllOnCreate. This will
				* ensure that when a new record is created it will log the attrs according to
				* the setting.
				*/
			isNew = LogAllMembersOnCreate;
		}

		Collection<? extends IMendixObjectMember<?>> members = inputObject.getMembers(sudoContext).values();
		List<IMendixObject> logLineList = new ArrayList<IMendixObject>(members.size());

		for (IMendixObjectMember<?> member : members) {
			if (member.getName().equals(skipAssociation))
				continue;

			if (!IncludeCalculatedAttributes && member.isVirtual())
				continue;

			if (member instanceof MendixObjectReference) {
				if (!member.getName().startsWith("System."))
					logLineList.addAll(createReferenceLogLine(logObject, member, isNew, sudoContext, currentContext));
			}

			else if (member instanceof MendixObjectReferenceSet)
				logLineList.addAll(createReferenceSetLogLine(logObject, member, isNew, sudoContext, currentContext));

			else {
				String attributeName = member.getName();

				if (!attributeName.startsWith("System.") && !attributeName.equals("changedDate")
						&& !attributeName.equals("createdDate")) {
					logLineList.addAll(createSingleLogLine(logObject, member, MemberType.Attribute.toString(), isNew, sudoContext));
				}
			}
		}

		if (logLineList.size() > 0) {
			Core.commit(sudoContext, logObject);
			Core.commit(sudoContext, logLineList);

			return logLineList.size();
		}

		return 0;
	}

	private static List<IMendixObject> createSingleLogLine(IMendixObject logObject, IMendixObjectMember<?> member,
			String memberType, boolean isNew, IContext context) throws CoreException {
		String oldValue = getValue(member, false, context), newValue = getValue(member, true, context);
		if (IncludeOnlyChangedAttributes == false || !oldValue.equals(newValue) || isNew) {
			IMendixObject logLine = Core.instantiate(context, LogLine.getType());

			logLine.setValue(context, LogLine.MemberNames.Member.toString(), member.getName());
			logLine.setValue(context, LogLine.MemberNames.MemberType.toString(), memberType);
			logLine.setValue(context, LogLine.MemberNames.LogLine_Log.toString(), logObject.getId());
			logLine.setValue(context, LogLine.MemberNames.NewValue.toString(), newValue);

			if (isNew)
				logLine.setValue(context, LogLine.MemberNames.OldValue.toString(), "");
			else
				logLine.setValue(context, LogLine.MemberNames.OldValue.toString(), oldValue);

			if (!oldValue.equals(newValue) || isNew) {
				_logNode.trace("Member: " + member.getName() + " has changed.");
				logObject.setValue(context, Log.MemberNames.NumberOfChangedMembers.toString(),
						(Integer) logObject.getValue(context, Log.MemberNames.NumberOfChangedMembers.toString()) + 1);
			}

			return Collections.singletonList(logLine);
		}

		_logNode.trace("Skipping member: " + member.getName() + " because it has not changed.");
		return Collections.emptyList();
	}

	private static List<IMendixObject> createReferenceLogLine(IMendixObject logObject, IMendixObjectMember<?> member,
			boolean isNew, IContext sudocontext, IContext currentcontext) throws CoreException {
		// get current and previous id
		IMendixIdentifier cID = (IMendixIdentifier) member.getValue(currentcontext);
		IMendixIdentifier pID = (IMendixIdentifier) member.getOriginalValue(currentcontext);

		if (IncludeOnlyChangedAttributes == false || !Objects.equals(cID, pID) || isNew) {
			List<IMendixObject> logLineList = new ArrayList<IMendixObject>();
			IMendixObject logLine = Core.instantiate(sudocontext, LogLine.getType());

			logLine.setValue(sudocontext, LogLine.MemberNames.Member.toString(), member.getName());
			logLine.setValue(sudocontext, LogLine.MemberNames.MemberType.toString(), MemberType.Reference.toString());
			logLine.setValue(sudocontext, LogLine.MemberNames.LogLine_Log.toString(), logObject.getId());
			logLine.setValue(sudocontext, LogLine.MemberNames.NewValue.toString(), "");
			logLine.setValue(sudocontext, LogLine.MemberNames.OldValue.toString(), "");

			logLineList.add(logLine);
			
			if (cID == pID && cID != null) {
				logLineList.addAll(createLogLinesForReferencedObject(pID, logLine.getId(), currentcontext, sudocontext, TypeOfReferenceLog.No_Change));
			} else {
				if (cID != null) {
					logLineList.addAll(createLogLinesForReferencedObject(cID, logLine.getId(), currentcontext, sudocontext, TypeOfReferenceLog.Deleted));
				}
				if (pID != null) {
					logLineList.addAll(createLogLinesForReferencedObject(pID, logLine.getId(), currentcontext, sudocontext, TypeOfReferenceLog.Added));
				}
			}

			if (logLineList.size() > 1 || isNew) {
				_logNode.trace("Member: " + member.getName() + " has changed.");
				logObject.setValue(sudocontext, Log.MemberNames.NumberOfChangedMembers.toString(),
						(Integer) logObject.getValue(sudocontext, Log.MemberNames.NumberOfChangedMembers.toString())
								+ 1);
			}

			return logLineList;
		}
		_logNode.trace("Skipping member: " + member.getName() + " because it has not changed.");
		return null;
	}

	private static List<IMendixObject> createLogLinesForReferencedObject(IMendixIdentifier ID, IMendixIdentifier parentID,
			IContext currentcontext, IContext sudocontext, TypeOfReferenceLog typeOfReference) throws CoreException {
		if (ID == null) return Collections.emptyList();

		// Get id of object
		IMendixObject refObj = Core.retrieveId(currentcontext, ID);
		List<IMendixObject> logLineList = new ArrayList<IMendixObject>();

		if (refObj != null) {
			// Get list of members with descriptive values
			Collection<? extends IMendixObjectMember<?>> list = refObj.getMembers(currentcontext).values();

			IMendixObject referenceLog = Core.instantiate(sudocontext, ReferenceLog.getType());
			referenceLog.setValue(sudocontext, ReferenceLog.MemberNames.AttributeID.toString(), ID.toString());
			referenceLog.setValue(sudocontext, ReferenceLog.MemberNames.Operation.toString(), typeOfReference.toString());
			
			logLineList.add(referenceLog);

			if (list.size() > 0) {
				// loop the list
				for (IMendixObjectMember<?> member : list) {
					IMendixObject referenceLogLine = Core.instantiate(sudocontext, ReferenceLogLine.getType());
					referenceLogLine.setValue(sudocontext, ReferenceLogLine.MemberNames.Member.toString(), member.getName());
					referenceLogLine.setValue(sudocontext, ReferenceLogLine.MemberNames.Value.toString(), getValue(member, true, currentcontext));
					logLineList.add(referenceLogLine);
				}
			}
		}

		return logLineList;
	}
	
	private static Comparator<IMendixIdentifier> IDCOMPARATOR = (IMendixIdentifier i1, IMendixIdentifier i2) -> (int)(i1.toLong() - i2.toLong());

	// TODO The lists may be null. Check that.
	@SuppressWarnings("unchecked")
	private static List<IMendixObject> createReferenceSetLogLine(IMendixObject logObject, IMendixObjectMember<?> member,
			boolean isNew, IContext sudocontext, IContext currentcontext) throws CoreException {

		List<IMendixIdentifier> currentIDList = (List<IMendixIdentifier>) member.getValue(currentcontext);
		List<IMendixIdentifier> previousIDList = (List<IMendixIdentifier>) member.getOriginalValue(currentcontext);
		
		currentIDList.sort(IDCOMPARATOR);
		previousIDList.sort(IDCOMPARATOR);

		if (IncludeOnlyChangedAttributes == false || !currentIDList.equals(previousIDList) || isNew) {

			// The size below is just a good guess
			List<IMendixObject> logLineList = new ArrayList<IMendixObject>(currentIDList.size() + 1);
			
			IMendixObject logLine = Core.instantiate(sudocontext, LogLine.getType());
			logLine.setValue(sudocontext, LogLine.MemberNames.Member.toString(), member.getName());
			logLine.setValue(sudocontext, LogLine.MemberNames.MemberType.toString(),
					MemberType.ReferenceSet.toString());
			logLine.setValue(sudocontext, LogLine.MemberNames.LogLine_Log.toString(), logObject.getId());
			logLine.setValue(sudocontext, LogLine.MemberNames.NewValue.toString(), "");
			logLine.setValue(sudocontext, LogLine.MemberNames.OldValue.toString(), "");

			logLineList.add(logLine);

			List<IMendixIdentifier> unchangedRefs = currentIDList.stream().filter(previousIDList::contains).collect(Collectors.toList());
			currentIDList.removeAll(unchangedRefs); // References that were added
			previousIDList.removeAll(unchangedRefs); // References that were removed
			
			for (IMendixIdentifier unchangedRef : unchangedRefs) {
				logLineList.addAll(createLogLinesForReferencedObject(unchangedRef, logLine.getId(), currentcontext, sudocontext, TypeOfReferenceLog.No_Change));
			}

			for (IMendixIdentifier currentRef : currentIDList) {
				logLineList.addAll(createLogLinesForReferencedObject(currentRef, logLine.getId(), currentcontext, sudocontext, TypeOfReferenceLog.Added));
			}

			for (IMendixIdentifier previousRef : previousIDList) {
				logLineList.addAll(createLogLinesForReferencedObject(previousRef, logLine.getId(), currentcontext, sudocontext, TypeOfReferenceLog.Deleted));
			}
			
			if (logLineList.size() > 1 || isNew) {
				_logNode.trace("Member: " + member.getName() + " has changed.");
				logObject.setValue(sudocontext, Log.MemberNames.NumberOfChangedMembers.toString(),
						(Integer) logObject.getValue(currentcontext, Log.MemberNames.NumberOfChangedMembers.toString())
								+ 1);
			}

			return logLineList;
		}

		_logNode.trace("Skipping member: " + member.getName() + " because it has not changed.");
		return null;
	}

	private static String getValue(IMendixObjectMember<?> member, boolean fromCache, IContext context) {
		Object value = null;
		// Values from cache
		if (fromCache == true)
			value = member.getValue(context);

		// Values form DB
		else
			value = member.getOriginalValue(context);

		if (value != null) {

			if (value instanceof Date)
				return parseDate((Date) value, context);

			else if (value instanceof String)
				return (String) value;

			return String.valueOf(value).trim();
		} else
			return "";
	}

	private static String parseDate(Date date, IContext context) {
		String dateOutput = "";
		if (date != null) {
			DateFormat dateFormat = new SimpleDateFormat(CreateLogObject.LogLineDateFormat);
			if (CreateLogObject.LogServerTimeZoneDateNotation) {
				TimeZone zone = TimeZone.getTimeZone(CreateLogObject.ServerTimeZone);
				dateFormat.setTimeZone(zone);
				dateOutput = dateFormat.format(date) + " (UTC) ";
			}

			if (CreateLogObject.LogSessionTimeZoneDateNotation && context.getSession() != null
					&& context.getSession().getTimeZone() != null) {
				if (!"".equals(dateOutput))
					dateOutput += " / ";

				TimeZone zone = context.getSession().getTimeZone();
				dateFormat.setTimeZone(zone);
				dateOutput += dateFormat.format(date) + " (" + zone.getDisplayName() + ") ";
			}
		}

		return dateOutput;
	}
}
