package io.ruleforge.analytics.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/analytics")
public class AnalyticsController {

    @GetMapping("/decisions")
    public void queryDecisions() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @GetMapping("/decisions/aggregate")
    public void aggregateDecisions() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    @GetMapping("/rules/{id}/stats")
    public void ruleStats(@PathVariable String id) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }
}
