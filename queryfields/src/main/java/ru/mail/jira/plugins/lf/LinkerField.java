/*
 * Created by Andrey Markelov 29-08-2012. Copyright Mail.Ru Group 2012. All
 * rights reserved.
 */
package ru.mail.jira.plugins.lf;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.mail.jira.plugins.lf.struct.AutocompleteUniversalData;
import ru.mail.jira.plugins.lf.struct.ISQLDataBean;
import ru.mail.jira.plugins.lf.struct.IssueData;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.impl.TextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.ApplicationProperties;


/**
 * Linker field.
 * 
 * @author Andrey Markelov
 */
public class LinkerField extends TextCFType implements
        SortableCustomField<String>
{
    /**
     * PlugIn data manager.
     */
    private final QueryFieldsMgr qfMgr;

    /**
     * Search service.
     */
    private final SearchService searchService;

    /**
     * Issue manager.
     */
    private final IssueManager issueMgr;

    /**
     * Application properties.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * Constructor.
     */
    public LinkerField(CustomFieldValuePersister customFieldValuePersister,
        GenericConfigManager genericConfigManager, QueryFieldsMgr qfMgr,
        SearchService searchService, IssueManager issueMgr,
        ApplicationProperties applicationProperties)
    {
        super(customFieldValuePersister, genericConfigManager);
        this.qfMgr = qfMgr;
        this.searchService = searchService;
        this.issueMgr = issueMgr;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Map<String, Object> getVelocityParameters(Issue issue,
        CustomField field, FieldLayoutItem fieldLayoutItem)
    {
        Map<String, Object> params = super.getVelocityParameters(issue, field,
            fieldLayoutItem);
        params.put("i18n", getI18nBean());
        params.put("baseUrl", applicationProperties.getBaseUrl());

        Utils.addViewAndEditParameters(params, field.getId());

        Map<String, String> cfVals = new LinkedHashMap<String, String>();

        Long prId;
        if (field.isAllProjects())
        {
            prId = Consts.PROJECT_ID_FOR_GLOBAL_CF;
        }
        else
        {
            if (issue == null)
            {
                return params;
            }
            prId = issue.getProjectObject().getId();
        }

        String jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), prId);
        boolean addNull = qfMgr.getAddNull(field.getIdAsLong(), prId);
        boolean isAutocompleteView = qfMgr.isAutocompleteView(
            field.getIdAsLong(), prId);
        List<String> options = qfMgr.getLinkeFieldsOptions(field.getIdAsLong(),
            prId);

        params.put("isAutocompleteView", isAutocompleteView);
        params.put("prId", prId.toString());

        String cfValue = field.getValueFromIssue(issue);
        if (Utils.isValidStr(cfValue))
        {
            MutableIssue mi = issueMgr.getIssueObject(cfValue);
            if (mi != null && Utils.isValidStr(mi.getSummary()))
            {
                StringBuilder sb = new StringBuilder();
                if (options.contains("status"))
                {
                    sb.append(getI18nBean().getText("queryfields.opt.status"))
                        .append(": ").append(mi.getStatusObject().getName());
                }
                if (options.contains("assignee")
                    && mi.getAssigneeUser() != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    User aUser = mi.getAssigneeUser();
                    String encodedUser;
                    try
                    {
                        encodedUser = URLEncoder.encode(aUser.getName(),
                            "UTF-8");
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        // --> impossible
                        encodedUser = aUser.getName();
                    }

                    sb.append(getI18nBean().getText("queryfields.opt.assignee"))
                        .append(": ").append("<a class='user-hover' rel='")
                        .append(aUser.getName())
                        .append("' id='issue_summary_assignee_'")
                        .append(aUser.getName())
                        .append("' href='/secure/ViewProfile.jspa?name='")
                        .append(encodedUser).append("'>")
                        .append(aUser.getDisplayName()).append("</a>");
                }
                if (options.contains("priority")
                    && mi.getPriorityObject() != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    sb.append(getI18nBean().getText("queryfields.opt.priority"))
                        .append(": ").append(mi.getPriorityObject().getName());
                }
                if (options.contains("due") && mi.getDueDate() != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    sb.append(getI18nBean().getText("queryfields.opt.due"))
                        .append(": ")
                        .append(
                            ComponentAccessor.getJiraAuthenticationContext()
                                .getOutlookDate().format(mi.getDueDate()));
                }

                if (sb.length() > 0)
                {
                    sb.insert(0, " (");
                    sb.append(")");
                }

                IssueData issueData;
                if (options.contains("justDesc"))
                {
                    String descr = mi.getDescription();
                    if (Utils.isValidStr(descr))
                    {
                        issueData = new IssueData(descr, sb.toString());
                    }
                    else
                    {
                        issueData = new IssueData(mi.getSummary(),
                            sb.toString());
                    }
                }
                else if (options.contains("key"))
                {
                    issueData = new IssueData(mi.getKey().concat(":")
                        .concat(mi.getSummary()), sb.toString());
                }
                else
                {
                    issueData = new IssueData(mi.getSummary(), sb.toString());
                }
                params.put("fullValue", issueData);
            }
        }

        boolean queryFlag = qfMgr.getQueryFlag(field.getIdAsLong());
        if (Consts.LANG_TYPE_JQL.equals(Utils.getKeyByQueryFlag(queryFlag)))
        {
            if (!Utils.isValidStr(jqlData))
            {
                params.put("jqlNotSet", Boolean.TRUE);
                return params;
            }
        }

        params.put("jqlNotSet", Boolean.FALSE);
        params.put("options", options);

        if (options.contains("editKey"))
        {
            params.put("hasKey", Boolean.TRUE);
        }

        List<Issue> issues = null;
        if (Consts.LANG_TYPE_JQL.equals(Utils.getKeyByQueryFlag(queryFlag)))
        {
            issues = Utils.executeJQLQuery(jqlData);
            if (issues == null)
            {
                params.put("jqlNotValid", Boolean.TRUE);
                return params;
            }
            else
            {
                for (Issue i : issues)
                {
                    String summary;
                    if (options.contains("justDesc"))
                    {
                        String descr = i.getDescription();
                        if (Utils.isValidStr(descr))
                        {
                            summary = descr;
                        }
                        else
                        {
                            summary = i.getSummary();
                        }
                    }
                    else if (options.contains("editKey"))
                    {
                        summary = i.getKey().concat(":").concat(i.getSummary());
                    }
                    else
                    {
                        summary = i.getSummary();
                    }
                    cfVals.put(i.getKey(), summary);
                }
            }
        }
        else
        {
            params.put("jqlNotValid", Boolean.FALSE);
            params.put("jqlNotSet", Boolean.FALSE);
            params.put("isError", Boolean.FALSE);

            long projectId;
            if (field.isAllProjects())
            {
                projectId = Consts.PROJECT_ID_FOR_GLOBAL_CF;
            }
            else
            {
                if (issue == null)
                {
                    JiraAuthenticationContext authCtx = ComponentManager
                        .getInstance().getJiraAuthenticationContext();
                    UserProjectHistoryManager userProjectHistoryManager = ComponentManager
                        .getComponentInstanceOfType(UserProjectHistoryManager.class);
                    Project currentProject = userProjectHistoryManager
                        .getCurrentProject(Permissions.BROWSE,
                            authCtx.getLoggedInUser());
                    if (currentProject != null)
                    {
                        projectId = currentProject.getId();
                    }
                    else
                    {
                        params.put("cfVals", cfVals);
                        return params;
                    }
                }
                else
                {
                    projectId = issue.getProjectObject().getId();
                }
            }

            String preparedQuery = qfMgr.getQueryFieldSQLData(
                field.getIdAsLong(), projectId);
            if (Utils.isValidStr(preparedQuery))
            {
                String issueKey = (issue != null && issue.getKey() != null) ? issue
                    .getKey() : Consts.EMPTY_VALUE;
                preparedQuery = preparedQuery.replaceAll(Consts.SQL_RLINK,
                    issueKey);
                preparedQuery = preparedQuery.replaceAll(Consts.SQL_PATTERN,
                    Consts.EMPTY_VALUE);
                preparedQuery = preparedQuery.replaceAll(Consts.SQL_ROWNUM,
                    Consts.SQL_MAX_LIMIT);

                List<ISQLDataBean> values = Utils.executeSQLQuery(
                    preparedQuery, AutocompleteUniversalData.class);
                for (ISQLDataBean dataPortion : values)
                {
                    // TODO revise options mech
                    cfVals.put(dataPortion.getName(),
                        dataPortion.getDescription());
                }
            }
            params.put("cfVals", cfVals);
        }

        if (addNull)
        {
            cfVals.put("Empty", Consts.EMPTY_VALUE);
        }

        String selected = Consts.EMPTY_VALUE;
        String value = (String) issue.getCustomFieldValue(field);
        for (Map.Entry<String, String> cf : cfVals.entrySet())
        {
            if (value != null && cf.getKey().equals(value))
            {
                selected = value;
                break;
            }
        }

        if (isAutocompleteView)
        {
            Issue selectedIssue = issueMgr.getIssueObject(selected);
            if (selectedIssue != null)
            {
                params.put("selIssue", selectedIssue);
            }
        }
        else
        {
            if (selected.equals(""))
            {
                String defaultValue = (String) field.getDefaultValue(issue);
                if (defaultValue != null && defaultValue.length() > 0
                    && cfVals.keySet().contains(defaultValue))
                {
                    selected = defaultValue;
                }
            }

            if (cfVals != null && !cfVals.isEmpty() && selected.equals(""))
            {
                selected = cfVals.keySet().iterator().next();
            }
        }

        params.put("selected", selected);
        params.put("isError", Boolean.FALSE);
        params.put("cfVals", cfVals);

        return params;
    }

    @Override
    public void validateFromParams(CustomFieldParams cfParams,
        ErrorCollection errorCollection, FieldConfig fieldConfig)
    {
        @SuppressWarnings("unchecked")
        final Collection<String> params = cfParams.getAllValues();
        CustomField cf = fieldConfig.getCustomField();
        JiraAuthenticationContext authCtx = ComponentManager.getInstance()
            .getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        UserProjectHistoryManager userProjectHistoryManager = ComponentManager
            .getComponentInstanceOfType(UserProjectHistoryManager.class);
        Project currentProject = userProjectHistoryManager.getCurrentProject(
            Permissions.BROWSE, authCtx.getLoggedInUser());

        boolean isAutocompleteView;
        if (cf.isAllProjects())
        {
            isAutocompleteView = qfMgr.isAutocompleteView(cf.getIdAsLong(),
                Consts.PROJECT_ID_FOR_GLOBAL_CF);
        }
        else
        {
            isAutocompleteView = qfMgr.isAutocompleteView(cf.getIdAsLong(),
                currentProject.getId());
        }

        if (isAutocompleteView)
        {
            if ((params == null) || params.isEmpty())
            {
                boolean addNull;
                if (cf.isAllProjects())
                {
                    addNull = qfMgr.getAddNull(cf.getIdAsLong(),
                        Consts.PROJECT_ID_FOR_GLOBAL_CF);
                }
                else
                {
                    addNull = qfMgr.getAddNull(cf.getIdAsLong(),
                        currentProject.getId());
                }

                if (!addNull)
                {
                    errorCollection.addError(fieldConfig.getFieldId(),
                        i18n.getText("queryfields.error.isnotnull"));
                }
            }
            else
            {
                if (params.size() > 1)
                {
                    errorCollection.addError(fieldConfig.getFieldId(),
                        i18n.getText("queryfields.error.invalid.params"));
                }
                else
                {
                    for (String param : params)
                    {
                        Issue issue = issueMgr.getIssueObject(param);
                        if (issue == null)
                        {
                            errorCollection.addError(fieldConfig.getFieldId(),
                                i18n.getText("queryfields.error.notissue",
                                    param));
                        }
                    }
                }
            }
        }
    }
}
