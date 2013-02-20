/*
 * Created by Andrey Markelov 29-08-2012. Copyright Mail.Ru Group 2012. All
 * rights reserved.
 */
package ru.mail.jira.plugins.lf;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;

import ru.mail.jira.plugins.lf.struct.HtmlEntity;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.xsrf.XsrfTokenGenerator;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;


/**
 * Query Issue Linking Custom Fields plugIn REST service.
 * 
 * @author Andrey Markelov
 */
@Path("/queryfieldssrv")
public class QueryFieldsService
{
    /**
     * Logger.
     */
    private final Logger log = Logger.getLogger(QueryFieldsService.class);

    /**
     * PlugIn data manager.
     */
    private final QueryFieldsMgr qfMgr;

    /**
     * Search service.
     */
    private final SearchService searchService;

    private final PermissionManager permissionManager;

    /**
     * Construct.
     */
    public QueryFieldsService(QueryFieldsMgr qfMgr,
        SearchService searchService, PermissionManager permissionManager)
    {
        this.qfMgr = qfMgr;
        this.searchService = searchService;
        this.permissionManager = permissionManager;
    }

    @POST
    @Path("/initjqldlg")
    @Produces({MediaType.APPLICATION_JSON})
    public Response initJqlDialog(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance()
            .getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryFieldsService::initJclDialog - User is not logged");
            return Response.ok(i18n.getText("queryfields.error.notlogged"))
                .status(401).build();
        }

        String cfIdStr = req.getParameter("cfId");
        String prIdStr = req.getParameter("prId");
        String type = req.getParameter("type");
        if (!Utils.isValidStr(cfIdStr) || !Utils.isValidStr(prIdStr)
            || !Utils.isValidStr(type))
        {
            log.error("QueryFieldsService::initJclDialog - Required parameters are not set");
            return Response
                .ok(i18n.getText("queryfields.error.notrequiredparms"))
                .status(500).build();
        }

        long cfId;
        long prId;
        try
        {
            cfId = Long.parseLong(cfIdStr);
            prId = Long.parseLong(prIdStr);
        }
        catch (NumberFormatException nex)
        {
            log.error("QueryFieldsService::initJclDialog - Parameters are not valid");
            return Response.ok(i18n.getText("queryfields.error.notvalidparms"))
                .status(500).build();
        }

        XsrfTokenGenerator xsrfTokenGenerator = ComponentManager
            .getComponentInstanceOfType(XsrfTokenGenerator.class);
        String atl_token = xsrfTokenGenerator.generateToken(req);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("i18n", authCtx.getI18nHelper());
        params.put("baseUrl", Utils.getBaseUrl(req));
        params.put("atl_token", atl_token);
        params.put("cfId", cfId);
        params.put("prId", prId);
        params.put("type", type);
        params.put("jqlData", qfMgr.getQueryFieldData(cfId, prId));
        params.put("sqlData", qfMgr.getQueryFieldSQLData(cfId, prId));
        params.put("jqlnull", qfMgr.getAddNull(cfId, prId));
        params.put("autocompleteview", qfMgr.isAutocompleteView(cfId, prId));
        params.put("queryflag", qfMgr.getQueryFlag(cfId));

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("key", "queryfields.opt.key");
        map.put("status", "queryfields.opt.status");
        map.put("assignee", "queryfields.opt.assignee");
        map.put("due", "queryfields.opt.due");
        map.put("priority", "queryfields.opt.priority");
        if (type.equals("1"))
        {
            map.put("editKey", "queryfields.opt.editKey");
            map.put("justDesc", "queryfields.opt.justDesc");
        }
        params.put("options", map);
        params.put("selectedOptions", qfMgr.getLinkeFieldsOptions(cfId, prId));

