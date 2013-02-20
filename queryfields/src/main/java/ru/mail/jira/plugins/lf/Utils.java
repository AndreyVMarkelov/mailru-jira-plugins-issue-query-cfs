/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.core.entity.ConnectionFactory;
import org.ofbiz.core.entity.DelegatorInterface;
import org.ofbiz.core.entity.GenericEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.mail.jira.plugins.lf.struct.ISQLDataBean;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.query.Query;

/**
 * PlugIn utility methods.
 * 
 * @author Andrey Markelov
 */
public class Utils
{
    private static final String CF_RIGHTS_CLASS_NAME = "ru.mail.jira.plugins.settings.IMailRuCFRights";
    private static final String CF_RIGHTS_METHOD_CAN_EDIT_NAME = "canEdit";
    private static final String CF_RIGHTS_METHOD_CAN_VIEW_NAME = "canView";

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    
    private static final SearchService searchService = ComponentManager
            .getComponentInstanceOfType(SearchService.class);

    private static Object cfRightsInstance;
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
    
    public static boolean isValidLongParam(String str)
    {
        boolean isValidLong = true;

        try
        {
            Long.valueOf(str);
        }
        catch (NumberFormatException e)
        {
            isValidLong = false;
        }
        return isValidLong;
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
    
    public static String getKeyByQueryFlag(boolean queryFlag)
    {
        return queryFlag ? Consts.LANG_TYPE_SQL : Consts.LANG_TYPE_JQL;
    }

    public static List<ISQLDataBean> executeSQLQuery(String sqlQuery,
        Class<? extends ISQLDataBean> dataClass)
    {
        List<ISQLDataBean> data = null;

        DelegatorInterface delegator = (DelegatorInterface) ComponentManager
            .getComponentInstanceOfType(DelegatorInterface.class);
        String helperName = delegator.getGroupHelperName("default");

        Connection conn = null;
        try
        {
            conn = ConnectionFactory.getConnection(helperName);
        }
        catch (SQLException e)
        {
            log.error("Utils::executeSQLQuery - Exception during getting connection");
            return null;
        }
        catch (GenericEntityException e)
        {
            log.error("Utils::executeSQLQuery - Generic Entity Exception occured durring getting connection");
            return null;
        }

        if (conn != null)
        {
            PreparedStatement ps = null;

            try
            {
                ps = conn.prepareStatement(sqlQuery);

                ResultSet rs = ps.executeQuery();

                data = new ArrayList<ISQLDataBean>();
                fillDataFromResultSet(rs, data, dataClass);
                if (!isPreparedDataValid(data))
                {
                    log.error("Utils::executeSQLQuery - Prepared data hasn't passed validation. It may be there is problem with query: "
                        + sqlQuery);
                    data = null;
                }

                rs.close();
                ps.close();
            }
            catch (SQLException e)
            {
                log.error("Utils::executeSQLQuery - SQL Exception occured while executing query: "
                    + e.getMessage());
                System.out.println(e.getMessage());
                data = null;
            }
            catch (InstantiationException e)
            {
                log.error("Utils::executeSQLQuery - InstantiationException occured while executing query: "
                    + e.getMessage() + sqlQuery);
                data = null;
            }
            catch (IllegalAccessException e)
            {
                log.error("Utils::executeSQLQuery - IllegalAccessException occured while executing query: "
                    + e.getMessage() + sqlQuery);
                data = null;
            }
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                log.error("Utils::executeSQLQuery - SQL Exception occured while closing connection: ");
            }
        }

        return data;
    }

    private static void fillDataFromResultSet(ResultSet rs,
        List<ISQLDataBean> data, Class<? extends ISQLDataBean> dataClass)
        throws SQLException, InstantiationException, IllegalAccessException
    {
        ISQLDataBean element;
        while (rs.next())
        {
            element = dataClass.newInstance();
            element.setName(rs.getString(ISQLDataBean.PROPERTY_NAME_NAME));
            element.setDescription(rs
                .getString(ISQLDataBean.PROPERTY_NAME_DESCRIPTION));
            element.setPreference(rs
                .getString(ISQLDataBean.PROPERTY_NAME_PREFERENCE));
            element.setPreferenceimage(rs
                .getString(ISQLDataBean.PROPERTY_NAME_PREFERENCE_IMAGE));
            element.setState(rs.getString(ISQLDataBean.PROPERTY_NAME_STATE));
            element.setStateimage(rs
                .getString(ISQLDataBean.PROPERTY_NAME_STATEIMAGE));
            element.setType(rs.getString(ISQLDataBean.PROPERTY_NAME_TYPE));
            element.setTypeimage(rs
                .getString(ISQLDataBean.PROPERTY_NAME_TYPEIMAGE));

            data.add(element);
        }
    }
    /**
     * key validation
     */
    private static boolean isPreparedDataValid(List<ISQLDataBean> data)
    {
        for (ISQLDataBean autocompleteUniversalData : data)
        {
            if (autocompleteUniversalData.getName() == null)
            {
                return false;
            }
        }
        return true;
    }
    
