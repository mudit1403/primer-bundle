/*
 * Copyright 2016 Phaneesh Nagaraja <phaneesh.n@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dropwizard.primer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.dropwizard.primer.auth.AuthType;
import io.dropwizard.primer.auth.PrimerAuthorizationRegistry;
import io.dropwizard.primer.core.VerifyResponse;
import io.dropwizard.primer.exception.PrimerException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

/**
 * @author phaneesh
 */
public class PrimerAuthorizationsTest extends BaseTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9999);

    @Test
    public void testWhiteListPresent() {
        assertTrue(PrimerAuthorizationRegistry.isWhilisted("simple/noauth/test"));
    }

    @Test
    public void testWhiteListAbsent() {
        assertFalse(PrimerAuthorizationRegistry.isWhilisted("simple/auth/test"));
    }

    @Test
    public void testAuthorizedCall() throws PrimerException, JsonProcessingException, ExecutionException {
            stubFor(post(urlEqualTo("/v1/verify/test/test"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(mapper.writeValueAsBytes(VerifyResponse.builder()
                                    .expiresAt(Instant.now().plusSeconds(10000).toEpochMilli())
                                    .token(token)
                                    .userId("test")
                                    .build()))));
        assertNotNull(PrimerAuthorizationRegistry.authorize("simple/auth/test", "GET", token, AuthType.CONFIG));
    }

    @Test
    public void testAnnotatedAuthorizedCall() throws JsonProcessingException {
        stubFor(post(urlEqualTo("/v1/verify/test/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsBytes(VerifyResponse.builder()
                                .expiresAt(Instant.now().plusSeconds(10000).toEpochMilli())
                                .token(token)
                                .userId("test")
                                .build()))));
        assertNotNull(PrimerAuthorizationRegistry.authorize("simple/auth/test", "GET", token, AuthType.ANNOTATION));
    }

    @Test
    public void testUnAuthorizedCallWithInvalidRole() {
        try {
            PrimerAuthorizationRegistry.authorize("simple/auth/test", "GET", buildTokenWithInvalidRole(), AuthType.CONFIG);
            fail("Should have failed!!");
        } catch (Exception e) {
            assertTrue(validateException(e));
        }
    }

    @Test
    public void testAnnotatedUnAuthorizedCall() throws PrimerException, ExecutionException {
        try {
            PrimerAuthorizationRegistry.authorize("simple/auth/test", "GET", token, AuthType.ANNOTATION);
            fail("Should have failed!!");
        } catch (Exception e) {
            assertTrue(validateException(e));
        }
    }

    @Test
    public void testUnAuthorizedCallWithInvalidMethod() throws PrimerException, ExecutionException {
        try {
        PrimerAuthorizationRegistry.authorize("simple/auth/test", "POST", token, AuthType.CONFIG);
        fail("Should have failed!!");
    } catch (Exception e) {
        assertTrue(validateException(e));
    }

}

    @Test
    public void testUnAuthorizedCallWithInvalidPath() throws PrimerException, ExecutionException {
        try {
            PrimerAuthorizationRegistry.authorize("simple/auth/test/invalid", "GET", token, AuthType.CONFIG);
            fail("Should have failed!!");
        } catch (Exception e) {
            assertTrue(validateException(e));
        }
    }

    @Test
    public void testUnAuthorizedCallWithInvalidRoleAndMethod() {
        try {
            PrimerAuthorizationRegistry.authorize("simple/auth/test", "POST", buildTokenWithInvalidRole(), AuthType.CONFIG);
            fail("Should have failed!!");
        } catch (Exception e) {
            assertTrue(validateException(e));
        }
    }

    private String buildTokenWithInvalidRole() throws JoseException {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("user_id" , "test");
        claimsMap.put("role" , "test_invalid");
        claimsMap.put("name", "test");
        claimsMap.put("type", "dynamic");
        NumericDate expiryDate = NumericDate.now();
        expiryDate.addSeconds(TimeUnit.SECONDS.convert(365, TimeUnit.DAYS));
        return generate(primerBundleConfiguration.getPrivateKey(), "test", "test", claimsMap, expiryDate);
    }

    private boolean validateException(Throwable e) {
        boolean exception = e instanceof PrimerException;
        if(e.getCause() instanceof PrimerException) {
            exception = true;
        } else if(e.getCause() instanceof CompletionException) {
            if(e.getCause().getCause() instanceof PrimerException) {
                exception = true;
            }
        }
        return exception;
    }
}