        try
        {
            String body = ComponentAccessor.getVelocityManager().getBody(
                "templates/", "setjql.vm", params);
            return Response.ok(new HtmlEntity(body)).build();
        }
        catch (VelocityException vex)
        {
            log.error(
                "QueryFieldsService::initJclDialog - Velocity parsing error",
                vex);
            return Response.ok(i18n.getText("queryfields.error.internalerror"))
                .status(500).build();
        }
    }

    @POST
    @Path("/setjql")
    @Produces({MediaType.APPLICATION_JSON})
    public Response setJcl(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance()
            .getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryFieldsService::setJcl - User is not logged");
            return Response.ok(i18n.getText("queryfields.error.notlogged"))
                .status(401).build();
        }

        XsrfTokenGenerator xsrfTokenGenerator = ComponentManager
            .getComponentInstanceOfType(XsrfTokenGenerator.class);
        String token = xsrfTokenGenerator.getToken(req);
        if (!xsrfTokenGenerator.generatedByAuthenticatedUser(token))
        {
            log.error("QueryFieldsService::setJcl - There is no token");
            return Response.ok(i18n.getText("queryfields.error.internalerror"))
                .status(500).build();
        }
        else
        {
            String atl_token = req.getParameter("atl_token");
            if (!atl_token.equals(token))
            {
                log.error("QueryFieldsService::setJcl - Token is invalid");
                return Response
                    .ok(i18n.getText("queryfields.error.internalerror"))
                    .status(500).build();
            }
        }

        String cfIdStr = req.getParameter("cfId");
        String prIdStr = req.getParameter("prId");
        String jqlData = req.getParameter("jqlclause");
        String sqlData = req.getParameter("sqlclause");
        String jqlnull = req.getParameter("jqlnull");
        String autocompleteView = req.getParameter("autocompleteview");
        String queryFlag = req.getParameter("queryflag");
        String[] options = req.getParameterValues("options");
        if (!Utils.isValidStr(cfIdStr) || !Utils.isValidStr(prIdStr))
        {
            log.error("QueryFieldsService::setJcl - Required parameters are not set");
            return Response
                .ok(i18n.getText("queryfields.error.notrequiredparms"))
                .status(500).build();
        }

        long cfId;
        long prId;
        try
        {
            cfId = Long.parseLong(cfIdStr);
            prId = Long.parseLong(prIdStr);
        }
        catch (NumberFormatException nex)
        {
            log.error("QueryFieldsService::setJcl - Parameters are not valid");
            return Response.ok(i18n.getText("queryfields.error.notvalidparms"))
                .status(500).build();
        }

        if (Utils.isValidStr(sqlData))
        {
            qfMgr.setQueryFieldSQLData(cfId, prId, sqlData);
        }
        else
        {
            qfMgr.setQueryFieldSQLData(cfId, prId, Consts.EMPTY_VALUE);
        }

        if (Utils.isValidStr(jqlData))
        {
            String jqlQuery = jqlData;
            if (jqlData.startsWith(Consts.REVERSE_LINK_PART))
            {
                String reserveData = jqlData.substring(Consts.REVERSE_LINK_PART
                    .length());
                int inx = reserveData.indexOf("|");
                if (inx < 0)
                {
                    Set<String> errs = new TreeSet<String>();
                    errs.add(i18n.getText("queryfields.error.rlinkerror"));
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("errs", errs);
                    params.put("i18n", i18n);

                    try
                    {
                        String body = ComponentAccessor.getVelocityManager()
                            .getBody("templates/", "conferr.vm", params);
                        return Response.ok(new HtmlEntity(body)).status(500)
                            .build();
                    }
                    catch (VelocityException vex)
                    {
                        log.error(
                            "QueryFieldsService::setJcl - Velocity parsing error",
                            vex);
                        return Response
                            .ok(i18n.getText("queryfields.error.internalerror"))
                            .status(500).build();
                    }
                }

                String proj = reserveData.substring(0, inx);
                String cfName = reserveData.substring(inx + 1);

                jqlQuery = String.format(Consts.TEST_QUERY_PATTERN, proj,
                    cfName);
            }

            SearchService.ParseResult parseResult = searchService.parseQuery(
                user, jqlQuery);
            if (parseResult.isValid())
            {
                qfMgr.setQueryFieldData(cfId, prId, jqlData);
                if (Utils.isValidStr(jqlnull) && jqlnull.equals("on"))
                {
                    qfMgr.setAddNull(cfId, prId, true);
                }
                else
                {
                    qfMgr.setAddNull(cfId, prId, false);
                }
                if (Utils.isValidStr(autocompleteView)
                    && autocompleteView.equals("on"))
                {
                    qfMgr.setAutocompleteView(cfId, prId, true);
                }
                else
                {
                    qfMgr.setAutocompleteView(cfId, prId, false);
                }
                List<String> optList = new ArrayList<String>();
                if (options != null)
                {
                    for (String option : options)
                    {
                        optList.add(option);
                    }
                }
                qfMgr.setLinkerFieldOptions(cfId, prId, optList);
            }
            else
            {
                MessageSet ms = parseResult.getErrors();
                Set<String> errs = ms.getErrorMessages();
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("errs", errs);
                params.put("i18n", i18n);

                try
                {
                    String body = ComponentAccessor.getVelocityManager()
                        .getBody("templates/", "conferr.vm", params);
                    return Response.ok(new HtmlEntity(body)).status(500)
                        .build();
                }
                catch (VelocityException vex)
                {
                    log.error(
                        "QueryFieldsService::setJcl - Velocity parsing error",
                        vex);
                    return Response
                        .ok(i18n.getText("queryfields.error.internalerror"))
                        .status(500).build();
                }
            }
        }
        else
        {
            qfMgr.setQueryFieldData(cfId, prId, "");
            if (Utils.isValidStr(jqlnull) && jqlnull.equals("on"))
            {
                qfMgr.setAddNull(cfId, prId, true);
            }
            else
            {
                qfMgr.setAddNull(cfId, prId, false);
            }
            if (Utils.isValidStr(autocompleteView)
                && autocompleteView.equals("on"))
            {
                qfMgr.setAutocompleteView(cfId, prId, true);
            }
            else
            {
                qfMgr.setAutocompleteView(cfId, prId, false);
            }
            List<String> optList = new ArrayList<String>();
            if (options != null)
            {
                for (String option : options)
                {
                    optList.add(option);
                }
            }
            qfMgr.setLinkerFieldOptions(cfId, prId, optList);
        }

        return Response.ok().build();
    }

    @GET
    @Path("/switchlang")
    @Produces({MediaType.TEXT_HTML})
    public Response switchLang(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance()
            .getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryFieldsService::switchLang - User is not logged");
            return Response.ok(i18n.getText("queryfields.error.notlogged"))
                .status(401).build();
        }
        if (!permissionManager.hasPermission(Permissions.ADMINISTER, user))
        {
            log.error("QueryFieldsService::switchLang - User is not admin");
            return Response.ok(i18n.getText("queryfields.error.notadmin"))
                .status(403).build();
        }

        String cfKey = req.getParameter("cfKey");

        if (!Utils.isValidLongParam(cfKey))
        {
            log.error("QueryFieldsService::switchLang - Parameters are not valid");
            return Response.ok(i18n.getText("queryfields.error.notvalidparms"))
                .status(500).build();
        }

        CustomField cf = ComponentAccessor.getCustomFieldManager()
            .getCustomFieldObject(Long.valueOf(cfKey));
        if (cf == null)
        {
            log.error("QueryFieldsService::switchLang - Parameters are not valid");
            return Response.ok(i18n.getText("queryfields.error.notvalidparms"))
                .status(500).build();
        }

        UserProjectHistoryManager userProjectHistoryManager = ComponentManager
            .getComponentInstanceOfType(UserProjectHistoryManager.class);
        Project currentProject = userProjectHistoryManager.getCurrentProject(
            Permissions.BROWSE, user);
        if (currentProject == null)
        {
            log.error("QueryFieldsService::switchLang - Unknown current project");
            return Response
                .ok(i18n.getText("queryfields.error.invalid.curproject"))
                .status(500).build();
        }

        long cfId = cf.getIdAsLong();
        long projectId = currentProject.getId();
        qfMgr.setQueryFlag(cfId,
            !qfMgr.getQueryFlag(cfId));

        return getRefererResponse(req, ComponentManager.getInstance()
            .getJiraAuthenticationContext().getI18nHelper());
    }

    @POST
    @Path("/sqlhelp")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSqlHelp(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance()
            .getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();

        try
        {
            String body = ComponentAccessor.getVelocityManager().getBody(
                "templates/", "sql-help.vm", new HashMap<String, Object>());
            return Response.ok(new HtmlEntity(body)).build();
        }
        catch (VelocityException vex)
        {
            log.error(
                "QueryFieldsService::getSqlHelp - Velocity parsing error", vex);
            return Response.ok(i18n.getText("queryfields.velocity.parseerror"))
                .status(500).build();
        }
    }

    private Response getRefererResponse(HttpServletRequest req, I18nHelper i18n)
    {
        String referrer = req.getHeader("referer");
        URI uri;
        try
        {
            uri = new URI(referrer);
        }
        catch (URISyntaxException e)
        {
            log.error("QueryFieldsService - Invalid uri");
            return Response
                .ok(i18n.getText("mailru.queryfields.service.invalid.uri"))
                .status(500).build();
        }

        return Response.seeOther(uri).build();
    }

}
