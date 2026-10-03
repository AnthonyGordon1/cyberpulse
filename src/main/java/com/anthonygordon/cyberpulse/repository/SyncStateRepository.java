package com.anthonygordon.cyberpulse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.anthonygordon.cyberpulse.model.SyncState;
 
public interface SyncStateRepository extends JpaRepository<SyncState, String> {

    
}
