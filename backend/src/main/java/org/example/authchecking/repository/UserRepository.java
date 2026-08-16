package org.example.authchecking.repository;

import org.example.authchecking.entity.AppUser;
import org.example.authchecking.entity.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<AppUser> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
