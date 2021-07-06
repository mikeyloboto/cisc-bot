package com.evilbas.discgm.client;

import com.evilbas.rslengine.creature.Encounter;
import com.evilbas.rslengine.networking.CombatResultWrapper;
import com.evilbas.rslengine.networking.InventoryInteractionWrapper;

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
        log.info("List inventory request");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUri + "/inventory").queryParam("guid",
                characterGuid);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<InventoryInteractionWrapper> response = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, entity, InventoryInteractionWrapper.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }
        return null;
    }

    public InventoryInteractionWrapper useItem(String characterGuid, String item) {
        log.info("use item request");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUri + "/inventory/use")
                .queryParam("guid", characterGuid).queryParam("item", item);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<InventoryInteractionWrapper> response = restTemplate.exchange(builder.build().toUriString(),
                HttpMethod.GET, entity, InventoryInteractionWrapper.class);
        log.info("Resp: {}", response);
        log.info("Req: {}", builder.toUriString());
        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Body: {}", response.getBody().getInventory());
            return response.getBody();
        }
        return null;
    }
}
