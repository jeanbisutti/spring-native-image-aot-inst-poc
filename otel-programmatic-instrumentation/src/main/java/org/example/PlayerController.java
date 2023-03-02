package org.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PlayerController {

    private final PlayerRepository playerRepository;

    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping("/")
    List<Player> url() {
        return playerRepository.findAll();
    }

    @GetMapping("/exception")
    String exception() {
        throw new RuntimeException();
    }


}
