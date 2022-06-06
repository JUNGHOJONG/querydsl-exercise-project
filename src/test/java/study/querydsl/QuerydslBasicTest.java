package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.aspectj.apache.bcel.generic.IINC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDTO;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.*;
import javax.transaction.Transactional;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
public class QuerydslBasicTest {

    @PersistenceUnit
    EntityManagerFactory emf;

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory factory;

    QMember member;
    QTeam team;

    // 만약에 Team 과 Member 를 영속성에 넣는 순서가 바뀌면 어떻게 될까?? ->순서는 상관없음
    @BeforeEach
    public void before() {
        factory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        member = QMember.member;
        team = QTeam.team;
    }

    @DisplayName("jpql 테스트")
    @Test
    public void startJPQL() {
        // member1 을 찾아라
        String jpqlString = "select m from Member m" + " where m.username = :username";

        Member findMember = em.createQuery(jpqlString, Member.class)
                                .setParameter("username", "member1")
                                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("querydsl 테스트 - QMember 기본 인스턴스 사용")
    @Test
    public void startQuerydsl() {
        // member1 을 찾아라
        //QMember member = QMember.member;

        Member findMember = factory.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("querydsl 테스트2 - QMember 별칭 사용")
    @Test
    public void startQuerydsl2() {
        //QMember m = new QMember("m");

        Member findMember = factory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("querydsl 테스트3 - and 조건 사용")
    @Test
    public void testSearchAndParam() {
        List<Member> result = factory.selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetch();
        assertThat(result.size()).isEqualTo(1);
    }

    @DisplayName("querydsl 테스트3 - and 조건 사용")
    @Test
    public void testSearchBetweenParam() {
        List<Member> result = factory.selectFrom(member)
                .where(member.age.between(10, 30))
                .fetch();
        assertThat(result.size()).isEqualTo(3);
    }

    @DisplayName("querydsl 테스트4 - 페이징에서 사용")
    @Test
    public void testPaging() {
        QueryResults<Member> results = factory.selectFrom(member).fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
    }

    @DisplayName("querydsl 테스트5 - count 쿼리 사용")
    @Test
    public void testCount() {
        long count = factory.selectFrom(member).fetchCount();

        assertThat(count).isEqualTo(4);
    }

    /**
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     *  단, 2에서 회원 이름이 null 로 없으면 마지막에 출력(nulls last)
     */
    @DisplayName("회원 정렬 순서")
    @Test
    public void testSort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        em.persist(new Member("member7", 200));
        em.persist(new Member("member8", 300));

        List<Member> results = factory.selectFrom(member)
                .where(member.age.between(100, 300))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(results.get(0).getUsername()).isEqualTo("member8");
        assertThat(results.get(2).getUsername()).isEqualTo("member5");
        assertThat(results.get(4).getUsername()).isNull();
    }

    @DisplayName("페이징 조회건수 제한")
    @Test
    public void testPaging1() {
        List<Member> results = factory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(3)
                .fetch(); // 4 3 2 1
        assertThat(results.size()).isEqualTo(3); //
        assertThat(results.get(0).getUsername()).isEqualTo("member3");
    }

    @DisplayName("페이지 전제 조회")
    @Test
    public void testPaging2() {
        QueryResults<Member> results = factory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(3)
                .fetchResults();
        assertThat(results.getTotal()).isEqualTo(4); // 페이징 단위의 총 개수가 아닌, 전체 총 개수
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getLimit()).isEqualTo(3);
        assertThat(results.getResults().size()).isEqualTo(3);
    }

    /**
     * JPQL
     * select
     *  COUNT(m), // 회원수
     *  SUM(m.age), // 나이 합
     *  AVG(m.age), // 평균 나이
     *  MAX(m.age), // 최대 나이
     *  MIN(m.age), // 최소 나이
     * from Member m
     */
    @DisplayName("집합")
    @Test
    public void testAggregation() {
        List<Tuple> results = factory.select(
                                        member.count(),
                                        member.age.sum(),
                                        member.age.avg(),
                                        member.age.max(),
                                        member.age.min()
                                    ).from(member).fetch();

        Tuple tuple = results.get(0);
        NumberExpression<Long> count = member.count();
        System.out.println(count);

        assertThat(tuple.get(member.count())).isEqualTo(4); // 총 개수
        assertThat(tuple.get(member.age.sum())).isEqualTo(100); // 총 합
        assertThat(tuple.get(member.age.avg())).isEqualTo(25); // 평균
        assertThat(tuple.get(member.age.max())).isEqualTo(40); // 최대값
        assertThat(tuple.get(member.age.min())).isEqualTo(10); // 최소값
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * team: id(1), teamName(해커스)
     * member: id(1), age(10), teamId(1)
     * -> join member.teamId = team.id
     */
    @DisplayName("GroupBy 사용")
    @Test
    public void testGroupBy() {
        List<Tuple> results = factory.select(
                team.name, member.age.avg()
        ).from(member).join(member.team, team)
                .groupBy(team.name)
                .orderBy(member.age.avg().asc())
                .fetch();

        Tuple tuple1 = results.get(0); // 15, 35
        Tuple tuple2 = results.get(1); // 15, 35
        assertThat(tuple1.get(team.name)).isEqualTo("teamA");
        assertThat(tuple1.get(member.age.avg())).isEqualTo(15);
        assertThat(tuple2.get(team.name)).isEqualTo("teamB");
        assertThat(tuple2.get(member.age.avg())).isEqualTo(35);
    }

    @DisplayName("GroupBy + having")
    @Test
    public void testGroupAndHaving() {
        List<Tuple> results = factory.select(
                team.name, member.age.avg()
        ).from(member).join(member.team, team)
                .groupBy(team.name)
                .having(member.age.avg().gt(30))
                .fetch();

        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).get(team.name)).isEqualTo("teamB");
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @DisplayName("기본 조인1")
    @Test
    public void testJoin1() {
        List<String> results = factory.select(
                member.username
        ).from(member).join(member.team, team)
                .where(team.name.eq("teamA"))
                .orderBy(member.username.asc())
                .fetch();

        assertThat(results.get(0)).isEqualTo("member1");
        assertThat(results.get(1)).isEqualTo("member2");
        assertThat(results.size()).isEqualTo(2);
    }

    @DisplayName("기본 조인2")
    @Test
    public void testJoin2() {
        List<Member> results = factory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(results).extracting("username").containsExactly("member1", "member2");
        assertThat(results.size()).isEqualTo(2);
    }

    @DisplayName("세타 조인 - 두 테이블이 연관이 없는 상태로 카티전 프로덕트")
    @Test
    public void testThetaJoin() {
        em.persist(new Member("teamA", 50));
        em.persist(new Member("teamB", 60));

        List<Member> results = factory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(results).extracting("username").containsExactly("teamA", "teamB");
        assertThat(results.size()).isEqualTo(2);
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원 모두 조회
     */
    @DisplayName("조인 - on절 - 조인 대상 필터링")
    @Test
    public void testJoinOnFiltering() {
        List<Tuple> results = factory.select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : results) {
            System.out.println("tuple = " + tuple);
        }

        assertThat(results.size()).isEqualTo(4);
    }

    /**
     * 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     */
    @DisplayName("연관관계 없는 외부 조인")
    @Test
    public void testJoinOnNoRelation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> results = factory.select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : results) {
            System.out.println("tuple = " + tuple);
        }

