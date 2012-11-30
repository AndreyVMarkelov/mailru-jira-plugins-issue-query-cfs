/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
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
        if (field.isAllProjects())
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
        }
        else
        {
            if (issue == null)
            {
                return params;
            }
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), issue.getProjectObject().getId());
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
                Map<String, String> cfVals = new LinkedHashMap<String, String>();
                SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                List<Issue> issues = results.getIssues();
                for (Issue i : issues)
                {
                    cfVals.put(i.getKey(), i.getSummary());
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
