package com.SmartContactManager.Repos;

import com.SmartContactManager.Entites.Contact;
import com.SmartContactManager.Entites.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepo extends JpaRepository<Contact , Long> {
    // ✅ Check duplicate phone for SAME user only
    boolean existsByPhonenoAndUser(String phoneno, User user);

    // ✅ Fetch all contacts belonging to a user
    List<Contact> findByUserId(Long userId);

    // Check duplicate excluding current contact
    boolean existsByPhonenoAndUserAndIdNot(String phoneno, User user, Long id);

    long countByUser(User user);

}