        assertThat(results.size()).isEqualTo(6);
    }

    @DisplayName("페치 조인 미적용")
    @Test
    public void fetchJoinOnUnApplied() throws Exception {
        em.flush();
        em.clear();

        Member findMember = factory.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @DisplayName("페치 조인 적용")
    @Test
    public void fetchJoinOnApplied() throws Exception {
        em.flush();
        em.clear();

        Member findMember = factory.selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     * select a.username
     *  from member a
     *  where a.age = (select max(b.age) from member b);
     *
     * select max(a.age) from member a;
     */
    @DisplayName("서브 쿼리 eq 사용")
    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> results = factory.selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();

        assertThat(results).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 나이 이상인 회원
     */
    @DisplayName("서브 쿼리 goe 사용")
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> results = factory.selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();

        assertThat(results).extracting("age").containsExactly(30, 40);
        assertThat(results).extracting("username").containsExactly("member3", "member4");
    }

    @DisplayName("서브 쿼리 여러 건 처리 in 사용")
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> results = factory.selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                )).fetch();

        assertThat(results).extracting("age").containsExactly(20, 30, 40);
    }

    @DisplayName("select 절에 subQuery")
    @Test
    public void subQueryInSelect() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> results = factory.select(member.username,
                select(memberSub.age.avg())
                        .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : results) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " + tuple.get(select(memberSub.age.avg())
                    .from(memberSub)));
        }
    }

    @DisplayName("select, 조건절(where), order by 에서 사용 가능")
    @Test
    public void testCaseInSelect() {
        List<String> results = factory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        assertThat(results.get(0)).isEqualTo("열살");
        assertThat(results.get(3)).isEqualTo("기타");
    }

    @DisplayName("case 문 좀더 복잡한 표현을 원할 때")
    @Test
    public void testCaseInSelectMoreComplex() {
        final List<String> results = factory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0-20살")
                        .when(member.age.between(21, 30)).then("21-30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        assertThat(results.get(0)).isEqualTo("0-20살");
        assertThat(results.get(1)).isEqualTo("0-20살");
        assertThat(results.get(2)).isEqualTo("21-30살");
        assertThat(results.get(3)).isEqualTo("기타");
    }

    @DisplayName("orderBy 에서 Case 문과 함께 사용하기")
    @Test
    public void testCaseInSelectWithOrderBy() {
        // 각 구간별 소트를 위한 우선순위를 정한 객체를 만든다
        final NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(3)
                .otherwise(1);

        final List<Tuple> results = factory.select(
                member.age, member.username, rankPath)
                .from(member)
                .orderBy(rankPath.asc())
                .fetch();

        for (Tuple tuple : results) {
            System.out.println("age = " + tuple.get(member.age));
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("rankPath = " + tuple.get(rankPath));
        }
        assertThat(results.get(0).get(member.username)).isEqualTo("member4");
    }

    /**
     * 최적화가 가능하면 SQL에 constant 값을 넘기지 않는다
     */
    @DisplayName("상수 더하기")
    @Test
    public void testConstantPlus() {
        final Tuple tuple = factory
                .select(
                        member.username, Expressions.constant("A"))
                .from(member)
                .fetchFirst();

        System.out.println("tuple = " + tuple);
    }

    /**
     * 상수 더하는 것처럼 최적화가 어려우면 SQL에 constant 값을 넘긴다
     */
    @DisplayName("문자 더하기")
    @Test
    public void testCharacterConcat() {
        final String result = factory
                .select(
                        member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member4"))
                .fetchFirst();

        assertThat(result).isEqualTo("member4_40");
    }

    @DisplayName("순수 JPA 에서 DTO 조회하기")
    @Test
    public void testSearchInJpaWithDTO() {
        List<MemberDto> results = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                "from Member m", MemberDto.class).getResultList();

        for (MemberDto memberDto : results) {
            System.out.println("memberDto.getUsername() = " + memberDto.getUsername());
            System.out.println("memberDto.getAge() = " + memberDto.getAge());
        }

        assertThat(results.size()).isEqualTo(4);
    }

    @DisplayName("Querydsl 빈 생성(1) - Bean population(프로퍼티 접근)")
    @Test
    public void testQuerydslBeanPopulation1() {
        List<MemberDto> results = factory.select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        assertThat(results).extracting("age").containsExactly(10, 20, 30, 40);
    }

    @DisplayName("Querydsl 빈 생성(2) - Bean population(필드 직접 접근)")
    @Test
    public void testQuerydslBeanPopulation2() {
        List<MemberDto> results = factory.select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        assertThat(results).extracting("age").containsExactly(10, 20, 30, 40);
    }

    @DisplayName("Querydsl - 별칭이 다를 때")
    @Test
    public void testQuerydslBeanPopulation3() {
        QMember memberSub = new QMember("memberSub");

        List<UserDTO> results = factory
                .select(Projections.fields(UserDTO.class, member.username.as("name"),
                        ExpressionUtils.as(select(memberSub.age.max()).from(memberSub), "age")
                        )
                ).from(member)
                .fetch();

        assertThat(results).extracting("name").containsExactly("member1", "member2", "member3", "member4");
    }

    @DisplayName("Querydsl - 생성자 사용")
    @Test
    public void testQuerydslBeanPopulation4() {
        List<MemberDto> results = factory.select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        assertThat(results.size()).isEqualTo(4);
    }

    @DisplayName("Querydsl - 생성자 + @QueryProjection")
    @Test
    public void testQuerydslBeanPopulation5() {
        List<MemberDto> results = factory.select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        assertThat(results.size()).isEqualTo(4);
    }

    @DisplayName("Querydsl - distinct")
    @Test
    public void testQuerydslBeanPopulation6() {
        em.persist(new Member("member1", 50));
        em.persist(new Member("member2", 30));

        List<String> results = factory.select(member.username)
                .distinct()
                .from(member)
                .fetch();

        assertThat(results.size()).isEqualTo(4);
    }

    @DisplayName("동적 쿼리 - BooleanBuilder 사용(1)")
    @Test
    public void testQuerydslBeanPopulation7() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> results = searchMember1(usernameParam, ageParam);

        assertThat(results.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (usernameCond != null) {
            booleanBuilder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            booleanBuilder.and(member.age.eq(ageCond));
        }

        return factory
                .selectFrom(member)
                .where(booleanBuilder)
                .fetch();
    }

    @DisplayName("동적 쿼리 - Where  다중 파라미터 사용")
    @Test
    public void 동적쿼리_WhereParam() {
        String usernameParam = null;
        Integer ageParam = 10;

        final List<Member> results = searchMember2(usernameParam, ageParam);

        assertThat(results.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return factory.selectFrom(member)
                        .where(usernameEq(usernameCond), ageEq(ageCond))
                        .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    /**
     * 장점: 여러가지 함수를 Composition 할 수 있다
     * 단점: null 로 반환되는 것 주의해야함(NullPointerException)
     */
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 벌크 연산: 대량 데이터 연산
     */
    @DisplayName("수정, 삭제 벌크 연산(1)")
    @Test
    public void testUpdate() {
        final long count = factory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 콘텍스트 영속성 반영한다.
        em.flush();
        em.clear();

        final List<Member> tempResults = factory.selectFrom(member).fetch();
        for (Member member : tempResults) {
            System.out.println("username = " + member.getUsername());
            System.out.println("age = " + member.getAge());
        }

        final List<Member> results = factory.selectFrom(member)
                .where(member.username.eq("비회원"))
                .fetch();

        assertThat(results.size()).isEqualTo(2);

        assertThat(count).isEqualTo(2);
    }

    @DisplayName("수정, 삭제 벌크 연산(2)")
    @Test
    public void testUpdate2() {
        final long count = factory.update(member)
                .set(member.age, member.age.add(1))
                .execute();

        em.flush(); // -> db 에 반영된다.
        em.clear(); // -> 영속성 컨텍스트가 비어있으면 db 의 데이터를 가져와서 조회한다. 만약에 clear를 하지 않으면 반영 전의 영속성 컨텍스트의 데이터가 조회됨

        final List<Member> results = factory.selectFrom(member)
                .fetch();

        for (Member member : results) {
            System.out.println("username = " + member.getUsername());
            System.out.println("age = " + member.getAge());
        }

        assertThat(count).isEqualTo(4);
    }

    @DisplayName("쿼리 한번으로 대량 데이터 삭제")
    @Test
    public void testDelete() {
        final long count = factory.delete(member)
                .where(member.age.gt(18))
                .execute();

        em.flush();
        em.clear();

        final List<Member> results = factory.selectFrom(member)
                .fetch();

        for (Member member : results) {
            System.out.println("username = " + member.getUsername());
            System.out.println("age = " + member.getAge());
        }

        assertThat(count).isEqualTo(3);
    }

    @DisplayName("sql function(member -> M)")
    @Test
    public void testSqlFunctionWithReplace() {
        final List<String> results = factory.select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : results) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("sql function(소문자로 변경해서 비교)")
    @Test
    public void testSqlFunctionReplaceWithLower() {
        final List<String> results = factory.select(member.username)
                .from(member)
                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .fetch();

        for (String s : results) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("sql function(소문자로 변경해서 비교2)")
    @Test
    public void testSqlFunctionReplaceWithLower2() {
        final List<String> results = factory.select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : results) {
            System.out.println("s = " + s);
        }
    }
}