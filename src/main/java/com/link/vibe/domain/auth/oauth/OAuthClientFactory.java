package com.link.vibe.domain.auth.oauth;

import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthClientFactory {

    private final Map<String, OAuthClient> clients;

    public OAuthClientFactory(List<OAuthClient> oAuthClients) {
        this.clients = oAuthClients.stream()
                .collect(Collectors.toMap(
                        client -> client.getProvider().toUpperCase(),
                        Function.identity()
                ));
    }

    public OAuthClient getClient(String provider) {
        OAuthClient client = clients.get(provider.toUpperCase());
        if (client == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        }
        return client;
    }
}
