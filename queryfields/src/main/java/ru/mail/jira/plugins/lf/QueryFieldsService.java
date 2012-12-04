/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
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
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.XsrfTokenGenerator;
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

    /**
     * Construct.
     */
    public QueryFieldsService(
        QueryFieldsMgr qfMgr,
        SearchService searchService)
    {
        this.qfMgr = qfMgr;
        this.searchService = searchService;
    }

    @POST
    @Path("/initjqldlg")
    @Produces({MediaType.APPLICATION_JSON})
    public Response initJqlDialog(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryFieldsService::initJclDialog - User is not logged");
            return Response.ok(i18n.getText("queryfields.error.notlogged")).status(401).build();
        }

        String cfIdStr = req.getParameter("cfId");
        String prIdStr = req.getParameter("prId");
        String type = req.getParameter("type");
        if (!Utils.isValidStr(cfIdStr) || !Utils.isValidStr(prIdStr) || !Utils.isValidStr(type))
        {
            log.error("QueryFieldsService::initJclDialog - Required parameters are not set");
            return Response.ok(i18n.getText("queryfields.error.notrequiredparms")).status(500).build();
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
            return Response.ok(i18n.getText("queryfields.error.notvalidparms")).status(500).build();
        }

        XsrfTokenGenerator xsrfTokenGenerator = ComponentManager.getComponentInstanceOfType(XsrfTokenGenerator.class);
        String atl_token = xsrfTokenGenerator.generateToken(req);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("i18n", authCtx.getI18nHelper());
        params.put("baseUrl", Utils.getBaseUrl(req));
        params.put("atl_token", atl_token);
        params.put("cfId", cfId);
        params.put("prId", prId);
        params.put("type", type);
        params.put("jqlData", qfMgr.getQueryFieldData(cfId, prId));
        params.put("jqlnull", qfMgr.getAddNull(cfId, prId));

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("key", "queryfields.opt.key");
        map.put("status", "queryfields.opt.status");
        map.put("assignee", "queryfields.opt.assignee");
        map.put("due", "queryfields.opt.due");
        map.put("priority", "queryfields.opt.priority");
        if (type.equals("1"))
        {
            map.put("editKey", "queryfields.opt.editKey");
        }
        params.put("options", map);
        params.put("selectedOptions", qfMgr.getLinkeFieldsOptions(cfId, prId));

        try
        {
            String body = ComponentAccessor.getVelocityManager().getBody("templates/", "setjql.vm", params);
            return Response.ok(new HtmlEntity(body)).build();
        }
        catch (VelocityException vex)
        {
            log.error("QueryFieldsService::initJclDialog - Velocity parsing error", vex);
            return Response.ok(i18n.getText("queryfields.error.internalerror")).status(500).build();
        }
    }

    @POST
    @Path("/setjql")
    @Produces({MediaType.APPLICATION_JSON})
    public Response setJcl(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryFieldsService::setJcl - User is not logged");
            return Response.ok(i18n.getText("queryfields.error.notlogged")).status(401).build();
        }

        XsrfTokenGenerator xsrfTokenGenerator = ComponentManager.getComponentInstanceOfType(XsrfTokenGenerator.class);
        String token = xsrfTokenGenerator.getToken(req);
        if (!xsrfTokenGenerator.generatedByAuthenticatedUser(token))
        {
            log.error("QueryFieldsService::setJcl - There is no token");
            return Response.ok(i18n.getText("queryfields.error.internalerror")).status(500).build();
        }
        else
        {
            String atl_token = req.getParameter("atl_token");
            if (!atl_token.equals(token))
            {
                log.error("QueryFieldsService::setJcl - Token is invalid");
                return Response.ok(i18n.getText("queryfields.error.internalerror")).status(500).build();
            }
        }

        String cfIdStr = req.getParameter("cfId");
        String prIdStr = req.getParameter("prId");
        String data = req.getParameter("jqlclause");
        String jqlnull = req.getParameter("jqlnull");
        String[] options = req.getParameterValues("options");
        if (!Utils.isValidStr(cfIdStr) || !Utils.isValidStr(prIdStr))
        {
            log.error("QueryFieldsService::setJcl - Required parameters are not set");
            return Response.ok(i18n.getText("queryfields.error.notrequiredparms")).status(500).build();
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
            return Response.ok(i18n.getText("queryfields.error.notvalidparms")).status(500).build();
        }

        if (Utils.isValidStr(data))
        {
            String jqlQuery = data;
            if (data.startsWith(Consts.REVERSE_LINK_PART))
            {
                String reserveData = data.substring(Consts.REVERSE_LINK_PART.length());
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
                        String body = ComponentAccessor.getVelocityManager().getBody("templates/", "conferr.vm", params);
                        return Response.ok(new HtmlEntity(body)).status(500).build();
                    }
                    catch (VelocityException vex)
                    {
                        log.error("QueryFieldsService::setJcl - Velocity parsing error", vex);
                        return Response.ok(i18n.getText("queryfields.error.internalerror")).status(500).build();
                    }
                }

                String proj = reserveData.substring(0, inx);
                String cfName = reserveData.substring(inx + 1);

                jqlQuery = String.format(Consts.TEST_QUERY_PATTERN, proj, cfName);
            }

            SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlQuery);
            if (parseResult.isValid())
            {
                qfMgr.setQueryFieldData(cfId, prId, data);
                if (Utils.isValidStr(jqlnull) && jqlnull.equals("on"))
                {
                    qfMgr.setAddNull(cfId, prId, true);
                }
                else
                {
                    qfMgr.setAddNull(cfId, prId, false);
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
                    String body = ComponentAccessor.getVelocityManager().getBody("templates/", "conferr.vm", params);
                    return Response.ok(new HtmlEntity(body)).status(500).build();
                }
                catch (VelocityException vex)
                {
                    log.error("QueryFieldsService::setJcl - Velocity parsing error", vex);
                    return Response.ok(i18n.getText("queryfields.error.internalerror")).status(500).build();
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
}
