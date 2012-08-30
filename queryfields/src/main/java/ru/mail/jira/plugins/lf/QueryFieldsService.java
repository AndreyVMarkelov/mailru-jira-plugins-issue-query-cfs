package ru.mail.jira.plugins.lf;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;

/**
 * 
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

    @POST
    @Path("/additem")
    @Produces({MediaType.APPLICATION_JSON})
    public Response addItem(@Context HttpServletRequest req)
    {
        return Response.ok().build();
    }
}
