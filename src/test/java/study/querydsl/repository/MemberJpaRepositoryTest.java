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
public class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() {
        final Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        final Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        final List<Member> members = memberJpaRepository.findAll();
        assertThat(members.size()).isEqualTo(1);
        assertThat(members).containsExactly(member);

        final List<Member> memberByUsername = memberJpaRepository.findByUsername("member1");
        assertThat(memberByUsername).containsExactly(member);
    }

    @Test
    public void basicQuerydslTest() {
        final Member member1 = new Member("member1", 10);
        final Member member2 = new Member("member2", 20);

        memberJpaRepository.save(member1);
        memberJpaRepository.save(member2);

        final List<Member> all_querydsl = memberJpaRepository.findAll_Querydsl();
        assertThat(all_querydsl).extracting("username").containsExactly("member1", "member2");

        final List<Member> username_querydsl = memberJpaRepository.findByUsername_Querydsl("member1");
        assertThat(username_querydsl).extracting("age").containsExactly(10);
    }

    @Test
    public void basicQuerydslJoinTest() {
        // member - 4
        // team - 2
        final Team teamA = new Team("teamA");
        final Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        em.persist(new Member("member1", 10, teamA));
        em.persist(new Member("member2", 20, teamA));
        em.persist(new Member("member3", 30, teamB));
        em.persist(new Member("member4", 40, teamB));

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(28); // age >= 28
        condition.setAgeLoe(40); // age <= 40
        condition.setTeamName("teamB");

        final List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchByBuilder(condition);

        for (MemberTeamDto memberTeamDto : memberTeamDtos) {
            System.out.println("username = " + memberTeamDto.getUsername());
            System.out.println("age = " + memberTeamDto.getAge());
            System.out.println("teamName = " + memberTeamDto.getTeamName());
        }

        assertThat(memberTeamDtos.size()).isEqualTo(2);
    }

    @Test
    public void basicQuerydslJoinTest2() {
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

        final List<MemberTeamDto> results = memberJpaRepository.search(condition);

        assertThat(results).extracting("age").containsExactly(10, 20);
    }
}
