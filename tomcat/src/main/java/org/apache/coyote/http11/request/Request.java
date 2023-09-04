package org.apache.coyote.http11.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.coyote.http11.common.HttpVersion;
import org.apache.coyote.http11.cookie.Cookie;

import nextstep.jwp.model.User;

public class Request {
    private static final String CONTENT_LENGTH = "Content-Length";
    private final RequestLine line;
    private final RequestHeader header;
    private final RequestBody body;
    private final Cookie cookie;

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
            return new Request(requestLine, requestHeader, RequestBody.EMPTY_REQUEST_BODY, cookie);
        }
        return new Request(requestLine, requestHeader, RequestBody.convert(bufferedReader, Integer.parseInt(contentLength)),
                cookie);
    }

    public boolean hasQueryString() {
        return line.hasQueryString();
    }

    public Map<String, String> getQueryString() {
        return line.getQueryString();
    }

    public User parseToUser() {
        return body.parseToUser();
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
}
