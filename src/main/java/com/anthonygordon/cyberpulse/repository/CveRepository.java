package com.anthonygordon.cyberpulse.repository;

import com.anthonygordon.cyberpulse.model.Cve;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Cve} records.
 */
@Repository
public interface CveRepository extends JpaRepository<Cve, Long> {

}
