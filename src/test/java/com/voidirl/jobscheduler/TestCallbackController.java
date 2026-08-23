package com.voidirl.jobscheduler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/test-callback")
public class TestCallbackController {

    public enum Mode { OK, FAIL }

    public static final AtomicInteger HITS = new AtomicInteger();
    public static final AtomicReference<Mode> MODE = new AtomicReference<>(Mode.OK);

    @PostMapping
    public ResponseEntity<Void> receive() {
        HITS.incrementAndGet();
        return MODE.get() == Mode.FAIL ? ResponseEntity.status(500).build() : ResponseEntity.ok().build();
    }
}
