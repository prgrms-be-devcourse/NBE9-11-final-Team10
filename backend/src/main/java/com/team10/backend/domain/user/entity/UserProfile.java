package com.team10.backend.domain.user.entity;

import com.team10.backend.domain.user.type.FinancialInterest;
import com.team10.backend.domain.user.type.OccupationStatus;
import com.team10.backend.domain.user.type.Region;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OccupationStatus occupationStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_profile_interests",
            joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interest", length = 30)
    private Set<FinancialInterest> financialInterests = new HashSet<>();

    public static UserProfile create(User user,
                                     Integer birthYear,
                                     Region region,
                                     OccupationStatus occupationStatus,
                                     Set<FinancialInterest> financialInterests) {
        UserProfile profile = new UserProfile();
        profile.user = user;
        profile.birthYear = birthYear;
        profile.region = region;
        profile.occupationStatus = occupationStatus;
        profile.financialInterests = financialInterests != null ? financialInterests : new HashSet<>();
        return profile;
    }

    public void update(Integer birthYear,
                       Region region,
                       OccupationStatus occupationStatus,
                       Set<FinancialInterest> financialInterests) {
        this.birthYear = birthYear;
        this.region = region;
        this.occupationStatus = occupationStatus;
        this.financialInterests.clear();
        if (financialInterests != null) {
            this.financialInterests.addAll(financialInterests);
        }
    }
}
