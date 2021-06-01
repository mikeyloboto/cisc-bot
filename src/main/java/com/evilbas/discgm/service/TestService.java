package com.evilbas.discgm.service;

import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class TestService {
    public Integer gamble() {
        return new Random().nextInt(100);
    }
}
