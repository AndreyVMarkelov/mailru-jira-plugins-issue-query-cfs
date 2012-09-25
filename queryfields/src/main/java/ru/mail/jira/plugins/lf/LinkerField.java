/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
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
import com.atlassian.jira.util.JiraWebUtils;
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
        params.put("baseUrl", Utils.getBaseUrl(JiraWebUtils.getHttpRequest()));

        String jqlData = null;
        boolean addNull = false;
        if (field.isAllProjects())
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
            addNull = qfMgr.getAddNull(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
        }
        else
        {
            if (issue == null)
            {
                return params;
            }
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), issue.getProjectObject().getId());
            addNull = qfMgr.getAddNull(field.getIdAsLong(), issue.getProjectObject().getId());
        }

        String cfValue = field.getValueFromIssue(issue);
        if (Utils.isValidStr(cfValue))
        {
            MutableIssue mi = issueMgr.getIssueObject(cfValue);
            if (mi != null && Utils.isValidStr(mi.getSummary()))
            {
                params.put("fullValue", mi.getSummary());
            }
        }

        if (!Utils.isValidStr(jqlData))
        {
            params.put("jqlNotSet", Boolean.TRUE);
            return params;
        }
        params.put("jqlNotSet", Boolean.FALSE);

        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlData);
        if (parseResult.isValid())
        {
            params.put("jqlNotValid", Boolean.FALSE);
            Query query = parseResult.getQuery();
            try
            {
                Map<String, String> cfVals = new TreeMap<String, String>();
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

                String selected = "Empty";
                String value = (String)params.get("value");
                for (Map.Entry<String, String> cf : cfVals.entrySet())
                {
                    if (value != null && cf.getKey().equals(value))
                    {
                        selected = value;
                        break;
                    }
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
