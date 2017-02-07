package me.vickychijwani.spectre.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import me.vickychijwani.spectre.model.entity.AuthToken;
import me.vickychijwani.spectre.model.entity.Role;
import me.vickychijwani.spectre.model.entity.User;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.network.entity.RevokeReqBody;
import me.vickychijwani.spectre.network.entity.UserList;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;
import rx.functions.Action1;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * PURPOSE: contract tests for the Ghost API
 * run these to detect breaking changes in the API when a new Ghost version comes out
 * these are integration-style tests that run against an actual Ghost instance
 * and verify whether the API matches the behaviour we expect
 */

// ignored by default since these tests require a running Ghost instance so they can't run in CI yet
// remove @Ignore and run this locally when needed
@Ignore
public final class GhostApiTest {

    // TODO set up a fixture with npm install ghost and some sql to make this portable
    private static final String TEST_USER = "vickychijwani@gmail.com";
    private static final String TEST_PWD = "ghosttest";

    private SynchronousGhostApiService api;

    @Before
    public void setupApiService() {
        Gson gson = GhostApiUtils.getGson();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://localhost:2368/ghost/api/v0.1")
                .setConverter(new GsonConverter(gson))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();
        api = restAdapter.create(SynchronousGhostApiService.class);
    }

    @After
    public void destroyApiService() {
        api = null;
    }

    @Test
    public void test_getClientSecret() {
        String clientSecret = getClientSecret(api);

        // must NOT be null since that's only possible with a very old Ghost version (< 0.7.x)
        assertThat(clientSecret, notNullValue());
        // Ghost uses a 12-character client secret, evident from the Ghost source code (1 byte can hold 2 hex chars):
        // { secret: crypto.randomBytes(6).toString('hex') }
        // file: core/server/data/migration/fixtures/004/04-update-ghost-admin-client.js
        assertThat(clientSecret.length(), is(12));
    }

    @Test
    public void test_getAuthToken() {
        doWithAuthToken(api, authToken -> {
            assertThat(authToken.getTokenType(), is("Bearer"));
            assertThat(authToken.getAccessToken(), notNullValue());
            assertThat(authToken.getRefreshToken(), notNullValue());
            //assertThat(authToken.getExpiresIn(), instanceOf(Integer.class)); // no-op, int can't be null
        });
    }

    @Test
    public void test_revokeAuthToken() {
        String clientSecret = getClientSecret(api);
        AuthReqBody credentials = new AuthReqBody(TEST_USER, TEST_PWD, clientSecret);
        AuthToken authToken = api.getAuthToken(credentials);

        RevokeReqBody[] revokeReqs = RevokeReqBody.from(authToken, clientSecret);
        for (RevokeReqBody reqBody : revokeReqs) {
            JsonElement jsonResponse = api.revokeAuthToken(authToken.getAuthHeader(), reqBody);

            JsonObject jsonObj = jsonResponse.getAsJsonObject();
            assertThat(jsonObj.has("error"), is(false));
            assertThat(jsonObj.get("token").getAsString(), is(reqBody.token));
        }
    }

    @Test
    public void test_getCurrentUser() {
        doWithAuthToken(api, authToken -> {
            UserList users = api.getCurrentUser(authToken.getAuthHeader(), "");
            User user = users.users.get(0);

            assertThat(user, notNullValue());
            //assertThat(user.getId(), instanceOf(Integer.class)); // no-op, int can't be null
            assertThat(user.getUuid(), notNullValue());
            assertThat(user.getName(), notNullValue());
            assertThat(user.getSlug(), notNullValue());
            assertThat(user.getEmail(), is(TEST_USER));
            //assertThat(user.getImage(), anyOf(nullValue(), notNullValue())); // no-op
            //assertThat(user.getBio(), anyOf(nullValue(), notNullValue())); // no-op
            assertThat(user.getRoles(), not(empty()));

            Role role = user.getRoles().first();
            //assertThat(role.getId(), instanceOf(Integer.class)); // no-op, int can't be null
            assertThat(role.getUuid(), notNullValue());
            assertThat(role.getName(), notNullValue());
            assertThat(role.getDescription(), notNullValue());
        });
    }



    // private helpers
    private static void doWithAuthToken(SynchronousGhostApiService api,
                                        Action1<AuthToken> callback) {
        String clientSecret = getClientSecret(api);
        AuthReqBody credentials = new AuthReqBody(TEST_USER, TEST_PWD, clientSecret);
        AuthToken authToken = api.getAuthToken(credentials);
        callback.call(authToken);
        RevokeReqBody[] revokeReqs = RevokeReqBody.from(authToken, clientSecret);
        for (RevokeReqBody reqBody : revokeReqs) {
            api.revokeAuthToken(authToken.getAuthHeader(), reqBody);
        }
    }

    private static String getClientSecret(SynchronousGhostApiService api) {
        Response response = api.getLoginPage();
        // TODO refactor to use GhostApiUtils.doWithClientSecret once we move to Retrofit2
        String html = new String(((TypedByteArray) response.getBody()).getBytes());
        return GhostApiUtils.extractClientSecretFromHtml(html);
    }

}
