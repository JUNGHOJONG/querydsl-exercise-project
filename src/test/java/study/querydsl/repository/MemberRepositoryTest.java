package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import study.querydsl.dto.MemberDto;
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

    @Test
    public void searchPageSimpleTest() {
        final Team teamA = new Team("teamA");
        final Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        em.persist(new Member("member1", 10, teamA));
        em.persist(new Member("member2", 20, teamA));
        em.persist(new Member("member3", 30, teamB));
        em.persist(new Member("member4", 40, teamB));

        final MemberSearchCondition condition = new MemberSearchCondition();

        final PageRequest pageable = PageRequest.of(0, 3);

        final Page<MemberTeamDto> results = memberRepository.searchPageSimple(condition, pageable);

        assertThat(results.getSize()).isEqualTo(3);
        assertThat(results.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

    @Test
    public void searchPageComplexTest() {
        final Team teamA = new Team("teamA");
        final Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        em.persist(new Member("member1", 10, teamA));
        em.persist(new Member("member2", 20, teamA));
        em.persist(new Member("member3", 30, teamB));
        em.persist(new Member("member4", 40, teamB));

        final MemberSearchCondition condition = new MemberSearchCondition();

        final PageRequest pageable = PageRequest.of(0, 3);

        final Page<MemberTeamDto> results = memberRepository.searchPageComplex(condition, pageable);

        assertThat(results.getTotalElements()).isEqualTo(4);
        assertThat(results.getSize()).isEqualTo(3);
        assertThat(results.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

    @Test
    public void sortTestOrderSpecifier() {
        final Team teamA = new Team("teamA");
        final Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        em.persist(new Member("member1", 10, teamA));
        em.persist(new Member("member2", 20, teamA));
        em.persist(new Member("member3", 30, teamB));
        em.persist(new Member("member4", 40, teamB));

        final MemberSearchCondition condition = new MemberSearchCondition();

        final PageRequest pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username")); // sort 후 페이징 처리

        final List<MemberDto> memberDtos = memberRepository.searchWithOrderSpecifier(condition, pageable);

        for (MemberDto dto : memberDtos) {
            System.out.println("username = " + dto.getUsername());
        }

        assertThat(memberDtos.get(0).getUsername()).isEqualTo("member4");
    }
}