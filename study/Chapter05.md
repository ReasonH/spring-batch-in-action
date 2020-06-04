## Spring Batch Scope & Job Parameter

`@StepScop` `@JobScope` 및 이 둘과 깊은 관계가 있는 Job Parameter에 대해 공부한다.

### 5-1. JobParameter와 Scope

Spring Batch의 외부 혹은 내부에서 파라미터를 받아 Batch 컴포넌트에서 사용할 수 있게 지원하는 파라미터를 Job Parameter라 한다.

이를 사용하기 위해서는 항상 Spring Batch 전용 Scope을 선언해야 한다. 크게는  `@StepScop` `@JobScope` 2가지가 있다.

다음과 같이 SpEL로 선언해서 사용한다.

```java
@Value("#{jobParameters[파라미터명]})
```

`ScopeJobConfiguration`

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScopeJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job scopeJob(){
        return jobBuilderFactory.get("scopeJob")
                .start(scopeStep1(null))
                .next(scopeStep2())
                .build();
    }

    @Bean
    @JobScope
    public Step scopeStep1(@Value("#{jobParameters[requestDate]}") String requestDate){
        return stepBuilderFactory.get("scopeStep1")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is scopeStep1");
                    log.info(">>>>> requestDate = {}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step scopeStep2() {
        return stepBuilderFactory.get("scopeStep2")
                .tasklet(scopeStep2Tasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Tasklet scopeStep2Tasklet(@Value("#{jobParameters[requestDate]}") String requestDate){
        return (contribution, chunkContext) -> {
            log.info(">>>>>> This is scopeStep2");
            log.info(">>>>>> requestDate = {}", requestDate);
            return RepeatStatus.FINISHED;
        };
    }
}
```

@JobScope는 Step 선언문에서 사용 가능하며, @StepScope는 Tasklet이나 ItemReader, ItemWriter, ItemProcessor에서 사용 가능하다.

현재 Job Parameter의 타입으로 사용할 수 있는 것은 `Double`, `Long`, `Date`, `String`이 있다. `LocalDate` 및 `LocalDateTime`은 `String`으로 받아 타입변환을 해서 사용해야한다.

예제 코드의 호출부에서 `null`을 할당하는 이유는 Job Parameter의 할당이 어플리케이션 실행시에 되지 않기 때문에 가능하다. 이는 다음에 이어서 이야기한다.

### 5-2. @StepScope & @JobScope

Spring Batch는 `@StepScope`와 `@JobScope`라는 특별한 Bean Scope를 지원한다. Spring Bean의 기본 Scope는 singleton 이다. 하지만, Spring Batch 컴포넌트 (ex: Tasklet, ItemReader, ItemWriter, ItemProcessor 등)에 `@StepScope`를 사용하게 되면 

1. Step의 실행시점에 해당 컴포넌트를 Spring Bean으로 생성한다.
2. 마찬가지로 `@JobScope`는 Job 실행시점에 Bean이 생성된다.

즉, Bean 생성 시점을 지정한 Scope가 실행되는 시점으로 지연시킨다.

이렇게 Bean 생성시점을 Step, Job 실행시점으로 지연시켜 얻게되는 장점은 두 가지이다.

#### 1. **JobParameter의 Late Binding**이 가능

Job Parameter를 StepContext 또는 JobExecutionContext 레벨에서 할당시킬 수 있다. 꼭 Application이 실행되는 시점이 아니더라도 Controller나 Service 같은 비지니스 로직 처리 단계에서 Job Parameter를 할당시킬 수 있습니다.

#### 2. 동일한 컴포넌트 병렬 or 동시 사용에 유용

Step 안에 Tasklet이 있고 이 Tasklet은 멤버 변수와 멤버 변수를 변경하는 로직이 있다고 가정한다.

이 경우 `@StepScope` 없이 Step을 병렬로 실행시키게 되면 서로 다른 Step에서 하나의 Tasklet을 두고 마구잡이로 상태를 변경하려고 할 것이다.하지만 `@StepScope` 가 있다면 각각의 Step에서 별도의 Tasklet을 생성하고 관리하기 때문에 서로의 상태를 침범할 일이 없다.

### 5-3. Job Parameter 오해

Job Parameters는 `@Value`를 통해서 가능하다. 이는 Step, Tasklet, Reader 등 Batch 컴포넌트 Bean의 생성 시점에 호출 가능하지만, 정확히는 Scope Bean을 생성할때만 가능하다.

즉, `@StepScope`, `@JobScope` Bean을 생성할때만 Job Parameters가 생성되기 때문에 사용 가능하다.

아래와 같이 메소드를 통해 Bean을 생성하지 않고 클래스에서 직접 Bean 생성을 해본다. Job과 Step의 코드에서 `@Bean`과 **`@Value("#{jobParameters[파라미터명]}")**을 제거하고 `SimpleJobTasklet`을 생성자 DI로 받도록 한다.

`SimpleJobConfiguration` 수정본

```java
@Slf4j // 로깅
@RequiredArgsConstructor // 생성자 DI 용
@Configuration // Spring Batch의 모든 Job은 @config 등록
public class SimpleJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final SimpleJobTasklet tasklet1; // 생성자 주입

    @Bean
    public Job simpleJob(){
        return jobBuilderFactory.get("simpleJob")
                .start(simpleStep1())
                .next(simpleStep2(null))
                .build();
    }

//    @Bean
//    @JobScope
    public Step simpleStep1() {
        log.info(">>>>>>> definition simpleStep1");
        return stepBuilderFactory.get("simpleStep1")
                .tasklet(tasklet1)
                .build();
    }

    @Bean
    @JobScope
    public Step simpleStep2(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("simpleStep2")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is step2");
                    log.info(">>>>> request Date {}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

`SimpleJobTasklet`

```java
@Slf4j
@Component
@StepScope
public class SimpleJobTasklet implements Tasklet {

    @Value("#{jobParameters[requestDate]}")
    private String requestDate;

    public SimpleJobTasklet() {log.info(">>>>>> tasklet 생성");}

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info(">>>>>> This is step1");
        log.info(">>>>>> requestDate = {}", requestDate);
        return null;
    }
}
```

`SimpleJobTasklet`은 `@Component`와 `@StepScope`로 Scope가 Step인 Bean으로 생성한다.

이 상태에서 `@Value("#{jobParameters[파라미터명]})`을 Tasklet의 멤버변수로 할당한다. 이렇게 메소드 파라미터로 JobParameter를 할당받지 않고, 클래스의 멤버 변수로 JobParameter를 할당받도록 하더라도

![](./image/5-1.png)

실행시 JobParameter를 정상적으로 받아 사용할 수 있다.

이는 **SimpleJobTasklet Bean**이 `@StepScope`로 생성되었기 때문이다.

반면, 이 SimpleJobTasklet Bean을 일반 singleton Bean으로 생성할 경우

`Property or field 'jobParameters' cannot be found on object of type`이 발생한다. 즉, Bean을 메소드나 클래스 어느것을 통해서 생성해도 무방하나 Bean의 Scope는 Step이나 Job이어야 한다는 것을 알 수 있다. **JobParameters를 사용하기 위해선 꼭 `@StepScope`, `@JobScope`로 Bean을 생성해야한다.**

### 5-4. JobParameter vs 시스템 변수

의문점

- 왜 꼭 Job Parameter를 써야하지?
- 기존 Spring Boot의 환경변수 or 시스템 변수를 사용하는 것은 안되나?
- CommandLineRunner를 사용한다면 `java jar application.jar -D파라미터`로 시스템 변수 지정하면 안되나?

이에 대해서 왜 Job Parameter를 써야하는지 제시한다.

#### JobParameter

```java
@Bean
@StepScope
public FlatFileItemReader<Partner> reader(
        @Value("#{jobParameters[pathToFile]}") String pathToFile){
    FlatFileItemReader<Partner> itemReader = new FlatFileItemReader<Partner>();
    itemReader.setLineMapper(lineMapper());
    itemReader.setResource(new ClassPathResource(pathToFile));
    return itemReader;
}
```

#### 시스템 변수

```java
@Bean
@ConfigurationProperties(prefix = "my.prefix")
protected class JobProperties {

    String pathToFile;

    ...getters/setters
}

@Autowired
private JobProperties jobProperties;

@Bean
public FlatFileItemReader<Partner> reader() {
    FlatFileItemReader<Partner> itemReader = new FlatFileItemReader<Partner>();
    itemReader.setLineMapper(lineMapper());
    String pathToFile = jobProperties.getPathToFile();
    itemReader.setResource(new ClassPathResource(pathToFile));
    return itemReader;
}
```

위 방식에는 몇 가지 차이점이 있다.

1. 시스템 변수를 사용할 경우 **Spring Batch의 Job Parameter 관련 기능을 사용할 수 없다.** 예를 들어, Spring Batch는 같은 JobParameter로 같은 Job을 두 번 실행하지 않는다. 하지만, 시스템 변수를 사용하게 될 경우 이는 동작하지 않는다. 또한 Spring Batch에서 자동으로 관리해주는 Parameter관련 메타 테이블이 전혀 작동하지 않는다.
2. Command line이 아닌 다른 방법으로 Job을 실행하기 어렵다. 만약 실행해야한다면 **전역 상태를 동적으로 계속해서 변경시킬 수 있도록** Spring Batch를 구성해야한다. 동시에 여러 Job을 실행하는 경우 또는 테스트 코드로 Job을 실행해야 할 때 문제가 발생할 수 있다.

특히, JobParameter를 사용하지 못한다는 것은 **5-2에서 언급된 Late Binding을 못하게 된다**는 의미이다.

웹 서버가 있고, 이 웹서버에서 Batch를 수행한다고 가정할 때, 외부에서 넘겨주는 파라미터에 따라 Batch가 다르게 작동해야한다면, 이를 시스템 변수로 풀어내는 것은 어렵다.

하지만 아래와 같이 JobParameter를 이용한다면 아주 손쉽게 해결할 수 있다.

```java
@Slf4j
@RequiredArgsConstructor
@RestController
public class JobLauncherController {

    private final JobLauncher jobLauncher;
    private final Job job;

    @GetMapping("/launchjob")
    public String handle(@RequestParam("fileName") String fileName) throws Exception {
		
        try {
						// Parameter로 받은 값을 JobParameter로 생성
            JobParameters jobParameters = new JobParametersBuilder()
                                    .addString("input.file.name", fileName)
                                    .addLong("time", System.currentTimeMillis())
                                    .toJobParameters();
						// 생성한 JobParameter로 job 수행
            jobLauncher.run(job, jobParameters);
        } catch (Exception e) {
            log.info(e.getMessage());
        }

        return "Done";
    }
}
```

즉, 개발자가 원하는 어느 타이밍이든 Job Parameter를 생성하고 job을 수행할 수 있음을 알 수 있다. Job Parameter를 각각의 Batch 컴포넌트들이 사용하면 되니 변경이 심한 경우에도 쉽게 대응할 수 있다.

> 실제 웹서버에서 Batch를 관리하는 것은 권장하지 않는다. 위는 예시일 뿐이다.