/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.atlassian.configurable.ObjectConfigurationException;
import org.apache.commons.lang.StringUtils;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.ShareTypeSearchParameter;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchResult;

public class FilterListPortlet extends PortletImpl
{

    // References to managers required for this portlet
    private final SearchProvider searchProvider;
    private final SearchRequestService searchRequestService;

   public FilterListPortlet(JiraAuthenticationContext authenticationContext, PermissionManager permissionManager, 
      ApplicationProperties applicationProperties, SearchProvider searchProvider, SearchRequestService searchRequestService
   ) {
      super(authenticationContext, permissionManager, applicationProperties);
      this.searchProvider = searchProvider;
      this.searchRequestService = searchRequestService;
   }

// Pass the data required for the portlet display to the view template
    protected Map getVelocityParams(PortletConfiguration portletConfiguration)
    {
        Map params = new HashMap();
        params.put("portlet", this);  // automatic?
        params.put("portletId", portletConfiguration.getId());

        try {
            String onlyFavourites = portletConfiguration.getProperty("filterlist-onlyfavourites");
            String filterRegexp = portletConfiguration.getProperty("filterlist-regexp");

            String filterDisplay = portletConfiguration.getProperty("filterlist-display");
            params.put("displayAsList", !"dropdown".equalsIgnoreCase(filterDisplay));

            String filterTitle = portletConfiguration.getProperty("filterlist-title");
            params.put("filterTitle", filterTitle);

            String filterIdsConf = portletConfiguration.getTextProperty("filterlist-ids");
            String[] filterIds = filterIdsConf.split("[, \n]");

            params.put("indexing", applicationProperties.getOption(APKeys.JIRA_OPTION_INDEXING));

            boolean loggedIn = (this.authenticationContext.getUser() != null);
            params.put("loggedin", loggedIn);

            String includeLinks = portletConfiguration.getProperty("filterlist-includelinks");
            params.put("includeLinks", "true".equals(includeLinks));

            String includeCounts = portletConfiguration.getProperty("filterlist-includecounts");
            params.put("includeCounts", "true".equals(includeCounts));

            if(loggedIn) {
                // find ALL filters to start with.
                User user = this.authenticationContext.getUser();
                Collection filters = null;
                final JiraServiceContext context = new JiraServiceContextImpl(user);
                final boolean idsExist = filterIds != null && filterIds.length != 0;
                final boolean favorites = Boolean.parseBoolean(onlyFavourites);
                final boolean expExists = StringUtils.isNotEmpty(filterRegexp);
                // yes, matching the regular expression against a regular expression. Awesome.
                // if they want to query their favorites or they have more than a trivial regular expression or they didn't specify any ids,
                // then get all the filters and filter the filters
                if(favorites || (expExists && !filterRegexp.matches("(\\.\\*)+")) || !idsExist) {
                   if(favorites) {
                      filters = this.searchRequestService.getFavouriteFilters( user );
                   } else {
                      // This gets all of the filters (up to the hardcoded 400 anyway at which I expect
                      // performance to have long since become a problem).
                      SharedEntitySearchResult sesr = this.searchRequestService.search(context, new NullSharedEntitySearchParameters(), 0, 400 );
                      filters = sesr.getResults();
                   }

                   if(idsExist && filters != null) {
                        if( !(filterIds.length == 1 && filterIds[0].equals("")) ) {
                             Map filterMap = new HashMap();
                             Iterator iterator = filters.iterator();
                             while(iterator.hasNext()) {
                                 SearchRequest request = (SearchRequest) iterator.next();
                                 String id = request.getId().toString();
                                 filterMap.put(id, request);
                             }
                             filters = new ArrayList();
                             for(int i=0; i<filterIds.length; i++) {
                                 String filterId = filterIds[i];
                                 if(filterMap.containsKey(filterId)) {
                                     filters.add(filterMap.get(filterId));
                                 }
                             }
                        }
                   }

                   if(expExists) {
                       Iterator iterator = filters.iterator();
                       try {
                           while(iterator.hasNext()) {
                               SearchRequest request = (SearchRequest) iterator.next();
                               if(!request.getName().matches(filterRegexp)) {
                                   iterator.remove();
                               }
                           }
                       } catch(java.util.regex.PatternSyntaxException pse) {
                           params.put("regexWarning", pse.getMessage());
                       }
                   }
                }
                // otherwise just look up the filter ids that they provided
                else {
                   filters = new ArrayList<SearchRequest>();
                   for(String id : filterIds) {
                      if(id.matches("\\d+")) {
                         final SearchRequest filter = searchRequestService.getFilter(context, Long.parseLong(id));
                         if(filter != null) {
                            filters.add(filter);
                         }
                      }
                   }
                }
                params.put("chosenFilters", filters);
            }
        } catch(ObjectConfigurationException oce) {
            oce.printStackTrace();
        }
        return params;
    }

   /**
    * called from velocity
    * @param sr search request
    * @return issues
    * @throws SearchException problem
    */
    @SuppressWarnings({"UnusedDeclaration"})
    public long getCountsForFilter(SearchRequest sr) throws SearchException {
        User user = this.authenticationContext.getUser();
        return this.searchProvider.searchCount( sr.getQuery(), user );
    }

    private static class NullSharedEntitySearchParameters implements SharedEntitySearchParameters {
        public String getDescription() { return null; }
        public Boolean getFavourite() { return null; }
        public String getName() { return null; }
        public ShareTypeSearchParameter getShareTypeParameter() { return null; }
        public SharedEntityColumn getSortColumn() { return SharedEntityColumn.NAME; }
        public SharedEntitySearchParameters.TextSearchMode getTextSearchMode() { return null; }
        public String getUserName() { return null; }
        public boolean isAscendingSort() { return true; }
    }

}