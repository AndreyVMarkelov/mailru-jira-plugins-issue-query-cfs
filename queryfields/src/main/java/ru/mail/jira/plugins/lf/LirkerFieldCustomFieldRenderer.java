/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.customfields.CustomFieldValueProvider;
import com.atlassian.jira.issue.customfields.searchers.renderer.CustomFieldRenderer;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.searchers.impl.NamedTerminalClauseCollectingVisitor;
import com.atlassian.jira.plugin.customfield.CustomFieldSearcherModuleDescriptor;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.query.Query;

/**
 * Custom field renderer.
 * 
 * @author Andrey Markelov
 */
public class LirkerFieldCustomFieldRenderer
    extends CustomFieldRenderer
{
    private ClauseNames clauseNames;

    /**
     * Constructor.
     */
    public LirkerFieldCustomFieldRenderer(
        ClauseNames clauseNames,
        CustomFieldSearcherModuleDescriptor customFieldSearcherModuleDescriptor,
        CustomField field,
        CustomFieldValueProvider customFieldValueProvider,
        FieldVisibilityManager fieldVisibilityManager)
    {
        super(clauseNames, customFieldSearcherModuleDescriptor, field, customFieldValueProvider, fieldVisibilityManager);
        this.clauseNames = clauseNames;
    }

    public boolean isRelevantForQuery(
        User searcher,
        Query query)
    {
        final NamedTerminalClauseCollectingVisitor clauseVisitor = new NamedTerminalClauseCollectingVisitor(clauseNames.getJqlFieldNames());
        if(query != null && query.getWhereClause() != null)
        {
            query.getWhereClause().accept(clauseVisitor);
        }

        return clauseVisitor.containsNamedClause();
    }
}
