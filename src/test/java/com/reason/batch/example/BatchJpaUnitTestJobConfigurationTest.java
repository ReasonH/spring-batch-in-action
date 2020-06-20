package com.reason.batch.example;

import com.reason.batch.TestBatchConfig;
import com.reason.batch.entity.sales.Sales;
import com.reason.batch.entity.sales.SalesRepository;
import com.reason.batch.entity.sales.SalesSum;
import com.reason.batch.entity.sales.SalesSumRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static com.reason.batch.example.BatchJpaTestConfiguration.FORMATTER;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBatchTest
@SpringBootTest(classes={BatchJpaTestConfiguration.class, TestBatchConfig.class})
public class BatchJpaUnitTestJobConfigurationTest {

    @Autowired
    private JpaPagingItemReader<SalesSum> reader;
    @Autowired
    private SalesRepository salesRepository;
    @Autowired
    private SalesSumRepository salesSumRepository;

    private static final LocalDate orderDate = LocalDate.of(2019,10,6);

    @AfterEach
    public void tearDown() throws Exception {
        salesRepository.deleteAllInBatch();
        salesSumRepository.deleteAllInBatch();
    }

    public StepExecution getStepExecution() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("orderDate", orderDate.format(FORMATTER))
                .toJobParameters();

        return MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @Test
    public void 기간내_Sales가_집계되어_SalesSum이된다() throws Exception {
        //given
        int amount1 = 1000;
        int amount2 = 500;
        int amount3 = 100;

        saveSales(amount1, "1");
        saveSales(amount2, "2");
        saveSales(amount3, "3");

        reader.open(new ExecutionContext());

        //when & then
        assertThat(reader.read().getAmountSum()).isEqualTo(amount1+amount2+amount3);
        assertThat(reader.read()).isNull(); // 더이상 읽을게 없어 null
    }

    private Sales saveSales(long amount, String orderNo) {
        return salesRepository.save(new Sales(orderDate, amount, orderNo));
    }
}