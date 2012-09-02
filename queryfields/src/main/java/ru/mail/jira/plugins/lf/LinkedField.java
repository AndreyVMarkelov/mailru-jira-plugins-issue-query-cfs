/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.impl.TextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.util.JiraWebUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

/**
 * Linked field.
 * 
 * @author Andrey Markelov
 */
public class LinkedField
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
     * Constructor.
     */
    public LinkedField(
        CustomFieldValuePersister customFieldValuePersister,
        GenericConfigManager genericConfigManager,
        QueryFieldsMgr qfMgr,
        SearchService searchService)
    {
        super(customFieldValuePersister, genericConfigManager);
        this.qfMgr = qfMgr;
        this.searchService = searchService;
    }

    @Override
    public int compare(
        String customFieldObjectValue1,
        String customFieldObjectValue2,
        FieldConfig fieldConfig)
    {
        return super.compare(customFieldObjectValue1, customFieldObjectValue2, fieldConfig);
    }

    @Override
    public Map<String, Object> getVelocityParameters(
        Issue issue,
        CustomField field,
        FieldLayoutItem fieldLayoutItem)
    {
        String jqlData = null;
        if (field.isAllProjects())
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
        }
        else
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), issue.getProjectObject().getId());
        }

        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("i18n", getI18nBean());
        params.put("baseUrl", Utils.getBaseUrl(JiraWebUtils.getHttpRequest()));
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
                Set<String> cfVals = new TreeSet<String>();
                SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                List<Issue> issues = results.getIssues();
                for (Issue i : issues)
                {
                    cfVals.add(i.getKey());
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
