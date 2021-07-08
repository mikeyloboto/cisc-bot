package com.evilbas.discgm.client;

import com.evilbas.rslengine.creature.Encounter;
import com.evilbas.rslengine.networking.CombatResultWrapper;
import com.evilbas.rslengine.networking.InventoryActionRequest;
import com.evilbas.rslengine.networking.InventoryInteractionWrapper;
import com.evilbas.rslengine.networking.ResultWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ProcServerClient {

    private static final Logger log = LoggerFactory.getLogger(ProcServerClient.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${proc.server.address}")
    private String serverUri;

    public CombatResultWrapper retrieveEncounter(String characterGuid) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUri + "/combat").queryParam("guid",
                characterGuid);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<CombatResultWrapper> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,
                entity, CombatResultWrapper.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }
        return null;
    }

    public CombatResultWrapper performAttack(String characterGuid, Integer target, Integer spell) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUri + "/combat/attack")
                .queryParam("guid", characterGuid).queryParam("targetSlot", target).queryParam("spellSlot", spell);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<CombatResultWrapper> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET,
                entity, CombatResultWrapper.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }
        return null;
    }

    public InventoryInteractionWrapper listInventory(String characterGuid) {
        InventoryActionRequest request = new InventoryActionRequest();
        request.setGuid(characterGuid);
        return (InventoryInteractionWrapper) sendToServer("/inventory", request, HttpMethod.POST);
    }

    public InventoryInteractionWrapper useItem(String characterGuid, String item) {
        InventoryActionRequest request = new InventoryActionRequest();
        request.setGuid(characterGuid);
        request.setItem(item);
        return (InventoryInteractionWrapper) sendToServer("/inventory/use", request, HttpMethod.POST);
    }

    private ResultWrapper sendToServer(String endpoint, Object requestBody, HttpMethod method) {
        log.debug("Initiated backend call");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUri + endpoint);

        ResponseEntity<InventoryInteractionWrapper> response = null;

        switch (method) {
            case POST:
                response = restTemplate.postForEntity(builder.toUriString(), requestBody,
                        InventoryInteractionWrapper.class);
                break;
            case GET:
                break;
        }
        log.debug("Resp: {}", response);
        log.debug("Req: {}", builder.toUriString());
        if (response.getStatusCode() == HttpStatus.OK) {
            log.debug("Body: {}", response.getBody());
            return response.getBody();
        }
        return null;
    }
}
