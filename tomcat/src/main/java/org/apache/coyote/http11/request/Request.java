package org.apache.coyote.http11.request;

import static org.apache.coyote.http11.request.RequestBody.EMPTY_REQUEST_BODY;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.coyote.http11.common.HttpVersion;
import org.apache.coyote.http11.common.Session;
import org.apache.coyote.http11.common.SessionManager;
import org.apache.coyote.http11.cookie.Cookie;

public class Request {
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String JSESSIONID = "JSESSIONID";
    private final RequestLine line;
    private final RequestHeader header;
    private final RequestBody body;
    private final Cookie cookie;
    private final SessionManager sessionManager = new SessionManager();

    private Request(final RequestLine line, final RequestHeader header, final RequestBody body, final Cookie cookie) {
        this.line = line;
        this.header = header;
        this.body = body;
        this.cookie = cookie;
    }

    public static Request convert(BufferedReader bufferedReader) throws IOException {
        RequestLine requestLine = RequestLine.convert(bufferedReader.readLine());
        RequestHeader requestHeader = RequestHeader.convert(bufferedReader);
        final String contentLength = requestHeader.getHeader().get(CONTENT_LENGTH);
        final Cookie cookie = Cookie.from(requestHeader.getCookie());
        if (Objects.isNull(contentLength)) {
            return new Request(requestLine, requestHeader, EMPTY_REQUEST_BODY, cookie);
        }
        return new Request(requestLine, requestHeader,
                RequestBody.convert(bufferedReader, Integer.parseInt(contentLength)),
                cookie);
    }

    public Session getSession() {
        Session oldSession = getOldSession();
        if (Objects.isNull(oldSession)) {
            final Session newSession = Session.generate();
            sessionManager.add(newSession.getId(), newSession);
            return newSession;
        }
        return oldSession;
    }

    private Session getOldSession() {
        final String jsessionid = cookie.findByKey(JSESSIONID);
        if (Objects.isNull(jsessionid)) {
            return null;
        }
        return sessionManager.getById(jsessionid);
    }

    public boolean hasUserInSession() {
        final String jsessionid = cookie.findByKey(JSESSIONID);
        if (Objects.isNull(jsessionid)) {
            return false;
        }

        final Session session = sessionManager.getById(jsessionid);
        if (Objects.isNull(session)) {
            return false;
        }

        return !Objects.isNull(session.get("user"));
    }

    public Map<String, String> getBodies() {
        return body.getBodies();
    }

    public RequestLine getLine() {
        return line;
    }

    public Map<String, String> getHeader() {
        return header.getHeader();
    }

    public HttpMethod getHttpMethod() {
        return line.getHttpMethod();
    }

    public HttpVersion getHttpVersion() {
        return line.getHttpVersion();
    }

    public RequestBody getBody() {
        return body;
    }

    public boolean hasQueryString() {
        return line.hasQueryString();
    }

    public Map<String, String> getQueryString() {
        return line.getQueryString();
    }

    public String getPath() {
        return line.getRequestPath();
    }
}
