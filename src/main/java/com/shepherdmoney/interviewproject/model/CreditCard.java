package com.shepherdmoney.interviewproject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.*;


@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String issuanceBank;

    private String number;

    // TODO: Credit card's owner. For detailed hint, please see User class
    // Some field here <> owner;

    // Credit card's owner. Establish a ManyToOne relationship to the User.
    @ManyToOne
    @JoinColumn(name = "user_id") // This will create a column 'user_id' in the CreditCard table to store the ID of the User
    private User user; // Link back to the User entity


    // TODO: Credit card's balance history. It is a requirement that the dates in the balanceHistory 
    //       list must be in chronological order, with the most recent date appearing first in the list. 
    //       Additionally, the last object in the "list" must have a date value that matches today's date, 
    //       since it represents the current balance of the credit card. For example:
    //       [
    //         {date: '2023-04-10', balance: 800},
    //         {date: '2023-04-11', balance: 1000},
    //         {date: '2023-04-12', balance: 1200},
    //         {date: '2023-04-13', balance: 1100},
    //         {date: '2023-04-16', balance: 900},
    //       ]
    // ADDITIONAL NOTE: For the balance history, you can use any data structure that you think is appropriate.
    //        It can be a list, array, map, pq, anything. However, there are some suggestions:
    //        1. Retrieval of a balance of a single day should be fast
    //        2. Traversal of the entire balance history should be fast
    //        3. Insertion of a new balance should be fast
    //        4. Deletion of a balance should be fast
    //        5. It is possible that there are gaps in between dates (note the 04-13 and 04-16)
    //        6. In the condition that there are gaps, retrieval of "closest **previous**" balance date should also be fast. Aka, given 4-15, return 4-13 entry tuple


    @OneToMany(mappedBy = "creditCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("date DESC")
    private List<BalanceHistory> balanceHistories = new ArrayList<>();

    @Transient
    private NavigableMap<LocalDate, Double> balanceMap = new TreeMap<>(Comparator.reverseOrder());

    @PostLoad
    private void loadBalanceMap() {
        balanceMap.clear();
        balanceHistories.forEach(h -> balanceMap.put(h.getDate(), h.getBalance()));
    }

    /*
     * Below are some potential methods associated with the ADDITIONAL NOTE above.
     * They may be used for testing or future development,
     * but are not used at the current stage.
     */
    
    public Double getBalanceOn(LocalDate date) {
        return balanceMap.get(date);
    }

    public void insertOrUpdateBalance(LocalDate date, Double balance) {
        balanceMap.put(date, balance);
        syncBalanceHistoriesFromMap();
    }

    public void deleteBalance(LocalDate date) {
        if (balanceMap.containsKey(date)) {
            balanceMap.remove(date);
            syncBalanceHistoriesFromMap();
        }
    }

    public Double getClosestPreviousBalance(LocalDate date) {
        return balanceMap.headMap(date, false).firstEntry().getValue();
    }

    public Map<LocalDate, Double> getBalances() {
        return new TreeMap<>(balanceMap);
    }

    private void syncBalanceHistoriesFromMap() {
        balanceHistories.clear();
        balanceMap.forEach((date, balance) -> {
            BalanceHistory history = new BalanceHistory();
            history.setDate(date);
            history.setBalance(balance);
            history.setCreditCard(this);
            balanceHistories.add(history);
        });
    }
}
