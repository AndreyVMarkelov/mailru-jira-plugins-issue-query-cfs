/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.XsrfTokenGenerator;

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
     * Construct.
     */
    public QueryFieldsService(
        QueryFieldsMgr qfMgr)
    {
        this.qfMgr = qfMgr;
    }

    @POST
    @Path("/initjcldlg")
    @Produces({MediaType.APPLICATION_JSON})
    public Response initJclDialog(@Context HttpServletRequest req)
    throws VelocityException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryFieldsService::initJclDialog - User is not logged");
            return Response.status(401).build();
        }

        String cfId = req.getParameter("cfId");
        String prId = req.getParameter("prId");

        XsrfTokenGenerator xsrfTokenGenerator = ComponentManager.getComponentInstanceOfType(XsrfTokenGenerator.class);
        String atl_token = xsrfTokenGenerator.generateToken(req);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("i18n", authCtx.getI18nHelper());
        params.put("baseUrl", Utils.getBaseUrl(req));
        params.put("atl_token", atl_token);
        params.put("cfId", cfId);
        params.put("prId", prId);

        return Response.ok(new HtmlEntity(ComponentAccessor.getVelocityManager().getBody("templates/", "setjcl.vm", params))).build();
    }

    @POST
    @Path("/setjcl")
    @Produces({MediaType.APPLICATION_JSON})
    public Response setJcl(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryFieldsService::initJclDialog - User is not logged");
            return Response.status(401).build();
        }

        XsrfTokenGenerator xsrfTokenGenerator = ComponentManager.getComponentInstanceOfType(XsrfTokenGenerator.class);
        String token = xsrfTokenGenerator.getToken(req);
        if (!xsrfTokenGenerator.generatedByAuthenticatedUser(token))
        {
            //JiraWebUtils.getHttpRequest().getContextPath();
            return Response.serverError().build();
        }
        else
        {
            String atl_token = req.getParameter("atl_token");
            if (!atl_token.equals(token))
            {
                return Response.serverError().build();
            }
        }

        ComponentManager.getInstance().getIssueTypeSchemeManager();

        String cfId = req.getParameter("cfId");
        String prId = req.getParameter("prId");
        String data = req.getParameter("email");

        qfMgr.setQueryFieldData(Long.parseLong(cfId), Long.parseLong(prId), data);

        String baseUrl = Utils.getBaseUrl(req);
        return Response.seeOther(URI.create(baseUrl + "/secure/MailSelectConfig.jspa")).build();
    }
}
