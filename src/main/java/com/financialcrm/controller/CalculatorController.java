package com.financialcrm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

@Controller
@RequestMapping("/calculator")
public class CalculatorController {

    @GetMapping
    public String calculatorPage() {
        return "calculator/index";
    }

    /** EMI Calculator: EMI = [P x R x (1+R)^N] / [(1+R)^N - 1] */
    @PostMapping("/emi")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calculateEmi(
            @RequestParam double principal,
            @RequestParam double annualRate,
            @RequestParam int tenureMonths) {

        double monthlyRate = annualRate / (12 * 100);
        double emi;
        if (monthlyRate == 0) {
            emi = principal / tenureMonths;
        } else {
            double factor = Math.pow(1 + monthlyRate, tenureMonths);
            emi = (principal * monthlyRate * factor) / (factor - 1);
        }
        double totalPayment = emi * tenureMonths;
        double totalInterest = totalPayment - principal;

        return ResponseEntity.ok(Map.of(
            "emi", round(emi),
            "totalPayment", round(totalPayment),
            "totalInterest", round(totalInterest),
            "principal", principal
        ));
    }

    /** SIP Calculator: FV = P x [((1+r)^n - 1) / r] x (1+r) */
    @PostMapping("/sip")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calculateSip(
            @RequestParam double monthlyInvestment,
            @RequestParam double annualReturn,
            @RequestParam int tenureYears) {

        double r = annualReturn / (12 * 100);
        int n = tenureYears * 12;
        double fv;
        // FIX: when annualReturn is 0, r is 0 and the formula divides by r,
        // producing NaN. round(NaN) then threw NumberFormatException and the
        // endpoint returned HTTP 500 ("Calculation failed" alert in the UI).
        // A 0% return is a perfectly valid input (e.g. comparing against a
        // savings-only scenario), so it's handled as a straight sum instead.
        if (r == 0) {
            fv = monthlyInvestment * n;
        } else {
            fv = monthlyInvestment * (((Math.pow(1 + r, n) - 1) / r) * (1 + r));
        }
        double invested = monthlyInvestment * n;
        double returns = fv - invested;

        return ResponseEntity.ok(Map.of(
            "futureValue", round(fv),
            "totalInvested", round(invested),
            "estimatedReturns", round(returns),
            "wealthGainRatio", round(invested == 0 ? 0 : (returns / invested) * 100)
        ));
    }

    /** SWP Calculator: Calculates how long corpus lasts with monthly withdrawal */
    @PostMapping("/swp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calculateSwp(
            @RequestParam double corpus,
            @RequestParam double monthlyWithdrawal,
            @RequestParam double annualReturn) {

        double r = annualReturn / (12 * 100);
        double balance = corpus;
        int months = 0;
        while (balance > 0 && months < 600) {
            balance = balance * (1 + r) - monthlyWithdrawal;
            months++;
        }
        double totalWithdrawn = monthlyWithdrawal * months;

        return ResponseEntity.ok(Map.of(
            "sustainableMonths", months,
            "sustainableYears", months / 12.0,
            "totalWithdrawn", round(totalWithdrawn),
            "corpusExhausted", balance <= 0
        ));
    }

    /** Insurance Premium (simple term plan approximation) */
    @PostMapping("/insurance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calculateInsurance(
            @RequestParam double sumAssured,
            @RequestParam int age,
            @RequestParam int tenureYears,
            @RequestParam String type) {

        double basePremium;
        double ratePerLakh;

        switch (type.toUpperCase()) {
            case "TERM" -> ratePerLakh = 100 + (age * 10);
            case "HEALTH" -> ratePerLakh = 500 + (age * 20);
            case "VEHICLE" -> ratePerLakh = 200;
            default -> ratePerLakh = 150;
        }

        basePremium = (sumAssured / 100000) * ratePerLakh;
        double gst = basePremium * 0.18;
        double totalPremium = basePremium + gst;
        double annualPremium = totalPremium;
        double monthlyPremium = totalPremium / 12;

        return ResponseEntity.ok(Map.of(
            "basePremium", round(basePremium),
            "gst", round(gst),
            "annualPremium", round(annualPremium),
            "monthlyPremium", round(monthlyPremium),
            "totalPremiumForTenure", round(annualPremium * tenureYears)
        ));
    }

    // FIX: BigDecimal.valueOf(NaN) / BigDecimal.valueOf(Infinity) throws
    // NumberFormatException, which surfaced to the user as "Calculation
    // failed: Server error: 500" — seemingly at random, since it only
    // happened for certain edge-case inputs (e.g. a 0% SIP return, or any
    // calculator field left blank so the browser sent "NaN" as the value).
    // Guarding here makes every calculator endpoint resilient regardless of
    // what produced the bad number upstream.
    private double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
