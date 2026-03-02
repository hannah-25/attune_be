package attune.common.security;

import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final UserType userType;
    private final Collection<? extends GrantedAuthority> authorities;
    private final UserStatus userStatus;


    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(
                user.getId(), user.getEmail(), user.getPassword(), user.getUserType(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name())),
                user.getUserStatus()
        );
    }

    public static CustomUserDetails fromJwt(UUID id, UserType userType, UserStatus userStatus) {
        return new CustomUserDetails(
                id, null, null, userType,
                List.of(new SimpleGrantedAuthority("ROLE_" + userType.name())),
                userStatus
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return userStatus != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isEnabled() {
        return userStatus == UserStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UserType getUserType() {
        return userType;
    }
}
