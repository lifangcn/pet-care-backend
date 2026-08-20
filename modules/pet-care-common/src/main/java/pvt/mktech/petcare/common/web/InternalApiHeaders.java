package pvt.mktech.petcare.common.web;

/** Headers shared by authenticated service-to-service requests. */
public final class InternalApiHeaders {

    public static final String SERVICE_TOKEN = "X-Internal-Service-Token";

    private InternalApiHeaders() {
    }
}
