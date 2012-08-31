/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

/**
 * 
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
     * Constructor.
     */
    public LinkedField(
        CustomFieldValuePersister customFieldValuePersister,
        GenericConfigManager genericConfigManager,
        QueryFieldsMgr qfMgr)
    {
        super(customFieldValuePersister, genericConfigManager);
        this.qfMgr = qfMgr;
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
        Set<String> cfVals = new TreeSet<String>();

        SearchService searchService = ComponentManager.getInstance().getSearchService();
        String jqlQuery = "project = \"DEMO\" and assignee = currentUser()";
        SearchService.ParseResult parseResult = searchService.parseQuery(ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser(), jqlQuery);

        if (parseResult.isValid())
        {
            // Carry On
        }
        else
        {
            // Log the error and exit!
        }

        Query query = parseResult.getQuery();
        try
        {
            SearchResults results = searchService.search(ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser(), query, PagerFilter.getUnlimitedFilter());
            List<Issue> issues = results.getIssues();
            for (Issue i : issues)
            {
                cfVals.add(i.getKey());
            }
        }
        catch (SearchException e)
        {
            e.printStackTrace();
        }

        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("cfVals", cfVals);
        return params;
    }
}
