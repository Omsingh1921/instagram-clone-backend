package com.instagramclone.security;

import com.instagramclone.ratelimit.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private RateLimitProperties properties;
    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setTrustedProxies(List.of("127.0.0.1", "::1", "10.0.0.1", "172.16.0.0/12"));
        resolver = new ClientIpResolver(properties);
    }

    @Test
    @DisplayName("Untrusted Peer - Direct client cannot spoof IP via X-Forwarded-For")
    void untrustedPeer_ShouldIgnoreXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.50"); // Public untrusted IP
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("Untrusted Peer - Direct client cannot spoof IP via X-Real-IP")
    void untrustedPeer_ShouldIgnoreXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.99");
        request.addHeader("X-Real-IP", "10.0.0.5");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("198.51.100.99");
    }

    @Test
    @DisplayName("Trusted Proxy - Correctly extracts client IP from single-hop X-Forwarded-For")
    void trustedProxy_SingleHop_ShouldExtractClientIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // Configured trusted proxy
        request.addHeader("X-Forwarded-For", "203.0.113.19");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("203.0.113.19");
    }

    @Test
    @DisplayName("Trusted Proxy - Correctly traverses multi-hop chain from right to left")
    void trustedProxy_MultiHopChain_ShouldStopAtFirstUntrustedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // Trusted immediate proxy
        // Chain: Client (198.51.100.7) -> Trusted intermediate (172.16.0.5)
        request.addHeader("X-Forwarded-For", "1.1.1.1, 198.51.100.7, 172.16.0.5");

        String resolvedIp = resolver.resolveClientIp(request);
        // 172.16.0.5 is trusted via CIDR 172.16.0.0/12; stops at 198.51.100.7; ignores spoofed 1.1.1.1
        assertThat(resolvedIp).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("Trusted Proxy - Falls back to X-Real-IP when X-Forwarded-For is absent")
    void trustedProxy_ShouldFallbackToXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.25");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("203.0.113.25");
    }

    @Test
    @DisplayName("Malformed Headers - Safely rejects invalid IP formats in forwarding headers")
    void malformedForwardingHeader_ShouldSkipInvalidEntries() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "invalid_ip, <script>alert(1)</script>, 203.0.113.88");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("203.0.113.88");
    }

    @Test
    @DisplayName("Malformed Headers - Falls back to remoteAddr if entire chain is malformed")
    void entirelyMalformedChain_ShouldFallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "bad_entry, another_garbage");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("CIDR Evaluation - Accurately matches subnet ranges")
    void cidrSubnetEvaluation() {
        assertThat(resolver.isTrustedProxy("172.16.0.1")).isTrue();
        assertThat(resolver.isTrustedProxy("172.31.255.254")).isTrue();
        assertThat(resolver.isTrustedProxy("172.32.0.1")).isFalse();
        assertThat(resolver.isTrustedProxy("10.0.0.1")).isTrue();
        assertThat(resolver.isTrustedProxy("192.168.1.1")).isFalse();
    }

    @Test
    @DisplayName("IPv6 Normalization - Normalizes loopback variants")
    void ipv6LoopbackNormalization() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        String resolvedIp = resolver.resolveClientIp(request);
        assertThat(resolvedIp).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("Null and Empty Requests - Return fallback 127.0.0.1 without throwing")
    void nullAndEmptyRequests_ShouldReturnDefaultFallback() {
        assertThat(resolver.resolveClientIp(null)).isEqualTo("127.0.0.1");

        MockHttpServletRequest emptyRequest = new MockHttpServletRequest();
        emptyRequest.setRemoteAddr(null);
        assertThat(resolver.resolveClientIp(emptyRequest)).isEqualTo("127.0.0.1");
    }
}
