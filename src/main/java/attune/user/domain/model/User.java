package attune.user.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


import java.util.UUID;

@Entity
public class User {

    @Id
    private UUID id;

    private String email;

    private String password;

    private String nickname;

    private Enum<UserType> userType;
    private Enum<UserStatus> userStatus;

    private String provider;
    private String providerId;

    private String profileImageUrl;

    private Boolean alarmPush;
    private Boolean isOnboarded;



}
