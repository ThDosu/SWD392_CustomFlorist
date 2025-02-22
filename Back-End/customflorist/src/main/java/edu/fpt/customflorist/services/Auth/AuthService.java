package edu.fpt.customflorist.services.Auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AuthService implements IAuthService{
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${spring.security.oauth2.client.registration.google.user-info-uri}")
    private String googleUserInfoUri;

    public String generateAuthUrl(String loginType) {
        String url = "";
        loginType = loginType.trim().toLowerCase(); // Normalize the login type

        if ("google".equals(loginType)) {
            GoogleAuthorizationCodeRequestUrl urlBuilder = new GoogleAuthorizationCodeRequestUrl(
                    googleClientId,
                    googleRedirectUri,
                    Arrays.asList("email", "profile", "openid"));
            url = urlBuilder.build();
        }

        return url;
    }
    public Map<String, Object> authenticateAndFetchProfile(String code, String loginType) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        String accessToken;
        //Gson gson = new Gson();

        try {
            accessToken = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(), new GsonFactory(),
                    googleClientId,
                    googleClientSecret,
                    code,
                    googleRedirectUri
            ).execute().getAccessToken();

            // Configure RestTemplate to include the access token in the Authorization header
            restTemplate.getInterceptors().add((req, body, executionContext) -> {
                req.getHeaders().set("Authorization", "Bearer " + accessToken);
                return executionContext.execute(req, body);
            });

            // Make a GET request to fetch user information
            return new ObjectMapper().readValue(
                    restTemplate.getForEntity(googleUserInfoUri, String.class).getBody(),
                    new TypeReference<>() {});
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
