package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.*;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;

import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                )).from(member)
                .leftJoin(member.team, team)
                .where(eqUsername(condition.getUsername()),
                        eqTeamName(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())).fetch();
    }

    private BooleanExpression eqUsername(String username) {
        return username != null ? member.username.eq(username) : null;
    }

    private BooleanExpression eqTeamName(String teamName) {
        return teamName != null ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    /**
     * ????????? ?????????, fetchResults() ??????
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        final QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(eqUsername(condition.getUsername()),
                        eqTeamName(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        final List<MemberTeamDto> content = results.getResults();
        final long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * ????????? ?????????
     * ????????? ?????? ?????????, ?????? ????????? ????????? ??????
     */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {

        // ?????? ????????? ??????
        final List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(eqUsername(condition.getUsername()),
                        eqTeamName(condition.getTeamName()),
                        ageLoe(condition.getAgeLoe()),
                        ageGoe(condition.getAgeGoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // ?????? ????????? ??????
        final JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(eqUsername(condition.getUsername()),
                        eqTeamName(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    /**
     * ?????? ?????? ?????? ???????????????
     * -> ????????? ???????????? ??????, Pageable ??? Sort ????????? ???????????? ?????????. ????????? ????????? ???????????? ???????????? sort ??? ???????????? ?????????
     * ??????????????? ????????? ?????? ???????????? ?????? ??????
     */
    @Override
    public List<MemberDto> searchWithOrderSpecifier(MemberSearchCondition condition, Pageable pageable) {
        final JPAQuery<MemberDto> query = queryFactory
                .select(new QMemberDto(
                        member.username,
                        member.age
                )).from(member)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        for (Sort.Order o : pageable.getSort()) {
            final PathBuilder<? extends Member> pathBuilder = new PathBuilder<>(member.getType(), member.getMetadata());
            query.orderBy(new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC, pathBuilder.get(o.getProperty())));
        }

        return query.fetch();
    }
}
