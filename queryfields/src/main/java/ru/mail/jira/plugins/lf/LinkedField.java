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
import com.atlassian.jira.issue.customfields.converters.StringConverterImpl;
import com.atlassian.jira.issue.customfields.impl.CalculatedCFType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

/**
 * Linked field.
 * 
 * @author Andrey Markelov
 */
public class LinkedField
    extends CalculatedCFType
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
     * Constructor.
     */
    public LinkedField(
        QueryFieldsMgr qfMgr,
        SearchService searchService)
    {
        this.qfMgr = qfMgr;
        this.searchService = searchService;
    }

    @Override
    public Object getSingularObjectFromString(
        final String string)
    throws FieldValidationException
    {
        return string;
    }

    @Override
    public String getStringFromSingularObject(
        final Object value)
    {
        assertObjectImplementsType(String.class, value);
        return StringConverterImpl.convertNullToEmpty((String) value);
    }

    @Override
    public Object getValueFromIssue(
        CustomField field,
        Issue issue)
    {
        if (issue != null && issue.getKey() != null)
        {
            return issue.getKey();
        }
        else
        {
            return "";
        }
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
        List<String> options = null;
        if (field.isAllProjects())
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
            options = qfMgr.getLinkeFieldsOptions(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
        }
        else
        {
            if (issue == null)
            {
                return params;
            }
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), issue.getProjectObject().getId());
            options = qfMgr.getLinkeFieldsOptions(field.getIdAsLong(), issue.getProjectObject().getId());
        }

        if (!Utils.isValidStr(jqlData))
        {
            params.put("jqlNotSet", Boolean.TRUE);
            return params;
        }
        params.put("jqlNotSet", Boolean.FALSE);

        String jqlQuery = jqlData;
        if (jqlData.startsWith(Consts.REVERSE_LINK_PART))
        {
            String reserveData = jqlData.substring(Consts.REVERSE_LINK_PART.length());
            int inx = reserveData.indexOf("|");
            if (inx < 0)
            {
                params.put("jqlNotValid", Boolean.TRUE);
                return params;
            }

            String proj = reserveData.substring(0, inx);
            String cfName = reserveData.substring(inx + 1);

            if (issue.getKey() == null)
            {
                return params;
            }
            jqlQuery = String.format(Consts.RLINK_QUERY_PATTERN, proj, cfName, issue.getKey());
        }
        else
        {
            if (jqlQuery.contains("RLINK"))
            {
                if (issue.getKey() == null)
                {
                    return params;
                }
                jqlQuery = jqlQuery.replace("RLINK", issue.getKey());
            }
        }

        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlQuery);
        if (parseResult.isValid())
        {
            params.put("jqlNotValid", Boolean.FALSE);
            Query query = parseResult.getQuery();
            try
            {
                Map<String, IssueData> cfVals = new LinkedHashMap<String, IssueData>();
                SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                List<Issue> issues = results.getIssues();
                for (Issue i : issues)
                {
                    StringBuilder sb = new StringBuilder();
                    if (options.contains("status"))
                    {
                        sb.append(getI18nBean().getText("queryfields.opt.status")).append(": ").append(i.getStatusObject().getName());
                    }
                    if (options.contains("assignee") && i.getAssigneeUser() != null)
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(", ");
                        }
                        User aUser = i.getAssigneeUser();
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
                    if (options.contains("priority") && i.getPriorityObject() != null)
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(", ");
                        }
                        sb.append(getI18nBean().getText("queryfields.opt.priority")).append(": ").append(i.getPriorityObject().getName());
                    }
                    if (options.contains("due") && i.getDueDate() != null)
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(", ");
                        }
                        sb.append(getI18nBean().getText("queryfields.opt.due")).append(": ").append(ComponentAccessor.getJiraAuthenticationContext().getOutlookDate().format(i.getDueDate()));
                    }

                    if (sb.length() > 0)
                    {
                        sb.insert(0, " (");
                        sb.append(")");
                    }

                    IssueData issueData;
                    if (options.contains("key"))
                    {
                        issueData = new IssueData(i.getKey().concat(":").concat(i.getSummary()), sb.toString());
                    }
                    else
                    {
                        issueData = new IssueData(i.getSummary(), sb.toString());
                    }
                    cfVals.put(i.getKey(), issueData);
                }
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
