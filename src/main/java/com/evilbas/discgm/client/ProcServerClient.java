package com.evilbas.discgm.client;

import com.evilbas.rslengine.creature.Encounter;
import com.evilbas.rslengine.networking.CombatResultWrapper;
import com.evilbas.rslengine.networking.InventoryActionRequest;
import com.evilbas.rslengine.networking.InventoryInteractionWrapper;
import com.evilbas.rslengine.networking.ResultWrapper;
import com.evilbas.rslengine.networking.SpellbookActionRequest;
import com.evilbas.rslengine.networking.SpellbookInteractionWrapper;

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
        return (InventoryInteractionWrapper) sendItemToServer("/inventory", request, HttpMethod.POST);
    }

    public SpellbookInteractionWrapper listSpellbook(String characterGuid) {
        SpellbookActionRequest request = new SpellbookActionRequest();
        request.setGuid(characterGuid);
        return (SpellbookInteractionWrapper) sendSpellToServer("/spellbook", request, HttpMethod.POST);
    }

    public InventoryInteractionWrapper useItem(String characterGuid, String item) {
        InventoryActionRequest request = new InventoryActionRequest();
        request.setGuid(characterGuid);
        request.setItem(item);
        return (InventoryInteractionWrapper) sendItemToServer("/inventory/use", request, HttpMethod.POST);
    }

    public SpellbookInteractionWrapper useSpell(String characterGuid, String spell) {
        SpellbookActionRequest request = new SpellbookActionRequest();
        request.setGuid(characterGuid);
        request.setSpell(spell);
        return (SpellbookInteractionWrapper) sendSpellToServer("/spellbook/use", request, HttpMethod.POST);
    }

    private InventoryInteractionWrapper sendItemToServer(String endpoint, Object requestBody, HttpMethod method) {
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
        log.info("Resp: {}", response);
        log.info("Req: {}", builder.toUriString());
        if (response.getStatusCode() == HttpStatus.OK) {
            log.debug("Body: {}", response.getBody());
            return response.getBody();
        }
        return null;
    }

    private SpellbookInteractionWrapper sendSpellToServer(String endpoint, Object requestBody, HttpMethod method) {
        log.debug("Initiated backend call");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUri + endpoint);

        ResponseEntity<SpellbookInteractionWrapper> response = null;

        switch (method) {
            case POST:
                response = restTemplate.postForEntity(builder.toUriString(), requestBody,
                        SpellbookInteractionWrapper.class);
                break;
            case GET:
                break;
        }
        log.info("Resp: {}", response.getBody().toString());
        log.info("Req: {}", builder.toUriString());
        if (response.getStatusCode() == HttpStatus.OK) {
            log.debug("Body: {}", response.getBody());
            return response.getBody();
        }
        return null;
    }
}
