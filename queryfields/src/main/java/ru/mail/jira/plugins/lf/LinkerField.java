/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.impl.TextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.web.bean.PagerFilter;

/**
 * 
 * 
 * @author Andrey Markelov
 */
public class LinkerField
    extends TextCFType
    implements SortableCustomField<String>
{
    /**
     * Search request service.
     */
    private final SearchRequestService srMgr;

    /**
     * Constructor.
     */
    public LinkerField(
        CustomFieldValuePersister customFieldValuePersister,
        GenericConfigManager genericConfigManager,
        SearchRequestService srMgr)
    {
        super(customFieldValuePersister, genericConfigManager);
        this.srMgr = srMgr;
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

        SearchRequest search = null;
        Collection<SearchRequest> rqs = srMgr.getOwnedFilters(ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser());
        if (rqs != null)
        {
            for (SearchRequest rq : rqs)
            {
                search = rq;
            }
        }

        try
        {
            List<Issue> issues = ComponentManager.getInstance().getSearchService().search(
                ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser(),
                search.getQuery(),
                PagerFilter.getUnlimitedFilter()).getIssues();
            for (Issue i : issues)
            {
                cfVals.add(i.getKey());
            }
        }
        catch (SearchException e)
        {
            e.printStackTrace();
        }

        cfVals.add("s");

        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("cfVals", cfVals);
        return params;
    }
}
