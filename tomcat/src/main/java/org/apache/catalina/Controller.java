package org.apache.catalina;

import org.apache.coyote.http11.request.HttpRequest;
import org.apache.coyote.http11.response.HttpResponse;

public interface Controller {
    HttpResponse getResponse(HttpRequest httpRequest) throws Exception;
}
