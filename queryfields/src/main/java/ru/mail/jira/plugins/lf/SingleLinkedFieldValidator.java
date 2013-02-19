/*
 * Created by Andrey Markelov 29-11-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.util.JiraWebUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.WorkflowException;

/**
 * Validate linked issues statuses.
 *
 * @author Andrey Markelov
 */
public class SingleLinkedFieldValidator
    implements Validator
{
    /**
     * Constructor.
     */
    public SingleLinkedFieldValidator() {}

    @Override
    public void validate(
        Map transientVars,
        Map args,
        PropertySet ps)
    throws InvalidInputException, WorkflowException
    {
        Issue issue = (Issue) transientVars.get("issue");
 
        String jql = (String) args.get("jql");
        String invalid_statuses = (String) args.get("invalid_statuses");

        if (jql != null && jql.length() > 0)
        {
            if (jql.contains(Consts.ISSUE_RLINK))
            {
                if (issue.getKey() == null)
                {
                    return;
                }
                jql = jql.replace(Consts.ISSUE_RLINK, issue.getKey());
            }

            User user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
            SearchService.ParseResult parseResult = ComponentManager.getInstance().getSearchService().parseQuery(user, jql);
            if (parseResult.isValid())
            {
                Set<String> params = new TreeSet<String>();
                if (invalid_statuses != null)
                {
                    StringTokenizer st = new StringTokenizer(invalid_statuses, "&");
                    while (st.hasMoreTokens())
                    {
                        String token = st.nextToken();
                        if (token.length() > 0)
                        {
                            params.add(token.trim());
                        }
                    }
                }

                Query query = parseResult.getQuery();
                try
                {
                    SearchResults results = ComponentManager.getInstance().getSearchService().search(user, query, PagerFilter.getUnlimitedFilter());
                    List<Issue> issues = results.getIssues();
                    for (Issue i : issues)
                    {
                        if (params.contains(i.getStatusObject().getId()))
                        {
                            String issueUrl = Utils.getBaseUrl(JiraWebUtils.getHttpRequest()) + "/browse/" + i.getKey();
                            throw new InvalidInputException(
                                ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("queryfields.linkedvalidator.error", issueUrl, i.getStatusObject().getName()));
                        }
                    }
                }
                catch (SearchException e)
                {
                    //--> nothing to do
                }
            }
        }
    }
}
