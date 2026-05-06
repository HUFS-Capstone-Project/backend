package com.hufs.capstone.backend.link.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkCandidateRepository extends JpaRepository<LinkCandidate, Long> {

	@EntityGraph(attributePaths = "link")
	List<LinkCandidate> findByLinkIdOrderByCandidateOrderAscIdAsc(Long linkId);

	@EntityGraph(attributePaths = "link")
	@Query("""
			select lc
			from LinkCandidate lc
			where lc.id = :id
			  and lc.link.id = :linkId
			""")
	Optional<LinkCandidate> findByIdAndLinkId(@Param("id") Long id, @Param("linkId") Long linkId);

	long countByLinkId(Long linkId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	long deleteByLinkId(Long linkId);
}
