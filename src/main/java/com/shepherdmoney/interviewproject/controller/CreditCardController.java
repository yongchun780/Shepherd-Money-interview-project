package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CardBalanceHistoryView;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<?> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        // Check if the user exists
        Optional<User> userOptional = userRepository.findById(payload.getUserId());
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        User user = userOptional.get();

        // Check if credit card number already exists
        if (creditCardRepository.existsByNumber(payload.getCardNumber())) {
            return ResponseEntity.badRequest().body("Credit card number already in use");
        }

        // Create a new credit card
        CreditCard newCard = new CreditCard();
        newCard.setIssuanceBank(payload.getCardIssuanceBank());
        newCard.setNumber(payload.getCardNumber());
        newCard.setUser(user);
        creditCardRepository.save(newCard);

        return ResponseEntity.ok(newCard.getId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<?> getAllCardOfUser(@RequestParam int userId) {
        // Check if the user exists
        boolean userExists = userRepository.existsById(userId);
        if (!userExists) {
            return ResponseEntity.badRequest().body("No user found with ID: " + userId);
        }

        List<CreditCard> cards = creditCardRepository.findByUserId(userId); // Add this method in CreditCardRepository
        List<CreditCardView> cardViews = cards.stream()
                .map(card -> new CreditCardView(card.getIssuanceBank(), card.getNumber()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(cardViews);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<?> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        // Use the repository to attempt to find the credit card by its number
        Optional<CreditCard> cardOpt = creditCardRepository.findByNumber(creditCardNumber);

        if (!cardOpt.isPresent()) {
            // If the card is not found, return a 400 Bad Request with a descriptive message
            return ResponseEntity.badRequest().body("Credit card not found");
        }

        // If the card is found, retrieve and return the user ID associated with this card
        CreditCard card = cardOpt.get();
        if (card.getUser() != null) {
            return ResponseEntity.ok(card.getUser().getId());
        } else {
            // Handle case where card exists but no user is associated with this card
            return ResponseEntity.badRequest().body("No user associated with this credit card");
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> updateBalanceHistory(@RequestBody UpdateBalancePayload[] payloads) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      This is because
        //      1. You would first populate 4/11 with previous day's balance (4/10), so {date: 4/11, amount: 100}
        //      2. And then you observe there is a +10 difference
        //      3. You propagate that +10 difference until today
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.

        if (payloads == null || payloads.length == 0) {
            return ResponseEntity.badRequest().body("No payloads provided.");
        }

        Arrays.sort(payloads, Comparator.comparing(UpdateBalancePayload::getBalanceDate));
        LocalDate today = LocalDate.now();

        for (UpdateBalancePayload payload : payloads) {
            Optional<CreditCard> cardOpt = creditCardRepository.findByNumber(payload.getCreditCardNumber());
            if (!cardOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Credit card not found for number: " + payload.getCreditCardNumber());
            }
            CreditCard card = cardOpt.get();
            TreeMap<LocalDate, Double> balanceMap = new TreeMap<>();

            // Load existing balances into a TreeMap
            card.getBalanceHistories().forEach(h -> balanceMap.put(h.getDate(), h.getBalance()));

            // First fill gaps up to the date before the new balance
            fillGaps(balanceMap);

            // Check if the date exists and calculate the difference
            double existingBalance = balanceMap.getOrDefault(payload.getBalanceDate(), balanceMap.lowerEntry(payload.getBalanceDate()) != null ? balanceMap.lowerEntry(payload.getBalanceDate()).getValue() : 0);
            double newBalance = payload.getBalanceAmount();
            double difference = Math.abs(newBalance - existingBalance); // Absolute difference

            // Put the new balance or calculate absolute difference if entry exists
            balanceMap.put(payload.getBalanceDate(), newBalance);

            // Propagate the difference if it's not zero and if there's an existing balance to adjust
            if (difference != 0) {
                propagateDifference(balanceMap, payload.getBalanceDate(), difference);
            }

            // Check if today's date already exists in the balance history
            if (!balanceMap.containsKey(today)) {
                ensureLastEntryMatchesToday(balanceMap);
            }

            // Sync back to the database
            syncBalancesToDatabase(card, balanceMap);
        }
        return ResponseEntity.ok("Balance histories updated successfully.");
    }

    private void fillGaps(TreeMap<LocalDate, Double> balanceMap) {
        LocalDate lastDate = null;
        double lastBalance = 0;
        List<LocalDate> keys = new ArrayList<>(balanceMap.keySet());
        for (LocalDate date : keys) {
            if (lastDate != null) {
                LocalDate nextDate = lastDate.plusDays(1);
                while (nextDate.isBefore(date)) {
                    balanceMap.putIfAbsent(nextDate, lastBalance);
                    nextDate = nextDate.plusDays(1);
                }
            }
            lastDate = date;
            lastBalance = balanceMap.get(date);
        }
    }

    private void propagateDifference(TreeMap<LocalDate, Double> balanceMap, LocalDate startDate, double difference) {
        balanceMap.tailMap(startDate, false).forEach((date, balance) -> balanceMap.put(date, balance + difference));
    }

    private void ensureLastEntryMatchesToday(TreeMap<LocalDate, Double> balanceMap) {
        LocalDate today = LocalDate.now();
        Map.Entry<LocalDate, Double> lastEntry = balanceMap.lastEntry();
        if (lastEntry != null && !lastEntry.getKey().equals(today)) {
            balanceMap.put(today, lastEntry.getValue());
        }
    }

    private void syncBalancesToDatabase(CreditCard card, TreeMap<LocalDate, Double> balanceMap) {
        card.getBalanceHistories().clear();
        balanceMap.forEach((date, balance) -> {
            BalanceHistory history = new BalanceHistory();
            history.setDate(date);
            history.setBalance(balance);
            history.setCreditCard(card);
            card.getBalanceHistories().add(history);
        });
        creditCardRepository.save(card);
    }

    // This is for debugging
    @GetMapping("/credit-card:balance-history")
    public ResponseEntity<?> getBalanceHistoryByCreditCardId(@RequestParam int creditCardId) {
        Optional<CreditCard> cardOpt = creditCardRepository.findById(creditCardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Credit card not found for number: " + creditCardId);
        }
        CreditCard card = cardOpt.get();
        List<BalanceHistory> histories = card.getBalanceHistories();
        List<CardBalanceHistoryView> historiesView = histories.stream()
                .map(history -> new CardBalanceHistoryView(history.getDate(), history.getBalance()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(historiesView);
    }
    // This is for debugging
    @PostMapping("/credit-card:add-balance")
    public ResponseEntity<String> addBalanceHistory(@RequestBody UpdateBalancePayload[] payloads) {
        if (payloads == null || payloads.length == 0) {
            return ResponseEntity.badRequest().body("No balance data provided.");
        }

        for (UpdateBalancePayload payload : payloads) {
            Optional<CreditCard> cardOpt = creditCardRepository.findByNumber(payload.getCreditCardNumber());
            if (!cardOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Credit card not found for number: " + payload.getCreditCardNumber());
            }

            CreditCard card = cardOpt.get();

            // Create new BalanceHistory object and set its values
            BalanceHistory newHistory = new BalanceHistory();
            newHistory.setCreditCard(card);
            newHistory.setDate(payload.getBalanceDate());
            newHistory.setBalance(payload.getBalanceAmount());

            // Add to the card's balance history list and save
            card.getBalanceHistories().add(newHistory);
            creditCardRepository.save(card);
        }

        return ResponseEntity.ok("Balance history added successfully.");
    }
}
