package com.mingle.a2srv_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String passwordHash;

    private String name;

    private String surname;

    private Integer age;

    private String gender;

    private String bio;

    private String photoUrl;

    private String city;

    @ElementCollection
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "interest")
    private List<String> interests;

    @Column(name = "date_preference")
    private String datePreference;

    @Column(name = "relationship_goal")
    private String relationshipGoal;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "is_online")
    private Boolean isOnline = false;

    private Boolean verified = false;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "location_lat")
    private Float locationLat;

    @Column(name = "location_lon")
    private Float locationLon;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
