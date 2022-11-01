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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import system.proxies.User;
import audittrail.proxies.AudittrailSuperClass;
import audittrail.proxies.Log;
import audittrail.proxies.LogLine;
import audittrail.proxies.MemberType;
import audittrail.proxies.ReferenceLog;
import audittrail.proxies.ReferenceLogLine;
import audittrail.proxies.TypeOfLog;
import audittrail.proxies.TypeOfReferenceLog;
import audittrail.proxies.constants.Constants;

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
	private static ILogNode logNode = Core.getLogger("AuditTrail");

	private static synchronized String getAssociationName(final String otherObjectType) {
		return associationMapping.get(otherObjectType);
	}

	private static synchronized void setAssociationName(final String otherObjectType, final String associationName) {
		associationMapping.put(otherObjectType, associationName);
	}

	private static void incNumberOfChangedMembers(final IMendixObject logObject, final IContext sudoContext,
			final IContext currentContext, final boolean isNew, final String memberName) {
		if (isNew) logNode.trace("Member: " + memberName + " was added.");
		else logNode.trace("Member: " + memberName + " has changed.");
		
		logObject.setValue(sudoContext, Log.MemberNames.NumberOfChangedMembers.toString(),
				(Integer) logObject.getValue(currentContext, Log.MemberNames.NumberOfChangedMembers.toString()) + 1);
	}

	public static IMendixObject createAuditLogItems(final IMendixObject inputObject, final IContext context) throws CoreException {
		final TypeOfLog log = inputObject.isNew() ? TypeOfLog.Add : TypeOfLog.Change;

		return CreateLogObject.createAuditLogItems(inputObject, context, log);
	}

	public static IMendixObject createAuditLogItems(final IMendixObject auditableObject, final IContext context, final TypeOfLog logType)
			throws CoreException {

		if (auditableObject == null)
			throw new CoreException(
					"The object you are trying to audit is empty, please pass a valid as a parameter into the Java Action");

		if (logNode.isDebugEnabled())
			logNode.debug("Evaluating audit log for object: " + auditableObject.getType() + "("
					+ auditableObject.getId().toLong() + "), state: " + auditableObject.getState() + "/" + logType);

		final IContext sudoContext = Core.createSystemContext();
		final IMendixObject logObject = Core.instantiate(sudoContext, Log.getType());

		IMendixIdentifier userObjectId = null;

		try {
			userObjectId = context.getSession().getUserId();
		} catch (final Exception e) {
			try {
				final List<IMendixObject> administrators = Core.retrieveXPathQuery(sudoContext, "//" + User.getType() + "["
						+ User.MemberNames.Name + "='" + Core.getConfiguration().getAdminUserName() + "']");
				if (administrators.size() > 0) {
					userObjectId = administrators.get(0).getId();
				}
			} catch (final CoreException e1) {
				logNode.error("MxAdmin not found");
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
			// Retrieve the custom created association to AuditableObject, look it up when
			// not found
			association = getAssociationName(auditableObject.getType());

			// Try to look up parent of the association
			if (association == null) {
				final IMetaObject imObject = Core.getMetaObject(auditableObject.getType());
				for (final IMetaAssociation ass : imObject.getMetaAssociationsParent()) {
					if (Core.isSubClassOf(Log.getType(), ass.getChild().getId())
							&& ass.getType() == AssociationType.REFERENCESET) {
						association = ass.getName();

						// Ticket 56528
						final MendixObjectReferenceSet logReferenceSet = (MendixObjectReferenceSet) auditableObject
								.getMember(sudoContext, association);
						logReferenceSet.addValue(sudoContext, logObject.getId());

						setAssociationName(auditableObject.getType(), association);
						break;
					}
				}
				// If not found, try to look up the child of the association
				if (association == null) {
					for (final IMetaAssociation ass : imObject.getMetaAssociationsChild()) {
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
				} catch (final IllegalArgumentException e) {
					logNode.error(
							"Could not find association in audit trail super class: " + auditableObject.getType(), e);
				}

			}
			logNode.trace("Setting the association: " + association);

			if (association == null)
				throw new CoreException("Unable to find a reference set between " + Log.getType() + " and "
						+ auditableObject.getType());

		}

		if ((createLogLines(auditableObject, logObject, sudoContext, context, logType, association) > 0)
				|| Constants.getCreateLogObjectWithoutMemberChanges() || logType == TypeOfLog.Delete) {
			Core.commit(sudoContext, logObject);
			return logObject;
		} else {
			logNode.debug(
					"No log lines created (no attributes changed), and configurition prevents empty log records to be created. Removing this tmp Log object for: "
							+ logObject.getValue(sudoContext, Log.MemberNames.ReferenceId.toString()));
			Core.delete(sudoContext, logObject);
			return null;
		}
	}

	private static int createLogLines(final IMendixObject inputObject, final IMendixObject logObject, final IContext sudoContext,
			final IContext currentContext, final TypeOfLog logType, final String skipAssociation) throws CoreException {
		boolean isNew = false;
		if (logType != TypeOfLog.Delete && inputObject.isNew()) {
			// The object is new
			logObject.setValue(sudoContext, Log.MemberNames.LogType.toString(), TypeOfLog.Add.toString());

			/*
			 * Set the isNew boolean to the same value as the LogAllOnCreate. This will
			 * ensure that when a new record is created it will log the attrs according to
			 * the setting.
			 */
			isNew = Constants.getLogAllMembersOnCreate();
		}
		boolean isDeleting = logType == TypeOfLog.Delete;

		final Collection<? extends IMendixObjectMember<?>> members = inputObject.getMembers(sudoContext).values();
		final List<IMendixObject> logLineList = new ArrayList<IMendixObject>(members.size());

		for (final IMendixObjectMember<?> member : members) {
			if (member.getName().equals(skipAssociation))
				continue;

			if (!Constants.getIncludeCalculatedAttributes() && member.isVirtual())
				continue;

			if (member instanceof MendixObjectReference) {
				if (!member.getName().startsWith("System."))
					logLineList.addAll(createReferenceLogLine(logObject, (MendixObjectReference) member, isNew,
							isDeleting, sudoContext, currentContext));
			}

			else if (member instanceof MendixObjectReferenceSet)
				logLineList.addAll(createReferenceSetLogLine(logObject, (MendixObjectReferenceSet) member, isNew,
						isDeleting, sudoContext, currentContext));

			else {
				final String attributeName = member.getName();

				if (!attributeName.startsWith("System.") && !attributeName.equals("changedDate")
						&& !attributeName.equals("createdDate")) {
					logLineList.addAll(createSingleLogLine(logObject, member, MemberType.Attribute.toString(), isNew,
							isDeleting, sudoContext));
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

	private static List<IMendixObject> createSingleLogLine(final IMendixObject logObject,
			final IMendixObjectMember<?> member, final String memberType, final boolean isNew, final boolean isDeleting,
			final IContext context) throws CoreException {
		final String oldValue = getMemberValueString(member, false, context), newValue = getMemberValueString(member, true, context);
		
		final boolean newOrChangedObject = !oldValue.equals(newValue) || isNew;
		if (!Constants.getIncludeOnlyChangedAttributes() || isDeleting || newOrChangedObject) {
			final IMendixObject logLine = Core.instantiate(context, LogLine.getType());

			logLine.setValue(context, LogLine.MemberNames.Member.toString(), member.getName());
			logLine.setValue(context, LogLine.MemberNames.MemberType.toString(), memberType);
			logLine.setValue(context, LogLine.MemberNames.LogLine_Log.toString(), logObject.getId());
			logLine.setValue(context, LogLine.MemberNames.NewValue.toString(), newValue);

			if (isNew)
				logLine.setValue(context, LogLine.MemberNames.OldValue.toString(), "");
			else
				logLine.setValue(context, LogLine.MemberNames.OldValue.toString(), oldValue);

			if (newOrChangedObject)
				incNumberOfChangedMembers(logObject, context, context, isNew, member.getName());

			return Collections.singletonList(logLine);
		}

		logNode.trace("Skipping member: " + member.getName() + " because it has not changed.");
		return Collections.emptyList();
	}

	private static List<IMendixObject> createReferenceLogLine(final IMendixObject logObject,
			final MendixObjectReference member, final boolean isNew, boolean isDeleting, final IContext sudocontext,
			final IContext currentcontext) throws CoreException {
		// get current and previous id
		final IMendixIdentifier currentId = member.getValue(currentcontext);
		final IMendixIdentifier previousId = member.getOriginalValue(currentcontext);

		final boolean newOrChangedObject = !Objects.equals(currentId, previousId) || isNew;
		if (!Constants.getIncludeOnlyChangedAttributes() || isDeleting || newOrChangedObject) {
			final List<IMendixObject> logLineList = new ArrayList<IMendixObject>();
			final IMendixObject logLine = Core.instantiate(sudocontext, LogLine.getType());

			logLine.setValue(sudocontext, LogLine.MemberNames.Member.toString(), member.getName());
			logLine.setValue(sudocontext, LogLine.MemberNames.MemberType.toString(), MemberType.Reference.toString());
			logLine.setValue(sudocontext, LogLine.MemberNames.LogLine_Log.toString(), logObject.getId());
			logLine.setValue(sudocontext, LogLine.MemberNames.NewValue.toString(), "");
			logLine.setValue(sudocontext, LogLine.MemberNames.OldValue.toString(), "");

			logLineList.add(logLine);

			if (Objects.equals(currentId, previousId)) {
				logLineList.addAll(createLogLinesForReferencedObject(previousId, logLine.getId(), currentcontext,
						TypeOfReferenceLog.No_Change));
			} else {
				if (currentId != null) {
					logLineList.addAll(createLogLinesForReferencedObject(currentId, logLine.getId(), currentcontext,
							TypeOfReferenceLog.Added));
				}
				if (previousId != null) {
					logLineList.addAll(createLogLinesForReferencedObject(previousId, logLine.getId(), currentcontext,
							TypeOfReferenceLog.Deleted));
				}
			}

			if (newOrChangedObject)
				incNumberOfChangedMembers(logObject, sudocontext, sudocontext, isNew, member.getName());

			return logLineList;
		}

		logNode.trace("Skipping member: " + member.getName() + " because it has not changed.");
		return Collections.emptyList();
	}

	private static List<IMendixObject> createLogLinesForReferencedObject(final IMendixIdentifier attributeId,
			final IMendixIdentifier parentId, final IContext currentcontext, final TypeOfReferenceLog typeOfReference)
			throws CoreException {
		if (attributeId == null)
			return Collections.emptyList();

		return Optional.ofNullable(Core.retrieveId(currentcontext, attributeId)) // Get id of object
				.map(refObj -> createReferenceObjects(attributeId, parentId, currentcontext, typeOfReference, refObj))
				.orElseGet(() -> Collections.emptyList());
	}

	private static List<IMendixObject> createReferenceObjects(final IMendixIdentifier attributeId, final IMendixIdentifier parentId,
			final IContext currentcontext, final TypeOfReferenceLog typeOfReference, final IMendixObject refObj) {
		final IMendixObject referenceLog = createReferenceLogObj(attributeId, parentId, typeOfReference);

		final Stream<IMendixObject> referenceLineObjects = refObj.getPrimitives(currentcontext).stream()
				.map(member -> createReferenceLineMendixObj(member, currentcontext, referenceLog.getId()));

		return Stream.concat(Stream.of(referenceLog), referenceLineObjects).collect(Collectors.toList());
	}

	private static IMendixObject createReferenceLogObj(final IMendixIdentifier attributeId, final IMendixIdentifier parentId,
			final TypeOfReferenceLog typeOfReference) {
		final Map<String, Object> nameToValueReferenceLog = new HashMap<String, Object>();
		nameToValueReferenceLog.put(ReferenceLog.MemberNames.AttributeID.toString(),
				String.valueOf(attributeId.toLong()));
		nameToValueReferenceLog.put(ReferenceLog.MemberNames.Operation.toString(), typeOfReference.toString());
		nameToValueReferenceLog.put(ReferenceLog.MemberNames.ReferenceLog_LogLine.toString(), parentId);

		return createMendixObject(ReferenceLog.getType(), nameToValueReferenceLog);
	}

	private static <T extends IMendixObjectMember<T>> IMendixObject createReferenceLineMendixObj(
			final IMendixObjectMember<?> member, final IContext context, final IMendixIdentifier refLogId) {
		final Map<String, Object> nameToValueReferenceLogLine = new HashMap<String, Object>();
		nameToValueReferenceLogLine.put(ReferenceLogLine.MemberNames.Member.toString(), member.getName());
		nameToValueReferenceLogLine.put(ReferenceLogLine.MemberNames.Value.toString(), getMemberValueString(member, true, context));
		nameToValueReferenceLogLine.put(ReferenceLogLine.MemberNames.ReferenceLogLine_ReferenceLog.toString(),
				refLogId);

		return createMendixObject(ReferenceLogLine.getType(), nameToValueReferenceLogLine);
	}

	private static IMendixObject createMendixObject(final String objectType, final Map<String, Object> nameToValue) {
		final IContext systemContext = Core.createSystemContext();
		final IMendixObject mendixObject = Core.instantiate(systemContext, objectType);
		nameToValue.forEach((name, value) -> mendixObject.setValue(systemContext, name, value));

		return mendixObject;
	}

	private static Comparator<IMendixIdentifier> IDCOMPARATOR = (final IMendixIdentifier i1,
			final IMendixIdentifier i2) -> (int) (i1.toLong() - i2.toLong());

	private static List<IMendixObject> createReferenceSetLogLine(final IMendixObject logObject,
			final MendixObjectReferenceSet member, final boolean isNew, boolean isDeleting, final IContext sudocontext,
			final IContext currentcontext) throws CoreException {

		final List<IMendixIdentifier> currentIdList = member.getValue(currentcontext);
		final List<IMendixIdentifier> previousIdList = member.getOriginalValue(currentcontext);

		currentIdList.sort(IDCOMPARATOR);
		previousIdList.sort(IDCOMPARATOR);

		final boolean newOrChangedObjects = !Objects.equals(currentIdList, previousIdList) || isNew;
		if (!Constants.getIncludeOnlyChangedAttributes() || isDeleting || newOrChangedObjects) {

			// The size below is just a good guess
			final List<IMendixObject> logLineList = new ArrayList<IMendixObject>(currentIdList.size() + 1);

			final IMendixObject logLine = Core.instantiate(sudocontext, LogLine.getType());
			logLine.setValue(sudocontext, LogLine.MemberNames.Member.toString(), member.getName());
			logLine.setValue(sudocontext, LogLine.MemberNames.MemberType.toString(),
					MemberType.ReferenceSet.toString());
			logLine.setValue(sudocontext, LogLine.MemberNames.LogLine_Log.toString(), logObject.getId());
			logLine.setValue(sudocontext, LogLine.MemberNames.NewValue.toString(), "");
			logLine.setValue(sudocontext, LogLine.MemberNames.OldValue.toString(), "");

			logLineList.add(logLine);

			final List<IMendixIdentifier> unchangedRefs = currentIdList.stream().filter(previousIdList::contains)
					.collect(Collectors.toList());
			currentIdList.removeAll(unchangedRefs); // References that were added
			previousIdList.removeAll(unchangedRefs); // References that were removed

			for (final IMendixIdentifier unchangedRef : unchangedRefs) {
				logLineList.addAll(createLogLinesForReferencedObject(unchangedRef, logLine.getId(), currentcontext,
						TypeOfReferenceLog.No_Change));
			}

			for (final IMendixIdentifier currentRef : currentIdList) {
				logLineList.addAll(createLogLinesForReferencedObject(currentRef, logLine.getId(), currentcontext,
						TypeOfReferenceLog.Added));
			}

			for (final IMendixIdentifier previousRef : previousIdList) {
				logLineList.addAll(createLogLinesForReferencedObject(previousRef, logLine.getId(), currentcontext,
						TypeOfReferenceLog.Deleted));
			}

			if (newOrChangedObjects) // Do not increase the number of changed members if nothing changed
				incNumberOfChangedMembers(logObject, sudocontext, currentcontext, isNew, member.getName());

			return logLineList;
		}

		logNode.trace("Skipping member: " + member.getName() + " because it has not changed.");
		return Collections.emptyList();
	}

	private static String getMemberValueString(final IMendixObjectMember<?> member, final boolean fromCache, final IContext context) {
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

	private static String parseDate(final Date date, final IContext context) {
		String dateOutput = "";
		if (date != null) {
			final DateFormat dateFormat = new SimpleDateFormat(Constants.getLogLineDateFormat());
			if (Constants.getLogServerTimeZoneDateNotation()) {
				final TimeZone zone = TimeZone.getTimeZone(Constants.getServerTimeZone());
				dateFormat.setTimeZone(zone);
				dateOutput = dateFormat.format(date) + " (UTC) ";
			}

			if (Constants.getLogSessionTimeZoneDateNotation() && context.getSession() != null
					&& context.getSession().getTimeZone() != null) {
				if (!"".equals(dateOutput))
					dateOutput += " / ";

				final TimeZone zone = context.getSession().getTimeZone();
				dateFormat.setTimeZone(zone);
				dateOutput += dateFormat.format(date) + " (" + zone.getDisplayName() + ") ";
			}
		}

		return dateOutput;
	}
}
