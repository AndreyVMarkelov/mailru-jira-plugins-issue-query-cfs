/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

/**
 * Linker field.
 * 
 * @author Andrey Markelov
 */
public class LinkerField
    extends TextCFType
    implements SortableCustomField<String>
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
     * Constructor.
     */
    public LinkerField(
        CustomFieldValuePersister customFieldValuePersister,
        GenericConfigManager genericConfigManager,
        QueryFieldsMgr qfMgr,
        SearchService searchService,
        IssueManager issueMgr)
    {
        super(customFieldValuePersister, genericConfigManager);
        this.qfMgr = qfMgr;
        this.searchService = searchService;
        this.issueMgr = issueMgr;
    }

    @Override
    public Map<String, Object> getVelocityParameters(
        Issue issue,
        CustomField field,
        FieldLayoutItem fieldLayoutItem)
    {
        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("i18n", getI18nBean());

        String jqlData = null;
        boolean addNull = false;
        List<String> options = null;
        if (field.isAllProjects())
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
            addNull = qfMgr.getAddNull(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
            options = qfMgr.getLinkeFieldsOptions(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
        }
        else
        {
            if (issue == null)
            {
                return params;
            }
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), issue.getProjectObject().getId());
            addNull = qfMgr.getAddNull(field.getIdAsLong(), issue.getProjectObject().getId());
            options = qfMgr.getLinkeFieldsOptions(field.getIdAsLong(), issue.getProjectObject().getId());
        }

        String cfValue = field.getValueFromIssue(issue);
        if (Utils.isValidStr(cfValue))
        {
            MutableIssue mi = issueMgr.getIssueObject(cfValue);
            if (mi != null && Utils.isValidStr(mi.getSummary()))
            {
                StringBuilder sb = new StringBuilder();
                if (options.contains("status"))
                {
                    sb.append(getI18nBean().getText("queryfields.opt.status")).append(": ").append(mi.getStatusObject().getName());
                }
                if (options.contains("assignee") && mi.getAssigneeUser() != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    User aUser = mi.getAssigneeUser();
                    String encodedUser;
                    try
                    {
                        encodedUser = URLEncoder.encode(aUser.getName(), "UTF-8");
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        //--> impossible
                        encodedUser = aUser.getName();
                    }

                    sb.append(getI18nBean().getText("queryfields.opt.assignee")).append(": ")
                        .append("<a class='user-hover' rel='").append(aUser.getName()).append("' id='issue_summary_assignee_'")
                        .append(aUser.getName()).append("' href='/secure/ViewProfile.jspa?name='").append(encodedUser)
                        .append("'>").append(aUser.getDisplayName()).append("</a>");
                }
                if (options.contains("priority") && mi.getPriorityObject() != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    sb.append(getI18nBean().getText("queryfields.opt.priority")).append(": ").append(mi.getPriorityObject().getName());
                }
                if (options.contains("due") && mi.getDueDate() != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    sb.append(getI18nBean().getText("queryfields.opt.due")).append(": ").append(ComponentAccessor.getJiraAuthenticationContext().getOutlookDate().format(mi.getDueDate()));
                }

                if (sb.length() > 0)
                {
                    sb.insert(0, " (");
                    sb.append(")");
                }

                IssueData issueData;
                if (options.contains("key"))
                {
                    issueData = new IssueData(mi.getKey().concat(":").concat(mi.getSummary()), sb.toString());
                }
                else
                {
                    issueData = new IssueData(mi.getSummary(), sb.toString());
                }
                params.put("fullValue", issueData);
            }
        }

        if (!Utils.isValidStr(jqlData))
        {
            params.put("jqlNotSet", Boolean.TRUE);
            return params;
        }
        params.put("jqlNotSet", Boolean.FALSE);
        params.put("options", options);

        if (options.contains("editKey"))
        {
            params.put("hasKey", Boolean.TRUE);
        }

        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlData);
        if (parseResult.isValid())
        {
            params.put("jqlNotValid", Boolean.FALSE);
            Query query = parseResult.getQuery();
            try
            {
                Map<String, String> cfVals = new LinkedHashMap<String, String>();
                SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                List<Issue> issues = results.getIssues();
                for (Issue i : issues)
                {
                    cfVals.put(i.getKey(), i.getSummary());
                }

                if (addNull)
                {
                    cfVals.put("Empty", " - ");
                }

                String selected = "";
                String value = (String)issue.getCustomFieldValue(field);
                for (Map.Entry<String, String> cf : cfVals.entrySet())
                {
                    if (value != null && cf.getKey().equals(value))
                    {
                        selected = value;
                        break;
                    }
                }

                if (selected.equals(""))
                {
                    String defaultValue = (String)field.getDefaultValue(issue);
                    if (defaultValue != null && defaultValue.length() > 0 && cfVals.keySet().contains(defaultValue))
                    {
                        selected = defaultValue;
                    }
                }

                if (cfVals != null && !cfVals.isEmpty() && selected.equals(""))
                {
                    selected = cfVals.keySet().iterator().next();
                }
                params.put("selected", selected);
                params.put("isError", Boolean.FALSE);
                params.put("cfVals", cfVals);
            }
            catch (SearchException e)
            {
                params.put("isError", Boolean.TRUE);
            }
        }
        else
        {
            params.put("jqlNotValid", Boolean.TRUE);
            return params;
        }

        return params;
    }
}
