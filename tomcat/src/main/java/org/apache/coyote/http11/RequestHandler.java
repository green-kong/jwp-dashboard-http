package org.apache.coyote.http11;

import static org.apache.coyote.http11.cookie.Cookie.KEY_VALUE_DELIMITER;
import static org.apache.coyote.http11.request.HttpMethod.GET;
import static org.apache.coyote.http11.request.HttpMethod.POST;
import static org.apache.coyote.http11.response.StatusCode.OK;
import static org.apache.coyote.http11.response.StatusCode.UNAUTHORIZED;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

import org.apache.coyote.http11.common.HttpVersion;
import org.apache.coyote.http11.common.Session;
import org.apache.coyote.http11.exception.LoginException;
import org.apache.coyote.http11.request.Request;
import org.apache.coyote.http11.response.ContentType;
import org.apache.coyote.http11.response.Response;
import org.apache.coyote.http11.response.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.model.User;

public class RequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
    private static final String JSESSIONID = "JSESSIONID";
    private static final String BASE_DIRECTORY = "static";
    private static final String LOGIN_HTML = "/login.html";
    private static final String INDEX_HTML = "/index.html";
    private static final String UNAUTHORIZED_HTML = "/401.html";
    private static final String NOT_FOUND_HTML = "/404.html";
    private static final String REGISTER_HTML = "/register.html";
    private static final String LOGIN = "/login";
    private static final String REGISTER = "/register";
    private final Request request;

    public RequestHandler(final Request request) {
        this.request = request;
    }

    public Response generateResponse() {
        final String requestPath = request.getLine().getRequestPath();
        if (requestPath.equals("/") && request.getHttpMethod().equals(GET)) {
            return getMainPage();
        }

        if (requestPath.equals(LOGIN) && request.getHttpMethod().equals(GET)) {
            return handleLoginPage();
        }

        if (requestPath.equals(LOGIN) && request.getHttpMethod().equals(POST)) {
            return handleLoginPostRequest();
        }

        if (requestPath.equals(REGISTER) && request.getHttpMethod().equals(GET)) {
            return getStaticPateResponse(REGISTER_HTML, OK);
        }

        if (requestPath.equals(REGISTER) && request.getHttpMethod().equals(POST)) {
            return register();
        }

        return getStaticPateResponse(requestPath, OK);
    }

    private Response handleLoginPage() {
        final Session session = request.getSession(false);
        if (Objects.isNull(session)) {
            return getStaticPateResponse(LOGIN_HTML, OK);
        }
        final User user = (User) session.get("user");
        LOGGER.info("user {}", user);
        return Response.generateRedirectResponse(INDEX_HTML);
    }

    private Response getStaticPateResponse(final String requestPath, final StatusCode statusCode) {
        try {
            final String responseBody = readFile(requestPath);
            final HttpVersion httpVersion = request.getHttpVersion();
            final ContentType contentType = ContentType.findByName(requestPath);
            return Response.of(httpVersion, statusCode, contentType, responseBody);
        } catch (IOException exception) {
            return getStaticPateResponse(NOT_FOUND_HTML, OK);
        }
    }

    private String readFile(final String fileName) throws IOException {
        try {
            final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            URL url = classLoader.getResource(BASE_DIRECTORY + fileName);
            final File file = new File(url.getFile());
            return new String(Files.readAllBytes(file.toPath()));
        } catch (NullPointerException exception) {
            throw new IOException();
        }
    }

    private Response getMainPage() {
        final String responseBody = "Hello world!";
        final HttpVersion httpVersion = request.getHttpVersion();
        final ContentType contentType = ContentType.HTML;
        return Response.of(httpVersion, OK, contentType, responseBody);
    }

    private Response handleLoginPostRequest() {
        final Map<String, String> bodies = request.getBody().getBodies();
        final String account = bodies.get("account");
        final String password = bodies.get("password");
        return login(account, password);
    }

    private Response login(final String account, final String password) {
        try {
            final User user = InMemoryUserRepository.findByAccount(account)
                    .orElseThrow(() -> new LoginException("로그인에 실패했습니다."));

            if (user.checkPassword(password)) {
                return successLogin(user);
            }
            throw new LoginException("로그인에 실패했습니다.");
        } catch (LoginException exception) {
            return getStaticPateResponse(UNAUTHORIZED_HTML, UNAUTHORIZED);
        }
    }

    private Response successLogin(final User user) {
        LOGGER.info("user {}", user);
        final Response response = Response.generateRedirectResponse(INDEX_HTML);
        Session session = request.getSession(true);
        session.put("user", user);
        response.setCookie(String.join(KEY_VALUE_DELIMITER, JSESSIONID, session.getId()));
        return response;
    }

    private Response register() {
        final User user = request.parseToUser();
        InMemoryUserRepository.save(user);
        return Response.generateRedirectResponse(INDEX_HTML);
    }
}
