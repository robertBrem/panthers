package ch.open.panthers.health.boundary;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("health")
public class HealthResource {

    @GET
    public String getHealth() {
        return "alles supi @chOpen";
    }

}
