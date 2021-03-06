package py.com.sodep.notificationserver.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import py.com.sodep.notificationserver.business.AplicacionBusiness;
import py.com.sodep.notificationserver.exceptions.handlers.BusinessException;

/**
 *
 * @author Vanessa
 */
@Path("/app")
@RequestScoped
public class DeviceService {

    private static final Logger LOGGER = Logger.getLogger(DeviceService.class);

    @Inject
    AplicacionBusiness appBussines;

    @POST
    @Path("/android/{app}/device")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response newDevice(@PathParam(value = "app") String appName, String json) throws BusinessException {
        LOGGER.info("REGISTRACION EN " + appName);
        LOGGER.info(json);
        return Response.ok().build();

    }

}
