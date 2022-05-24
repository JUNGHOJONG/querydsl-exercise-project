package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    // 만약에 Team 과 Member 를 영속성에 넣는 순서가 바뀌면 어떻게 될까?? ->순서는 상관없음
    @BeforeEach
    public void before() {
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

    @DisplayName("querydsl 테스트")
    @Test
    public void startQuerydsl() {
        // member1 을 찾아라
        JPAQueryFactory factory = new JPAQueryFactory(em);

        QMember member = QMember.member;

        Member findMember = factory.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
}
