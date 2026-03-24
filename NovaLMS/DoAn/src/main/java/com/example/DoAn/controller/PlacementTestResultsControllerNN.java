package com.example.DoAn.controller;

import com.example.DoAn.dto.response.PlacementTestResultDTO;
import com.example.DoAn.service.PlacementTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PlacementTestResultsControllerNN {

    private final PlacementTestService placementTestService;

    @GetMapping("/placement-test/results/{resultId}")
    public String showResults(@PathVariable Integer resultId, Model model) {
        try {
            PlacementTestResultDTO result = placementTestService.getPlacementTestResult(resultId);
            model.addAttribute("result", result);
            return "public/placement-test-results";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/"; // Redirect to home on error
        }
    }
}