    // CF Rights injection
    private static Object getCfRightsClass()
    {
        if (cfRightsInstance == null)
        {
            PluginAccessor pluginAccessor = ComponentManager.getInstance()
                .getPluginAccessor();
            Class<?> mailRuCfRightsClass;
            try
            {
                mailRuCfRightsClass = pluginAccessor.getClassLoader()
                    .loadClass(CF_RIGHTS_CLASS_NAME);
            }
            catch (ClassNotFoundException e)
            {
                log.info("Utils::getCfRightsClass - ClassNotfoundException "
                    + CF_RIGHTS_CLASS_NAME
                    + " not found. It is possible that plugin is turned off");
                return null;
            }
            cfRightsInstance = ComponentManager
                .getOSGiComponentInstanceOfType(mailRuCfRightsClass);
            if (cfRightsInstance == null)
            {
                log.info("Utils::getCfRightsClass - Class "
                    + CF_RIGHTS_CLASS_NAME
                    + ". Method getOSGiComponentInstanceOfType failed to load component");
            }
        }
        return cfRightsInstance;
    }

    private static boolean canEditCF(User user, String cfId, Project project)
    {
        return getPermission(user, cfId, project,
            CF_RIGHTS_METHOD_CAN_EDIT_NAME, "canEditCF");
    }

    private static boolean canViewCF(User user, String cfId, Project project)
    {
        return getPermission(user, cfId, project,
            CF_RIGHTS_METHOD_CAN_VIEW_NAME, "canViewCF");
    }

    /**
     * adds "canView" and "canEdit" keys to map
     */
    public static void addViewAndEditParameters(Map<String, Object> params,
        String cfId)
    {
        UserProjectHistoryManager userProjectHistoryManager = ComponentManager
            .getComponentInstanceOfType(UserProjectHistoryManager.class);
        JiraAuthenticationContext authCtx = ComponentManager.getInstance()
            .getJiraAuthenticationContext();
        User currentUser = authCtx.getLoggedInUser();
        Project currentProject = userProjectHistoryManager.getCurrentProject(
            Permissions.BROWSE, currentUser);

        boolean canEdit = Utils.canEditCF(currentUser, cfId, currentProject);
        params.put("canEdit", canEdit);
        if (canEdit)
        {
            params.put("canView", true);
        }
        else
        {
            params.put("canView",
                Utils.canViewCF(currentUser, cfId, currentProject));
        }
    }

    private static boolean getPermission(User user, String cfId,
        Project project, String externalMethodName, String internalMethodName)
    {
        Object cfRights = getCfRightsClass();

        // occurs only if class not found or not load
        // it's possible that parent plugin was disabled manually
        // so we should return true
        if (cfRights == null)
        {
            return true;
        }

        Method[] methods = cfRights.getClass().getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            if (externalMethodName.equals(methods[i].getName()))
            {
                Boolean result = Boolean.FALSE;
                try
                {
                    result = (Boolean) methods[i].invoke(cfRights, user, cfId,
                        project);
                }
                catch (IllegalArgumentException e)
                {
                    log.error(getErrorMessage(user, cfId, project,
                        internalMethodName, externalMethodName,
                        "IllegalArgumentException"));
                }
                catch (IllegalAccessException e)
                {
                    log.error(getErrorMessage(user, cfId, project,
                        internalMethodName, externalMethodName,
                        "IllegalAccessException"));
                }
                catch (InvocationTargetException e)
                {
                    log.error(getErrorMessage(user, cfId, project,
                        internalMethodName, externalMethodName,
                        "InvocationTargetException"));
                    cfRightsInstance = null; // set instance to null it's
                                             // possible that class was disabled
                }

                return result;
            }
        }
        return false;
    }

    private static String getErrorMessage(User user, String cfId,
        Project project, String internalMethodName, String externalMethodName,
        String exception)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Utils::");
        sb.append(internalMethodName);
        sb.append(" - Class ");
        sb.append(CF_RIGHTS_CLASS_NAME);
        sb.append(". ");
        sb.append(exception);
        sb.append(" occured invoking ");
        sb.append(externalMethodName);
        sb.append(" method ");

        return sb.toString();
    }

    /**
     * Private constructor.
     */
    private Utils() {}
}
