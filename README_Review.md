# matching
선생님-학생 매칭 서비스

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다.
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.
- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [예제 ](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#DDD-의-적용)
    - [동기식 호출 과 Fallback 처리](#동기식-호출과-Fallback-처리)
    - [이벤트드리븐 아키텍쳐의 구현](#이벤트드리븐-아키텍쳐의-구현)
    - [Poliglot](#폴리글랏-퍼시스턴스)
    - [Gateway](#Gateway)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-/-서킷-브레이킹-/-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [Persistence Volume](#Persistence-Volume)
    - [Self_healing (liveness probe)](#Self_healing-(liveness-probe))
    - [무정지 재배포](#무정지-재배포)


# 서비스 시나리오

기능적 요구사항 
1. 학생이 금액을 제시하여 매칭 요청을 한다. (선생님을 선택할 수 없다)
1. 학생이 결제한다.
1. 금액이 결제되면 방문요청내역이 목록에 조회된다.
1. 선생님은 방문요청을 조회한 후 선생님 이름과 만남시간을 입력하여 방문을 확정한다.
1. 학생이 리뷰를 작성한다.
1. 작성된 리뷰를 마이페이지에서 확인할 수 있다.

비기능적 요구사항
1. 장애격리
    1. 리뷰관리 기능이 수행되지 않더라도 매칭요청은 365/24 받을 수 있어야 한다. Async(event-driven) Eventual Consistency
1. 성능
    1. 학생이 매칭시스템에서 확인할 수 있는 상태를 마이페이지(프론트엔드)에서 확인할 수 있어야 한다 CQRS
    1. 상태가 바뀔때마다 myPage에서는 변경된 상태를 조회할 수 있어야 한다. Event driven
    1. 리뷰 작성이 완료될 때마다 매칭관리에서 상태가 변경되어야 한다. corelation


# 체크포인트

- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?



# 분석/설계


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  
![스크린샷 2021-01-20 오후 1 33 22](https://user-images.githubusercontent.com/15210906/105127551-7aac6200-5b24-11eb-9387-ce973219c9fb.png)
### 최종 이벤트스토밍 결과

```
- 도메인 서열 분리
    - Core Domain:  match, visit  : 핵심 서비스이며, 연간 Up-time SLA 수준을 99.999% 목표, 배포주기는 match의 경우 1주일 1회 미만, visit의 경우 1개월 1회 미만
    - Supporting Domain:  visitReqLists , myPages : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
    - General Domain:   payment(결제) : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음
```

## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/45473909/105029093-3c1b9680-5a95-11eb-812d-b4b634e5fcec.png)

  - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
  - 호출관계에서 PubSub 과 Req/Res 를 구분함
  - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다.

```
cd review
mvn spring-boot:run  
```


## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 match 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하였고, 모든 구현에 있어서 영문으로 사용하여 별다른  오류없이 구현하였다.

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package matching;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ReviewRepository extends PagingAndSortingRepository<Review, Long>{
}
```

- 적용 후 REST API 의 테스트
```
# match 서비스의 접수처리
http localhost:8088/matches id=5000 price=50000 status=matchRequest
```
![1 match에서명령어날림](https://user-images.githubusercontent.com/45473909/105010823-898d0900-5a7f-11eb-82b3-ab7163311364.PNG)
```
# match 서비스의 접수상태확인
http localhost:8088/matches/5000
```
![2 match테이블에쌓임](https://user-images.githubusercontent.com/45473909/105010828-8b56cc80-5a7f-11eb-8a8e-9a96984d6ab6.PNG)
```
# payment 서비스의 상태확인
http localhost:8088/payments/5000
```
![3 payment에서match에서날린데이터확인](https://user-images.githubusercontent.com/45473909/105011427-48e1bf80-5a80-11eb-9c95-e3d2e760e931.PNG)
```
# match 서비스에 대한 visit 응답
http POST localhost:8088/visits matchId=5000 teacher=TEACHER visitDate=21/01/21
```
![6 visit에서선생님방문계획작성](https://user-images.githubusercontent.com/45473909/105011436-4aab8300-5a80-11eb-8d3e-5fbe98a20668.PNG)

```
# review 작성 완료 요청
http PATCH http://localhost:8085/reviews/5000 review=GOOD status=ReviewCompleted
```
![스크린샷 2021-01-20 오후 1 51 57](https://user-images.githubusercontent.com/15210906/105129070-a1b86300-5b27-11eb-8ae5-4f0743c30be6.png)

```
# myPage 리뷰 확인
http http://localhost:8084/myPages/5000
```
![스크린샷 2021-01-20 오후 1 56 32](https://user-images.githubusercontent.com/15210906/105129189-e643fe80-5b27-11eb-9083-f2933fc60815.png)

```
# match 상태 확인
http http://localhost:8081/matches/5000
```
![스크린샷 2021-01-20 오후 1 52 43](https://user-images.githubusercontent.com/15210906/105129172-daf0d300-5b27-11eb-871a-5115c0059813.png)


## 이벤트드리븐 아키텍쳐의 구현

### 비동기식 호출 

1) 방문배정이 완료 된 후에 방문(visit) 시스템은 리뷰(review) 시스템에 이를 알려주며, 비동기 방식으로 처리하여 매칭요청/결제가 블로킹 되지 않도록 처리한다.

- 이를 위하여 방문배정이력에 기록을 남긴 후에 곧바로 방문배정 완료 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
- 리뷰 서비스에서는 방문배정 이벤트를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
@Service
public class PolicyHandler{
    @Autowired
    ReviewRepository reviewRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverVisitAssigned_ReviewGenerate(@Payload VisitAssigned visitAssigned){
        if(visitAssigned.isMe()){
            System.out.println("##### listener  : " + visitAssigned.toJson());
            Review review = new Review();
            review.setMatchId(visitAssigned.getMatchId());
            review.setStatus("Reviewing");
            reviewRepository.save(review);
        }
    }
}
```

2) 리뷰작성이 완료 된 후에 리뷰(review) 시스템은 매칭, 마이페이지 시스템에 이를 알려주며, 비동기 방식으로 처리하여 매칭요청/결제가 블로킹 되지 않도록 처리한다.

- 이를 위하여 리뷰이력에 기록을 남긴 후에 곧바로 리뷰작성 완료 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
- 매칭, 마이페이지 서비스에서는 리뷰완료 이벤트를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
# 마이페이지 이벤트 핸들러
@StreamListener(KafkaProcessor.INPUT)
public void wheneverReviewCompleted_StatusUpdate(@Payload ReviewCompleted reviewCompleted){

    if(reviewCompleted.isMe()){
        System.out.println("##### listener  : " + reviewCompleted.toJson());

        MyPageRepository.findById(reviewCompleted.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverMatchCanceled_MyPageRepository.findById : exist" );
            MyPage.setReview(reviewCompleted.getReview());
            MyPage.setStatus(reviewCompleted.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPageRepository.save(MyPage);
        });
    }
}
```

```
#  이벤트 핸들러
@StreamListener(KafkaProcessor.INPUT)
public void wheneverReviewCompleted_StatusUpdate(@Payload ReviewCompleted reviewCompleted) {
    if (reviewCompleted.isMe()) {
        System.out.println("##### listener  : " + reviewCompleted.toJson());

        MatchRepository.findById(reviewCompleted.getMatchId()).ifPresent(Match -> {
            System.out.println("##### wheneverVisitCanceled_MatchRepository.findById : exist");
            Match.setStatus(reviewCompleted.getEventType());
            MatchRepository.save(Match);
        });
    }
}
```


### 시간적 디커플링 / 장애격리 

리뷰(review) 시스템은 매칭(match), 방문(visit), 결제(payment) 시스템과 완전히 분리되어있으며 이벤트 수신에 따라 처리되기 때문에, 
리뷰(review) 시스템이 유지보수로 인해 잠시 내려간 상태라도 방문요청(match) 및 결제(payment)하는데에 문제가 없다


- 리뷰 서비스(review)를 잠시 놓은 후 매칭 요청 처리
```
# 매칭요청 처리
http POST http://localhost:8081/matches id=5000 price=50000 status=matchRequest   #Success
```
![1 match에서명령어날림](https://user-images.githubusercontent.com/45473909/105010823-898d0900-5a7f-11eb-82b3-ab7163311364.PNG)

- 결제서비스가 정상적으로 조회되었는지 확인
```
http http://localhost:8083/payments/5000   #Success
```
![3 payment에서match에서날린데이터확인](https://user-images.githubusercontent.com/45473909/105011427-48e1bf80-5a80-11eb-9c95-e3d2e760e931.PNG)

- 방문서비스가 정상적으로 동작하는지 확인
```
http POST http://localhost:8082/visit matchId=5000 teacher=TEACHER visitDate=21/01/21
http http://localhost:8082/visit/5000   #Success
```
![6 visit에서선생님방문계획작성](https://user-images.githubusercontent.com/45473909/105011436-4aab8300-5a80-11eb-8d3e-5fbe98a20668.PNG)

- 리뷰 서비스 다시 가동
```
cd review
mvn spring-boot:run
```

- 가동 전/후의 리뷰상태 확인
```
http PATCH http://localhost:8085/review/5000 review=GOOD status=ReviewCompleted 
http localhost:8085/review
```
![스크린샷 2021-01-20 오후 1 51 57](https://user-images.githubusercontent.com/15210906/105129070-a1b86300-5b27-11eb-8ae5-4f0743c30be6.png)


### SAGA / Corelation

리뷰(review) 시스템에서 상태가 리뷰작성 완료로 변경되면 
1) 마이페이지 시스템의 상태가 변경된다. 
2) 매치(match) 시스템 원천데이터의 상태(status) 정보가 update된다.

```
# 마이페이지 이벤트 핸들러
@StreamListener(KafkaProcessor.INPUT)
public void wheneverReviewCompleted_StatusUpdate(@Payload ReviewCompleted reviewCompleted){

    if(reviewCompleted.isMe()){
        System.out.println("##### listener  : " + reviewCompleted.toJson());

        MyPageRepository.findById(reviewCompleted.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverMatchCanceled_MyPageRepository.findById : exist" );
            MyPage.setReview(reviewCompleted.getReview());
            MyPage.setStatus(reviewCompleted.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPageRepository.save(MyPage);
        });
    }
}
```
![스크린샷 2021-01-20 오후 1 56 32](https://user-images.githubusercontent.com/15210906/105129189-e643fe80-5b27-11eb-9083-f2933fc60815.png)

```
#  이벤트 핸들러
@StreamListener(KafkaProcessor.INPUT)
public void wheneverReviewCompleted_StatusUpdate(@Payload ReviewCompleted reviewCompleted) {
    if (reviewCompleted.isMe()) {
        System.out.println("##### listener  : " + reviewCompleted.toJson());

        MatchRepository.findById(reviewCompleted.getMatchId()).ifPresent(Match -> {
            System.out.println("##### wheneverVisitCanceled_MatchRepository.findById : exist");
            Match.setStatus(reviewCompleted.getEventType());
            MatchRepository.save(Match);
        });
    }
}
```
![스크린샷 2021-01-20 오후 1 52 43](https://user-images.githubusercontent.com/15210906/105129172-daf0d300-5b27-11eb-871a-5115c0059813.png)



### CQRS

매칭 상태가 변경될 때 마다 mypage에서 event를 수신하여 mypage의 매칭상태를 조회하도록 view를 구현하였다.   

```
# mypage > PolicyHandler.java
@StreamListener(KafkaProcessor.INPUT)
public void wheneverVisitCanceled_(@Payload VisitCanceled visitCanceled){

    if(visitCanceled.isMe()){
        System.out.println("##### listener  : " + visitCanceled.toJson());

        MyPageRepository.findById(visitCanceled.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverVisitCanceled_MyPageRepository.findById : exist" );
            MyPage.setStatus(visitCanceled.getEventType());
            MyPageRepository.save(MyPage);
        });
    }
}
@StreamListener(KafkaProcessor.INPUT)
public void wheneverVisitAssigned_(@Payload VisitAssigned visitAssigned){

    if(visitAssigned.isMe()){
        System.out.println("##### listener wheneverVisitAssigned  : " + visitAssigned.toJson());

        MyPageRepository.findById(visitAssigned.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverVisitAssigned_MyPageRepository.findById : exist" );

            MyPage.setStatus(visitAssigned.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPage.setTeacher(visitAssigned.getTeacher());
            MyPage.setVisitDate(visitAssigned.getVisitDate());
            MyPageRepository.save(MyPage);
        });

    }
}
@StreamListener(KafkaProcessor.INPUT)
public void wheneverPaymentApproved_(@Payload PaymentApproved paymentApproved){

    if(paymentApproved.isMe()){
        System.out.println("##### listener  : " + paymentApproved.toJson());

        MyPage mypage = new MyPage();
        mypage.setId(paymentApproved.getMatchId());
        mypage.setPrice(paymentApproved.getPrice());
        mypage.setStatus(paymentApproved.getEventType());
        MyPageRepository.save(mypage);
    }
}
@StreamListener(KafkaProcessor.INPUT)
public void wheneverPaymentCanceled_(@Payload PaymentCanceled paymentCanceled){

    if(paymentCanceled.isMe()){
        System.out.println("##### listener  : " + paymentCanceled.toJson());


        MyPageRepository.findById(paymentCanceled.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverPaymentCanceled_MyPageRepository.findById : exist" );

            MyPage.setStatus(paymentCanceled.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPageRepository.save(MyPage);
        });
    }
}
@StreamListener(KafkaProcessor.INPUT)
public void wheneverMatchCanceled_(@Payload MatchCanceled matchCanceled){

    if(matchCanceled.isMe()){
        System.out.println("##### listener  : " + matchCanceled.toJson());

        MyPageRepository.findById(matchCanceled.getId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverMatchCanceled_MyPageRepository.findById : exist" );

            MyPage.setStatus(matchCanceled.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPageRepository.save(MyPage);
        });

    }
}

@StreamListener(KafkaProcessor.INPUT)
public void wheneverReviewCompleted_StatusUpdate(@Payload ReviewCompleted reviewCompleted){

    if(reviewCompleted.isMe()){
        System.out.println("##### listener  : " + reviewCompleted.toJson());

        MyPageRepository.findById(reviewCompleted.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverMatchCanceled_MyPageRepository.findById : exist" );
            MyPage.setReview(reviewCompleted.getReview());
            MyPage.setStatus(reviewCompleted.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPageRepository.save(MyPage);
        });
    }
}

```

- mypage의 view로 조회

![스크린샷 2021-01-20 오후 1 56 32](https://user-images.githubusercontent.com/15210906/105129189-e643fe80-5b27-11eb-9083-f2933fc60815.png)


## 폴리글랏 퍼시스턴스

review는 다른 서비스와 구별을 위해 별도 hsqldb를 사용 하였다. 이를 위해 review내 pom.xml에 dependency를 h2database에서 hsqldb로 변경 하였다.

```
#review의 pom.xml dependency를 수정하여 DB변경

  <!--
  <dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
  </dependency>
  -->

  <dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
    <version>2.4.0</version>
    <scope>runtime</scope>
  </dependency>

```


## Gateway

```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: match
          uri: http://localhost:8081
          predicates:
            - Path=/matches/** /teacherLists/**
        - id: visit
          uri: http://localhost:8082
          predicates:
            - Path=/visits/** /visitReqLists/**
        - id: payment
          uri: http://localhost:8083
          predicates:
            - Path=/payments/** 
        - id: mypage
          uri: http://localhost:8084
          predicates:
            - Path= /myPages/**
        - id: review
            uri: http://localhost:8085
            predicates:
              - Path= /reviews/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: match
          uri: http://match:8080
          predicates:
            - Path=/matches/** /teacherLists/**
        - id: visit
          uri: http://visit:8080
          predicates:
            - Path=/visits/** /visitReqLists/**
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: mypage
          uri: http://mypage:8080
          predicates:
            - Path= /myPages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

```

- Gateway 서비스 실행 상태에서 8088과 8085로 각각 서비스 실행하였을 때 동일하게 review 서비스 실행되었다.
```
http http://localhost:8088/reviews
```
![스크린샷 2021-01-20 오후 3 18 00](https://user-images.githubusercontent.com/15210906/105135168-eb5a7b00-5b32-11eb-892e-7cd6f481e15f.png)

```
http localhost:8085/reviews
```
![스크린샷 2021-01-20 오후 3 18 16](https://user-images.githubusercontent.com/15210906/105135203-fb725a80-5b32-11eb-8036-592ea7cb4bc3.png)



# 운영

## CI/CD 설정
각 구현체들은 각자의 source repository 에 구성되었으며, 사용한 CI/CD 플랫폼은 Azure를 사용함
pipeline build script 는 각 프로젝트 폴더 이하에 deployment.yml, service.yml 에 포함되었다

![스크린샷 2021-01-20 오후 8 56 01](https://user-images.githubusercontent.com/15210906/105171824-0a243600-5b62-11eb-8a84-ab01452aab8d.png)

![스크린샷 2021-01-20 오후 8 56 14](https://user-images.githubusercontent.com/15210906/105171833-0db7bd00-5b62-11eb-97c1-fa9b8a2b8271.png)


## 오토스케일 아웃

앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다.

visit 구현체에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 10프로를 넘어서면 replica 를 10개까지 늘려준다:

```
# AutoScale Out 설정
kubectl autoscale deploy review --min=1 --max=10 --cpu-percent=10
```
```
# HPA 확인 및 기존 Replicas 수 확인
kubectl get horizontalpodautoscaler.autoscaling/review
```

![스크린샷 2021-01-20 오후 10 34 22](https://user-images.githubusercontent.com/15210906/105182582-8b82c500-5b70-11eb-91cc-5e10a59f3a1a.png)

![스크린샷 2021-01-20 오후 10 18 06](https://user-images.githubusercontent.com/15210906/105182673-a9e8c080-5b70-11eb-9cc8-736ffa73fe4d.png)


```
# 부하 발생
kubectl exec -it pod siege -- /bin/bash
siege -c50 -t60S -v http://review:8080/reviews
```

부하에 따라 visit pod의 cpu 사용률이 증가했고, Pod Replica 수가 증가하는 것을 확인할 수 있었음

![스크린샷 2021-01-20 오후 10 12 50](https://user-images.githubusercontent.com/15210906/105182806-d270ba80-5b70-11eb-8760-d8774228ba33.png)



## Persistence Volume

review 컨테이너를 마이크로서비스로 배포하면서 영속성 있는 저장장치(Persistent Volume)를 적용함


• PVC 설정 확인

Default 스토리지 클래스를 갖는 azure-managed-disk라는 pvc를 생성

kubectl describe pvc

![스크린샷 2021-01-20 오후 11 45 21](https://user-images.githubusercontent.com/15210906/105190729-a86fc600-5b79-11eb-8dc7-7f30d80d707b.png)


• PVC Volume설정 확인

review 구현체에서 해당 pvc를 volumeMount 하여 사용 (kubectl describe pod review-pvc)

![스크린샷 2021-01-20 오후 11 43 20](https://user-images.githubusercontent.com/15210906/105191113-11573e00-5b7a-11eb-8cfa-70ba0b1e4883.png)


• review-pvc pod에 접속하여 mount 용량 확인

![스크린샷 2021-01-20 오후 11 51 45](https://user-images.githubusercontent.com/15210906/105191508-7f036a00-5b7a-11eb-8927-88a8887bf543.png)


## 무정지 재배포

먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함
seige 로 배포작업 직전에 워크로드를 모니터링 함.

```
siege -c10 -t30S -r10 --content-type "application/json" 'http://match:8080/matches POST {"id": "101"}'

```
1. CI/CD를 통해 새로운 배포 시작
1. seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인
![image](https://user-images.githubusercontent.com/75401933/105041017-d7b50300-5aa5-11eb-90dd-5031cd846d81.png)

1. 배포기간중 Availability 가 평소 100%에서 80% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:
1. CI/CD를 통해 새로운 배포 시작
1. 동일한 시나리오로 재배포 한 후 Availability 확인:
![스크린샷 2021-01-21 오전 9 39 57](https://user-images.githubusercontent.com/15210906/105258273-b7cc2f00-5bcc-11eb-902d-602ec0ed155c.png)

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.
