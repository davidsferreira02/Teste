package pt.psoft.g1.psoftg1.auth.service;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.CreateUserRequest;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;
import org.springframework.security.core.GrantedAuthority;


import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("google")
@Service("googleIAMService")
public class GoogleIAMService implements IAMService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final UserService userService;
    private final JwtEncoder jwtEncoder;
    public GoogleIAMService(UserService userService, JwtEncoder jwtEncoder) {
        this.userService = userService;
        this.jwtEncoder=jwtEncoder;
    }

    @Override
    public User authenticate(String authorizationCode) throws ParseException {

        BearerAccessToken accessToken = exchangeAuthorizationCodeForAccessToken(authorizationCode);
        User googleUser = fetchUserInfo(accessToken.getValue());
        CreateUserRequest createUserRequest= new CreateUserRequest(googleUser.getUsername(), googleUser.getName().toString(), googleUser.getPassword());
        createUserRequest.setRole("READER");
        return userService.findByUsername(googleUser.getUsername()).orElseGet(() ->   userService.create(createUserRequest));
    }

    private BearerAccessToken exchangeAuthorizationCodeForAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = String.format(
                "code=%s&client_id=%s&client_secret=%s&redirect_uri=%s&grant_type=authorization_code",
                URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8),
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
                URLEncoder.encode("http://localhost:8080/login/oauth2/code/google", StandardCharsets.UTF_8)
        );
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            URI uri = URI.create(tokenUri);
            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = JSONObjectUtils.parse(response.getBody());
                return new BearerAccessToken((String) responseBody.get("access_token"));
            } else {
                System.out.println("Erro na resposta da Google: " + response.getBody());
                throw new RuntimeException("Falha a obter o token de acesso do Google: " + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao comunicar com o Google: " + e.getMessage(), e);
        }
    }



        private User fetchUserInfo(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);


        ResponseEntity<Map> response = restTemplate.exchange(
                URI.create(userInfoUri),
                HttpMethod.GET,
                entity,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> userInfo = response.getBody();
            String email = (String) userInfo.get("email");
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Email não encontrado na resposta do Google.");
            }

            User googleUser = new User(email, "OAuthDummyPassword123");
            String name = (String) userInfo.get("name");
            if (name != null && !name.isEmpty()) {
                googleUser.setName(new Name(name).toString());
            }

            return googleUser;
        } else {
            throw new RuntimeException("Falha ao obter informações do utilizador do Google");
        }
    }

    public String generateJwtToken(User user) {
        final Instant now = Instant.now();
        final long expiry = 3600L;

        final String scope = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("yourapp.io")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(String.format("%s,%s", user.getId(), user.getUsername()))
                .claim("roles", scope)
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }



}
