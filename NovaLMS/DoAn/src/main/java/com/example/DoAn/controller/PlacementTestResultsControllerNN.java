package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PlacementTestResultsControllerNN {

    /** Old standalone results → redirect to hybrid entry */
    @GetMapping("/placement-test/results/{resultId}")
    public String showResults(@PathVariable Integer resultId) {
        return "redirect:/hybrid-entry";
    }
}
