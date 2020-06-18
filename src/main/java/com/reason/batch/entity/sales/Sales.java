package com.reason.batch.entity.sales;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
public class Sales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate orderDate;
    private long amount;
    private String orderNo;

    @Builder
    public Sales(LocalDate orderDate, long amount, String orderNo) {
        this.orderDate = orderDate;
        this.amount = amount;
        this.orderNo = orderNo;
    }
}
