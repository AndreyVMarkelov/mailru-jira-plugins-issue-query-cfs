/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

/**
 * PlugIn utility methods.
 * 
 * @author Andrey Markelov
 */
public class Utils
{
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    
    private static final SearchService searchService = ComponentManager
            .getComponentInstanceOfType(SearchService.class);

    /**
     * Get base URL from HTTP request.
     */
    public static String getBaseUrl(HttpServletRequest req)
    {
        return (req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath());
    }

    /**
     * Check that string is not empty and not null.
     */
    public static boolean isValidStr(String str)
    {
        return (str != null && str.length() > 0);
    }

    /**
     * Convert string list to string.
     */
    public static String listToString(List<String> list)
    {
        StringBuilder sb = new StringBuilder();

        if (list != null)
        {
            for (String item : list)
            {
                sb.append(item).append("&");
            }
        }

        return sb.toString();
    }

    /**
     * Convert string to string list.
     */
    public static List<String> stringToList(String str)
    {
        List<String> list = new ArrayList<String>();

        if (str != null)
        {
            StringTokenizer st = new StringTokenizer(str, "&");
            while (st.hasMoreTokens())
            {
                list.add(st.nextToken());
            }
        }

        return list;
    }

    public static List<Issue> executeJQLQuery(String jqlQuery)
    {
        List<Issue> result = null;

        User user = ComponentManager.getInstance()
            .getJiraAuthenticationContext().getLoggedInUser();
        SearchService.ParseResult parseResult = searchService.parseQuery(user,
            jqlQuery);

        if (parseResult.isValid())
        {
            Query query = parseResult.getQuery();
            try
            {
                SearchResults results = searchService.search(user, query,
                    PagerFilter.getUnlimitedFilter());
                result = results.getIssues();
            }
            catch (SearchException e)
            {
                log.error("Utils::search exception during executing JQL", e);
            }
        }

        return result;
    }
    
    public static boolean isOfQueryMultiSelectType(String cfType)
    {
        return Consts.CF_KEY_QUERY_LINKER_MULTI_FIELD.equals(cfType);
    }

    /**
     * Private constructor.
     */
    private Utils() {}
}
