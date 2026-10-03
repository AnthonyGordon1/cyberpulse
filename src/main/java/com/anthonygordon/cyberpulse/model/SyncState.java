package com.anthonygordon.cyberpulse.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sync_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncState {
    
    @Id
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_successful_sync", nullable = false)
    private LocalDateTime lastSuccessfulSync;

}
