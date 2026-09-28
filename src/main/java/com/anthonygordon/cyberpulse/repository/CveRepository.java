package com.anthonygordon.cyberpulse.repository;

import com.anthonygordon.cyberpulse.model.Cve;


import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Cve} records.
 */
public interface CveRepository extends JpaRepository<Cve, String> {

}
