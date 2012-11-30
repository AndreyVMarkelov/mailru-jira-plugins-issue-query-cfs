/*
 * Created by Andrey Markelov 29-11-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.JiraWebUtils;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.WorkflowException;

/**
 * Validate linked issues statuses.
 *
 * @author Andrey Markelov
 */
public class SingleLinkerFieldValidator
    implements Validator
{
    /**
     * Constructor.
     */
    public SingleLinkerFieldValidator() {}

    @Override
    public void validate(
        Map transientVars,
        Map args,
        PropertySet ps)
    throws InvalidInputException, WorkflowException
    {
        Issue issue = (Issue) transientVars.get("issue");

        String cfId = (String) args.get("cfId");
        String invalid_statuses = (String) args.get("invalid_statuses");

        if (cfId != null && cfId.length() > 0)
        {
            CustomField cfObj = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObject(cfId);
            if (cfObj != null)
            {
                Object cfValObj = issue.getCustomFieldValue(cfObj);
                if (cfValObj != null)
                {
                    String linkedIssueKey = cfValObj.toString();
                    Issue linkedIssue = ComponentManager.getInstance().getIssueManager().getIssueObject(linkedIssueKey);
                    if (linkedIssue != null)
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
                        if (params.contains(linkedIssue.getStatusObject().getId()))
                        {
                            String issueUrl = Utils.getBaseUrl(JiraWebUtils.getHttpRequest()) + "/browse/" + linkedIssue.getKey();
                            throw new InvalidInputException(
                                ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("queryfields.linkervalidator.error", issueUrl, linkedIssue.getStatusObject().getName()));
                        }
                    }
                }
            }
        }
    }
}
