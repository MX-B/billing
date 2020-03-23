package io.gr1d.billing;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.*;
import io.gr1d.billing.api.recipients.Recipient;
import io.gr1d.billing.api.subscriptions.Api;
import io.gr1d.billing.api.subscriptions.Provider;
import io.gr1d.billing.api.subscriptions.Tenant;
import lombok.Value;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;

public enum TestUtils {
    ;

    private static final Gson gsonSnakeCase = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) ->
                    LocalDateTime.parse(json.getAsJsonPrimitive().getAsString()))
            .registerTypeAdapter(LocalDate.class, (JsonSerializer) (src, typeOfSrc, context) ->
                    new JsonPrimitive(((LocalDate) src).format(DateTimeFormatter.ISO_DATE)))
            .create();

    private static final Gson gsonCammelCase = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) ->
                    LocalDateTime.parse(json.getAsJsonPrimitive().getAsString()))
            .create();

    public static String json(final Object object) {
        return gsonSnakeCase.toJson(object);
    }

    public static String jsonCammelCase(final Object object) {
        return gsonCammelCase.toJson(object);
    }

    public static <T> T getResult(final ResultActions resultActions, Class<T> clazz) {
        final byte[] byteArray = resultActions.andReturn().getResponse().getContentAsByteArray();
        return gsonSnakeCase.fromJson(new InputStreamReader(new ByteArrayInputStream(byteArray)), clazz);
    }

    @Value
    private static class MockAccessToken {
        private String accessToken;
        private int expiresIn;
        private long createdTimestamp;
    }

    public static void createStubAuthentication() {
        final String token = jsonCammelCase(new MockAccessToken("1h98dh89d12", 100000000, Instant.now().toEpochMilli()));
        stubFor(WireMock.post(urlEqualTo("/realms/realm/protocol/openid-connect/token"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(token)));
    }

    public static Api configureStubForApi(final String apiUuid, final String apiName, final String externalId) {
        return TestUtils.configureStubForApi(new Api(apiUuid, apiName, externalId));
    }

    public static Provider configureStubForProvider(final String providerUuid) {
        final Recipient recipient = new Recipient("66072095000167", "CNPF", "Bank", "222", "4234", new HashMap<>());
        return TestUtils.configureStubForProvider(new Provider(providerUuid, "Provider Name Over here", "a-random-wallet-id", "123453", "provider@email.com", recipient));
    }

    public static Tenant configureStubForTenant(final String realm, final String name) {
        return configureStubForTenant(realm, name, UUID.randomUUID().toString());
    }

    public static Tenant configureStubForTenant(final String realm, final String name, final String walletId) {
        final Tenant tenant = new Tenant();

        tenant.setUuid(UUID.randomUUID().toString());
        tenant.setEmail("random@email.com");
        tenant.setSupportEmail("support@email.com");
        tenant.setLogo("https://random.com/logo.com");
        tenant.setRealm(realm);
        tenant.setName(name);
        tenant.setUuid("https://random.com");
        tenant.setWalletId(walletId);

        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo(String.format("/tenant/by-realm/%s", realm)))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(tenant))));

        return tenant;
    }

    public static Api configureStubForApi(final Api api) {
        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo(String.format("/api/%s", api.getUuid())))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(api))));
        return api;
    }

    public static Provider configureStubForProvider(final Provider provider) {
        createStubAuthentication();
        stubFor(WireMock.get(urlEqualTo(String.format("/provider/%s", provider.getUuid())))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json(provider))));

        return provider;
    }

    public static Clock createClock(final int year, final int month, final int day) {
        final Clock clock = Mockito.mock(Clock.class);
        when(clock.instant()).thenReturn(instant(year, month, day));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        return clock;
    }

    public static Clock createClock(final LocalDate localDate) {
        final Clock clock = Mockito.mock(Clock.class);
        when(clock.instant()).thenReturn(localDate.atStartOfDay().toInstant(ZoneOffset.UTC));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        return clock;
    }

    private static Instant instant(final int year, final int month, final int day) {
        return LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
