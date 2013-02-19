/*
 * Created by Andrey Markelov 12-02-2013.
 * Copyright Mail.Ru Group 2013. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Query field picker.
 * 
 * @author Andrey Markelov
 */
public class MailRuQueryPickerAction
    extends JiraWebActionSupport
{
    /*
     * Unique ID.
     */
    private static final long serialVersionUID = 8862828963589161294L;

    /*
     * Logger.
     */
    private final Logger log = Logger.getLogger(MailRuQueryPickerAction.class);

    private final ApplicationProperties applicationProperties;

    private QueryFieldsMgr qfMgr;

    private Map<String, String> cfValues;

    private String cfid;

    private String inputid;

    private String returnid;

    private String prId;

    /**
     * Search service.
     */
    private final SearchService searchService;

    /**
     * Constructor.
     */
    public MailRuQueryPickerAction(
        CustomFieldManager cfMgr,
        ApplicationProperties applicationProperties,
        QueryFieldsMgr qfMgr,
        SearchService searchService)
    {
        this.applicationProperties = applicationProperties;
        this.qfMgr = qfMgr;
        this.searchService = searchService;
        this.cfValues = new TreeMap<String, String>();
    }

    @Override
    protected String doExecute() throws Exception
    {
        CustomField field = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObject(cfid);

        String jqlData = null;
        if (field.isAllProjects())
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), Long.valueOf(prId));
        }
        else
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), Long.valueOf(prId));
        }

        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlData);
        if (parseResult.isValid())
        {
            Query query = parseResult.getQuery();
            try
            {
                SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                List<Issue> issues = results.getIssues();
                for (Issue i : issues)
                {
                    cfValues.put(i.getKey(), i.getSummary());
                }
            }
            catch (SearchException e)
            {
                log.error("MailRuQueryPickerAction::doExecute - Search exception", e);
            }
        }

        return super.doExecute();
    }

    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }

    public String getCfid()
    {
        return cfid;
    }

    public Map<String, String> getCfValues()
    {
        return cfValues;
    }

    public String getInputid()
    {
        return inputid;
    }

    public String getPrId()
    {
        return prId;
    }

    public String getReturnid()
    {
        return returnid;
    }

    public void setCfid(String cfid)
    {
        this.cfid = cfid;
    }

    public void setInputid(String inputid)
    {
        this.inputid = inputid;
    }

    public void setPrId(String prId)
    {
        this.prId = prId;
    }

    public void setReturnid(String returnid)
    {
        this.returnid = returnid;
    }
}