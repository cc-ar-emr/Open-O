package ca.openosp.openo.webserv.rest;

import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.utility.LoggedInInfo;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/llm")
public class LLMExtractorEndpoint {

    @Autowired
    private LLMExtractorService llmExtractorService;

    @Autowired
    private SecurityInfoManager securityInfoManager;

    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportPatient(
            @QueryParam("demographicId") Integer demographicId,
            @Context HttpServletRequest request) {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromRequest(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_demographic", "r", demographicId)) {
            return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"Access denied\"}")
                        .build();
        }

        String json = llmExtractorService.generatePatientExport(loggedInInfo, demographicId);
        return Response.ok(json).build();
    }
}