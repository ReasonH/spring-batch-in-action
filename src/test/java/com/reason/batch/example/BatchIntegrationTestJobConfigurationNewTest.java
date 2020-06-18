package com.reason.batch.example;

import com.reason.batch.TestBatchConfig;
import com.reason.batch.entity.sales.Sales;
import com.reason.batch.entity.sales.SalesRepository;
import com.reason.batch.entity.sales.SalesSum;
import com.reason.batch.entity.sales.SalesSumRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.List;

import static com.reason.batch.example.BatchJpaTestConfiguration.FORMATTER;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBatchTest
@SpringBootTest(classes={BatchJpaTestConfiguration.class, TestBatchConfig.class})
public class BatchIntegrationTestJobConfigurationNewTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils; // (2)

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private SalesSumRepository salesSumRepository;

    @AfterEach
    public void tearDown() throws Exception {
        salesRepository.deleteAllInBatch();
        salesSumRepository.deleteAllInBatch();
    }

    @Test
    public void 기간내_Sales가_집계되어_SalesSum이된다() throws Exception {
        //given
        LocalDate orderDate = LocalDate.of(2019,10,6);
        int amount1 = 1000;
        int amount2 = 500;
        int amount3 = 100;

        salesRepository.save(new Sales(orderDate, amount1, "1"));
        salesRepository.save(new Sales(orderDate, amount2, "2"));
        salesRepository.save(new Sales(orderDate, amount3, "3"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("orderDate", orderDate.format(FORMATTER))
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters); // (3)

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<SalesSum> salesSumList = salesSumRepository.findAll();
        assertThat(salesSumList.size()).isEqualTo(1);
        assertThat(salesSumList.get(0).getOrderDate()).isEqualTo(orderDate);
        assertThat(salesSumList.get(0).getAmountSum()).isEqualTo(amount1+amount2+amount3);
    }
}
