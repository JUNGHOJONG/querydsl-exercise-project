package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class MemberRepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() {
        final Member member = new Member("member1", 10);
        memberRepository.save(member);

        final Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        final List<Member> results1 = memberRepository.findAll();
        assertThat(results1).containsExactly(member);

        final List<Member> results2 = memberRepository.findByUsername("member1");
        assertThat(results2).containsExactly(member);
    }

    @Test
    public void searchTest() {
        final Team teamA = new Team("teamA");
        final Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        em.persist(new Member("member1", 10, teamA));
        em.persist(new Member("member2", 20, teamA));
        em.persist(new Member("member3", 30, teamB));
        em.persist(new Member("member4", 40, teamB));

        final MemberSearchCondition condition = new MemberSearchCondition();
        condition.setTeamName("teamA");
        condition.setAgeGoe(5);
        condition.setAgeLoe(20);

        final List<MemberTeamDto> results = memberRepository.search(condition);

        assertThat(results).extracting("age").containsExactly(10, 20);
    }
}