## Spring-Batch Tutorial

스프링 배치 어플리케이션의 개념에 대해 학습하고 DB 연동 및 간단한 배치 어플리케이션을 경험해봅니다.  
해당 Repository 의 정리 내용은
[기억보단 기록을](https://jojoldu.tistory.com/category/Spring%20Batch)를 읽고 참조하고 있습니다.  

실습은 작성 시점의 구현환경에 맞게 변경했습니다.

- Spring Boot 2.3.0
- Spring Batch
- DB: H2 / MySQL (8+)
- JPA

---
### 목차

Chapter01. [Spring Batch 개요](study/Chapter01.md)

Chapter02. [Batch Job 실행해보기](study/Chapter02.md)

Chapter03. [메타테이블](study/Chapter03.md)

Chapter04. [Job Flow](study/Chapter04.md)

Chapter05. [Scope & Job Parameter](study/Chapter05.md)

Chapter06. [Chunk 지향 처리](study/Chapter06.md)

Chapter07. [ItemReader](study/Chapter07.md)

Chapter08. [ItemWriter](study/Chapter08.md)

Chapter09. [ItemProcessor](study/Chapter09.md)

Chapter10. [테스트](study/Chapter10.md)

### 테이블 세팅 (추가될때마다 업데이트)

~~~sql
// 예제 테이블 생성
create table pay (
  id         bigint not null auto_increment,
  amount     bigint,
  tx_name     varchar(255),
  tx_date_time datetime,
  primary key (id)
) engine = InnoDB;

create table pay2 (
  id         bigint not null auto_increment,
  amount     bigint,
  tx_name     varchar(255),
  tx_date_time datetime,
  primary key (id)
) engine = InnoDB;

create table teacher (
  id        bigint not null auto_increment,
  name      varchar(255),
  subject   varchar(255),
  primary key (id)
) engine = InnoDB;

create table student (
  id        bigint not null auto_increment,
  name      varchar(255),
  teacher_id    bigint,
  foreign key (teacher_id) references teacher(id),
  primary key (id)
) engine = InnoDB;
~~~